package com.baiviet.game.xidach.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baiviet.core.ai.BotLevel
import com.baiviet.core.ai.BotSpeed
import com.baiviet.core.ai.ThinkTime
import com.baiviet.core.data.HouseRulesRepository
import com.baiviet.core.data.SavedGameRepository
import com.baiviet.core.data.SavedTable
import com.baiviet.core.data.SettingsRepository
import com.baiviet.core.data.Speed
import com.baiviet.core.data.TableSession
import com.baiviet.core.data.WalletRepository
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.SettlementLine
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.xidach.bot.XiDachBot
import com.baiviet.game.xidach.engine.XiDachAction
import com.baiviet.game.xidach.engine.XiDachEngine
import com.baiviet.game.xidach.engine.XiDachPlayerStatus
import com.baiviet.game.xidach.engine.XiDachState
import com.baiviet.game.xidach.rules.XiDachHandStatus
import com.baiviet.game.xidach.rules.XiDachHandType
import com.baiviet.game.xidach.rules.XiDachRules
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.security.SecureRandom
import javax.inject.Inject
import kotlin.random.Random

/**
 * Game loop Xì dách: bạn + 1–3 bot, xu thật trong ví, chọn vai trò cái (Bạn / Máy / Luân phiên),
 * bot nghĩ theo tốc độ trong Cài đặt, hết giờ thì dằn nếu đủ tuổi, không thì rút (cái: xét tất cả).
 * Ván dở được lưu sau mỗi nước để mở lại chơi tiếp.
 */
