package com.baiviet.game.bacay.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baiviet.core.ai.BotLevel
import com.baiviet.core.ai.BotSpeed
import com.baiviet.core.ai.ThinkTime
import com.baiviet.core.data.HouseRulesRepository
import com.baiviet.core.data.SavedGameRepository
import com.baiviet.core.data.SettingsRepository
import com.baiviet.core.data.Speed
import com.baiviet.core.data.TableSession
import com.baiviet.core.data.WalletRepository
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.SettlementLine
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.bacay.bot.BaCayBot
import com.baiviet.game.bacay.engine.BaCayAction
import com.baiviet.game.bacay.engine.BaCayEngine
import com.baiviet.game.bacay.engine.BaCayState
import com.baiviet.game.bacay.rules.BaCayMode
import com.baiviet.game.bacay.rules.BaCayRules
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
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
 * Game loop Ba cây: bạn + 1–3 bot, xu thật trong ví, chọn vai trò cái (Bạn / Máy / Luân phiên).
 * Bạn nặn hoặc mở bài trước (hết giờ tự mở), sau đó các bot lần lượt lật bài.
 * Ván dở được lưu sau mỗi nước để mở lại chơi tiếp.
 */
@HiltViewModel
class BaCayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallet: WalletRepository,
    private val settings: SettingsRepository,
    private val houseRules: HouseRulesRepository,
    private val saved: SavedGameRepository,
) : ViewModel() {

    private val engine = BaCayEngine()
    private val random = Random(SecureRandom().nextLong())
    private val bot = BaCayBot(random)
    private var state: BaCayState? = null
    private var session: TableSession? = null
    private var level = BotLevel.NORMAL
    private var difficulty = 1
    private var speed = BotSpeed.NORMAL
    private var mode = BaCayDealerMode.BOT_DEALER
    private var dealerSeat = 1
    private var pending: CompletableDeferred<BaCayAction>? = null
    private var nextRound = CompletableDeferred<Unit>()
    private var loop: Job? = null

    private val _uiState = MutableStateFlow(BaCayUiState(tableConfig = TableConfig(4, BaCayRules(), 100L)))
    val uiState: StateFlow<BaCayUiState> = _uiState.asStateFlow()

    private val _effects = Channel<BaCayEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * @param dealerOption vai trò cái: 0 = Bạn, 1 = Máy, 2 = Luân phiên
     */
    fun start(players: Int, difficulty: Int, bet: Long, dealerOption: Int) {
        if (loop != null) return
        loop = viewModelScope.launch {
            val resume = saved.loadTable(BaCayRules.GAME_ID)
            val p = resume?.players ?: players.coerceIn(2, 4)
            this@BaCayViewModel.difficulty = resume?.difficulty ?: difficulty
            level = BotLevel.entries.getOrElse(this@BaCayViewModel.difficulty) { BotLevel.NORMAL }
            mode = BaCayDealerMode.entries.getOrElse(resume?.option ?: dealerOption) { BaCayDealerMode.BOT_DEALER }
            val rules = houseRules.current(BaCayRules.GAME_ID, BaCayRules.serializer(), BaCayRules.DEFAULT)
            speed = when (settings.settings.first().botSpeed) {
                Speed.SLOW -> BotSpeed.SLOW
                Speed.NORMAL -> BotSpeed.NORMAL
                Speed.FAST -> BotSpeed.FAST
            }
            val config = TableConfig(p, rules, resume?.bet ?: bet)
            val s = TableSession(context, wallet, BaCayRules.GAME_ID, p, config.betUnit, rules.minBalanceMultiplier, random)
            s.init(resume)
            session = s
            dealerSeat = resume?.round?.coerceIn(0, p - 1) ?: when (mode) {
                BaCayDealerMode.PLAYER_DEALER -> 0
                BaCayDealerMode.BOT_DEALER -> 1
                BaCayDealerMode.ROTATING -> random.nextInt(p)
            }
            _uiState.update {
                it.copy(tableConfig = config, dealerMode = mode, names = s.names(), balances = s.balances(), loading = false)
            }
            var restored = saved.decode(BaCayState.serializer(), resume?.stateJson)
            while (true) {
                playRound(config, s, restored)
                restored = null
                nextRound = CompletableDeferred()
                nextRound.await()
            }
        }
    }

    private suspend fun playRound(config: TableConfig<BaCayRules>, s: TableSession, restored: BaCayState?) {
        var st = restored ?: engine.startRound(config, SecureRandom().nextLong(), dealerSeat, bets = null)
        state = st
        sync(st)
        persist(st)
        // Bạn mở bài trước (nặn hoặc mở ngay), hết giờ tự mở
        if (engine.legalActions(st, PlayerId(0)).isNotEmpty()) {
            st = engine.apply(st, PlayerId(0), awaitHuman(st)).state
            state = st
            sync(st)
            persist(st)
        }
        // Các bot lần lượt lật bài
        for (pid in st.players.drop(1)) {
            if (engine.isFinished(st)) break
            val legal = engine.legalActions(st, pid)
            if (legal.isEmpty()) continue
            _uiState.update { it.copy(turnStartedAt = System.currentTimeMillis()) }
            delay(ThinkTime.pick(speed, random) / 2)
            val action = bot.decide(engine.viewOf(st, pid), legal.filter { it == BaCayAction.Reveal }, level)
            st = engine.apply(st, pid, action).state
            state = st
            sync(st)
            persist(st)
        }
        val (settlement, notes) = s.settle(engine.settle(st))
        if (mode == BaCayDealerMode.ROTATING) dealerSeat = (dealerSeat + 1) % config.playerCount
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

    private suspend fun persist(st: BaCayState?) {
        val s = session ?: return
        val json = st?.let { saved.encode(BaCayState.serializer(), it) }
        withContext(NonCancellable) {
            saved.saveTable(s.snapshot(difficulty, mode.ordinal, dealerSeat, json))
        }
    }

    private suspend fun awaitHuman(st: BaCayState): BaCayAction {
        val d = CompletableDeferred<BaCayAction>()
        pending = d
        _uiState.update { it.copy(isHumanTurn = true, turnStartedAt = System.currentTimeMillis()) }
        val action = withTimeoutOrNull(st.rules.turnSeconds * 1000L) { d.await() } ?: BaCayAction.Reveal
        pending = null
        _uiState.update { it.copy(isHumanTurn = false, showSqueezeDialog = false, squeezingCard = null) }
        return action
    }

    private fun sync(st: BaCayState) {
        _uiState.update {
            it.copy(
                view = engine.viewOf(st, PlayerId(0)),
                legalActions = engine.legalActions(st, PlayerId(0)),
                isHumanTurn = pending != null,
            )
        }
    }

    fun onIntent(intent: BaCayIntent) {
        when (intent) {
            BaCayIntent.Reveal -> pending?.complete(BaCayAction.Reveal)
            BaCayIntent.RequestSqueeze -> {
                val cards = state?.playerStates?.get(PlayerId(0))?.cards.orEmpty()
                if (cards.isNotEmpty() && pending != null) {
                    // Nặn lá thứ 3 — lá quyết định số nút
                    _uiState.update { it.copy(showSqueezeDialog = true, squeezingCard = cards.last()) }
                }
            }
            BaCayIntent.DismissSqueeze -> {
                _uiState.update { it.copy(showSqueezeDialog = false, squeezingCard = null) }
                pending?.complete(BaCayAction.Reveal)
            }
            BaCayIntent.NextRound -> {
                if (!_uiState.value.canPlayAgain) {
                    viewModelScope.launch {
                        saved.clear(BaCayRules.GAME_ID)
                        _effects.send(BaCayEffect.NavigateBack)
                    }
                    return
                }
                _uiState.update { it.copy(showResultDialog = false, settlement = null, notes = emptyList()) }
                nextRound.complete(Unit)
            }
            BaCayIntent.RequestExit -> {
                val st = state
                if (st == null || engine.isFinished(st)) leave(forfeit = false) else _uiState.update { it.copy(showExitDialog = true) }
            }
            BaCayIntent.DismissExit -> _uiState.update { it.copy(showExitDialog = false) }
            BaCayIntent.ConfirmExit -> {
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
            saved.clear(BaCayRules.GAME_ID)
            _effects.send(BaCayEffect.NavigateBack)
        }
    }

    /**
     * Thoát giữa ván = thua tiền cược: có cái thì con trả cái (cái thoát thì trả mọi con);
     * Ăn tất thì trả cho người có bài cao nhất trong số còn lại.
     */
    private fun forfeitSettlement(st: BaCayState): Settlement {
        val me = PlayerId(0)
        val bet = st.playerStates.getValue(me).betAmount
        val lines = when {
            st.rules.gameMode == BaCayMode.WINNER_TAKES_ALL -> {
                // Bạn có thể đã lật bài (RevealAll khi đó không hợp lệ) → tính trên bản sao coi như chưa lật
                val probe = st.copy(playerStates = st.playerStates + (me to st.playerStates.getValue(me).copy(isRevealed = false)))
                val best = engine.apply(probe, me, BaCayAction.RevealAll).state.results
                    .filter { it.playerId != me }
                    .maxByOrNull { it.delta }?.playerId ?: st.players.first { it != me }
                listOf(SettlementLine(me, best, bet, "Thoát giữa ván: mất tiền cược"))
            }
            st.dealerId == me -> st.players.filter { it != me }
                .map { SettlementLine(me, it, st.playerStates.getValue(it).betAmount, "Cái thoát giữa ván") }
            else -> listOf(SettlementLine(me, st.dealerId, bet, "Thoát giữa ván: mất tiền cược"))
        }
        return Settlement.fromLines(st.players, lines)
    }
}