@HiltViewModel
class XiDachViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallet: WalletRepository,
    private val settings: SettingsRepository,
    private val houseRules: HouseRulesRepository,
    private val saved: SavedGameRepository,
) : ViewModel() {

    private val engine = XiDachEngine()
    private val random = Random(SecureRandom().nextLong())
    private val bot = XiDachBot(random)
    private var state: XiDachState? = null
    private var session: TableSession? = null
    private var level = BotLevel.NORMAL
    private var difficulty = 1
    private var speed = BotSpeed.NORMAL
    private var mode = XiDachDealerMode.BOT_DEALER
    private var dealerSeat = 1
    private var pending: CompletableDeferred<XiDachAction>? = null
    private var nextRound = CompletableDeferred<Unit>()
    private var loop: Job? = null

    private val _uiState = MutableStateFlow(XiDachUiState(tableConfig = TableConfig(4, XiDachRules(), 100L)))
    val uiState: StateFlow<XiDachUiState> = _uiState.asStateFlow()

    private val _effects = Channel<XiDachEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * @param dealerOption vai trò cái: 0 = Bạn, 1 = Máy, 2 = Luân phiên
     */
    fun start(players: Int, difficulty: Int, bet: Long, dealerOption: Int) {
        if (loop != null) return
        loop = viewModelScope.launch {
            val resume = saved.loadTable(XiDachRules.GAME_ID)
            val p = resume?.players ?: players.coerceIn(2, 4)
            this@XiDachViewModel.difficulty = resume?.difficulty ?: difficulty
            level = BotLevel.entries.getOrElse(this@XiDachViewModel.difficulty) { BotLevel.NORMAL }
            mode = XiDachDealerMode.entries.getOrElse(resume?.option ?: dealerOption) { XiDachDealerMode.BOT_DEALER }
            val rules = houseRules.current(XiDachRules.GAME_ID, XiDachRules.serializer(), XiDachRules.DEFAULT)
            speed = settings.settings.first().botSpeed.toBotSpeed()
            val config = TableConfig(p, rules, resume?.bet ?: bet)
            val s = TableSession(context, wallet, XiDachRules.GAME_ID, p, config.betUnit, rules.minBalanceMultiplier, random)
            s.init(resume)
            session = s
            dealerSeat = resume?.round?.coerceIn(0, p - 1) ?: initialDealer(p)
            _uiState.update {
                it.copy(tableConfig = config, dealerMode = mode, names = s.names(), balances = s.balances(), loading = false)
            }
            var restored = saved.decode(XiDachState.serializer(), resume?.stateJson)
            while (true) {
                playRound(config, s, restored)
                restored = null
                nextRound = CompletableDeferred()
                nextRound.await()
            }
        }
    }

    private fun initialDealer(players: Int) = when (mode) {
        XiDachDealerMode.PLAYER_DEALER -> 0
        XiDachDealerMode.BOT_DEALER -> 1
        XiDachDealerMode.ROTATING -> random.nextInt(players)
    }

    private suspend fun playRound(config: TableConfig<XiDachRules>, s: TableSession, restored: XiDachState?) {
        var st = restored ?: engine.startRound(config, SecureRandom().nextLong(), dealerSeat, bets = null)
        state = st
        sync(st)
        persist(st)
        while (!engine.isFinished(st)) {
            val actor = st.currentActor ?: break
            val action = if (actor.seat == 0) awaitHuman(st) else botAction(st, actor)
            st = engine.apply(st, actor, action).state
            state = st
            sync(st)
            persist(st)
            delay(250)
        }
        val (settlement, notes) = s.settle(engine.settle(st))
        dealerSeat = nextDealer(st, config.playerCount)
        persist(null)
        _uiState.update {
            it.copy(
                settlement = settlement,
                showResultDialog = true,
                balances = s.balances(),
                names = s.names(),
                notes = notes,
                canPlayAgain = s.canContinue,
                isHumanTurn = false,
            )
        }
    }

    /** Cái ván sau: cố định theo lựa chọn, hoặc luân phiên (người Xì bàng/Xì dách làm cái nếu bật luật). */
    private fun nextDealer(st: XiDachState, players: Int): Int {
        if (mode != XiDachDealerMode.ROTATING) return dealerSeat
        if (st.rules.winnerBecomesDealer) {
            val winner = st.results.firstOrNull {
                it.outcome > 0 && (it.playerHand.type == XiDachHandType.XI_BANG || it.playerHand.type == XiDachHandType.XI_DACH)
            }
            if (winner != null) return winner.playerId.seat
        }
        return (dealerSeat + 1) % players
    }

    private suspend fun persist(st: XiDachState?) {
        val s = session ?: return
        val json = st?.let { saved.encode(XiDachState.serializer(), it) }
        withContext(NonCancellable) {
            saved.saveTable(s.snapshot(difficulty, mode.ordinal, dealerSeat, json))
        }
    }

    private suspend fun awaitHuman(st: XiDachState): XiDachAction {
        val d = CompletableDeferred<XiDachAction>()
        pending = d
        _uiState.update { it.copy(isHumanTurn = true, turnStartedAt = System.currentTimeMillis()) }
        val legal = engine.legalActions(st, PlayerId(0))
        val action = withTimeoutOrNull(st.rules.turnSeconds * 1000L) { d.await() } ?: timeoutAction(st, legal)
        pending = null
        _uiState.update { it.copy(isHumanTurn = false) }
        return action
    }

    /** Hết giờ: con dằn nếu đủ tuổi, không thì rút; cái xét tất cả nếu được, không thì rút. */
    private fun timeoutAction(st: XiDachState, legal: List<XiDachAction>): XiDachAction {
        val hand = engine.viewOf(st, PlayerId(0)).myHand
        return when {
            XiDachAction.InspectAll in legal -> XiDachAction.InspectAll
            XiDachAction.Stand in legal && hand.status != XiDachHandStatus.NON -> XiDachAction.Stand
            XiDachAction.Hit in legal -> XiDachAction.Hit
            else -> legal.first()
        }
    }

    private suspend fun botAction(st: XiDachState, actor: PlayerId): XiDachAction {
        _uiState.update { it.copy(turnStartedAt = System.currentTimeMillis()) }
        val think = ThinkTime.pick(speed, random)
        val began = System.currentTimeMillis()
        val action = withContext(Dispatchers.Default) {
            bot.decide(engine.viewOf(st, actor), engine.legalActions(st, actor), level)
        }
        val left = think - (System.currentTimeMillis() - began)
        if (left > 0) delay(left)
        return action
    }

    private fun sync(st: XiDachState) {
        _uiState.update {
            it.copy(
                view = engine.viewOf(st, PlayerId(0)),
                legalActions = engine.legalActions(st, PlayerId(0)),
                isHumanTurn = st.currentActor == PlayerId(0) && pending != null,
            )
        }
    }

    fun onIntent(intent: XiDachIntent) {
        when (intent) {
            XiDachIntent.Hit -> submit(XiDachAction.Hit)
            XiDachIntent.Stand -> submit(XiDachAction.Stand)
            is XiDachIntent.Inspect -> submit(XiDachAction.Inspect(intent.target))
            XiDachIntent.InspectAll -> submit(XiDachAction.InspectAll)
            XiDachIntent.RequestSqueeze -> {
                val cards = state?.playerStates?.get(PlayerId(0))?.cards.orEmpty()
                if (cards.isNotEmpty()) _uiState.update { it.copy(showSqueezeDialog = true, squeezingCard = cards.last()) }
            }
            XiDachIntent.DismissSqueeze -> _uiState.update { it.copy(showSqueezeDialog = false, squeezingCard = null) }
            XiDachIntent.NextRound -> {
                if (!_uiState.value.canPlayAgain) {
                    viewModelScope.launch {
                        saved.clear(XiDachRules.GAME_ID)
                        _effects.send(XiDachEffect.NavigateBack)
                    }
                    return
                }
                _uiState.update { it.copy(showResultDialog = false, settlement = null, notes = emptyList()) }
                nextRound.complete(Unit)
            }
            XiDachIntent.RequestExit -> {
                val st = state
                if (st == null || engine.isFinished(st)) {
                    leave(forfeit = false)
                } else {
                    _uiState.update { it.copy(showExitDialog = true) }
                }
            }
            XiDachIntent.DismissExit -> _uiState.update { it.copy(showExitDialog = false) }
            XiDachIntent.ConfirmExit -> {
                _uiState.update { it.copy(showExitDialog = false) }
                leave(forfeit = true)
            }
        }
    }

    private fun leave(forfeit: Boolean) {
        viewModelScope.launch {
            loop?.cancel()
            val st = state
            val s = session
            if (forfeit && st != null && s != null && !engine.isFinished(st)) s.forfeit(forfeitSettlement(st))
            saved.clear(XiDachRules.GAME_ID)
            _effects.send(XiDachEffect.NavigateBack)
        }
    }

    /**
     * Thoát giữa ván = thua ngay: làm con thì mất tiền cược cho cái;
     * làm cái thì trả tiền cược cho mọi nhà con chưa bị xét.
     */
    private fun forfeitSettlement(st: XiDachState): Settlement {
        val me = PlayerId(0)
        val lines = if (st.dealerId == me) {
            st.players.filter { it != me && st.playerStates[it]?.status != XiDachPlayerStatus.RESOLVED }
                .map { SettlementLine(me, it, st.playerStates.getValue(it).betAmount, "Cái thoát giữa ván") }
        } else {
            listOf(SettlementLine(me, st.dealerId, st.playerStates.getValue(me).betAmount, "Thoát giữa ván: mất tiền cược"))
        }
        return Settlement.fromLines(st.players, lines)
    }

    private fun submit(action: XiDachAction) {
        val st = state ?: return
        val d = pending ?: return
        if (action in engine.legalActions(st, PlayerId(0))) {
            d.complete(action)
        } else {
            viewModelScope.launch { _effects.send(XiDachEffect.ShowToast("Hành động không hợp lệ")) }
        }
    }
}

internal fun Speed.toBotSpeed(): BotSpeed = when (this) {
    Speed.SLOW -> BotSpeed.SLOW
    Speed.NORMAL -> BotSpeed.NORMAL
    Speed.FAST -> BotSpeed.FAST
}
