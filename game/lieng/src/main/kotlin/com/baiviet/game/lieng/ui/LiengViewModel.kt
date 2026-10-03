package com.baiviet.game.lieng.ui

import com.baiviet.core.data.SavedGameRepository
import kotlinx.coroutines.NonCancellable
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baiviet.core.ai.BotLevel
import com.baiviet.core.ai.BotSpeed
import com.baiviet.core.ai.ThinkTime
import com.baiviet.core.data.HouseRulesRepository
import com.baiviet.core.data.SettingsRepository
import com.baiviet.core.data.Speed
import com.baiviet.core.data.TableSession
import com.baiviet.core.data.WalletRepository
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.lieng.bot.LiengBot
import com.baiviet.game.lieng.engine.LiengAction
import com.baiviet.game.lieng.engine.LiengEngine
import com.baiviet.game.lieng.engine.LiengState
import com.baiviet.game.lieng.rules.LiengRules
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
 * Game loop Liêng: bạn + 1–3 bot, xu thật trong ví (stack = min(số dư, stackMultiplier × B)),
 * bot nghĩ 0.6–1.8s theo tốc độ trong Cài đặt, hết giờ thì Xem/Theo nếu chưa ai tố, không thì Úp.
 */
@HiltViewModel
class LiengViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallet: WalletRepository,
    private val settings: SettingsRepository,
    private val houseRules: HouseRulesRepository,
    private val saved: SavedGameRepository,
) : ViewModel() {

    private val engine = LiengEngine()
    private val bot = LiengBot()
    private val random = Random(SecureRandom().nextLong())
    private var state: LiengState? = null
    private var session: TableSession? = null
    private var level = BotLevel.NORMAL
    private var speed = BotSpeed.NORMAL
    private var firstSeat = 0
    private var pending: CompletableDeferred<LiengAction>? = null
    private var nextRound = CompletableDeferred<Unit>()
    private var loop: Job? = null

    private val _uiState = MutableStateFlow(LiengUiState(tableConfig = TableConfig(4, LiengRules(), 100L)))
    val uiState: StateFlow<LiengUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LiengEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun start(players: Int, difficulty: Int, bet: Long) {
        if (loop != null) return
        loop = viewModelScope.launch {
            val resume = saved.loadTable(LiengRules.GAME_ID)
            this@LiengViewModel.difficulty = resume?.difficulty ?: difficulty
            level = BotLevel.entries.getOrElse(this@LiengViewModel.difficulty) { BotLevel.NORMAL }
            val rules = houseRules.current(LiengRules.GAME_ID, LiengRules.serializer(), LiengRules.DEFAULT)
            speed = when (settings.settings.first().botSpeed) {
                Speed.SLOW -> BotSpeed.SLOW
                Speed.NORMAL -> BotSpeed.NORMAL
                Speed.FAST -> BotSpeed.FAST
            }
            val config = TableConfig(resume?.players ?: players.coerceIn(2, 4), rules, resume?.bet ?: bet)
            val s = TableSession(context, wallet, LiengRules.GAME_ID, config.playerCount, config.betUnit, rules.minBalanceMultiplier, random)
            s.init(resume)
            session = s
            firstSeat = resume?.round?.coerceIn(0, config.playerCount - 1) ?: 0
            var restored = saved.decode(LiengState.serializer(), resume?.stateJson)
            _uiState.update { it.copy(tableConfig = config, names = s.names(), balances = s.balances(), loading = false) }
            while (true) {
                playRound(config, s, restored)
                restored = null
                firstSeat = (firstSeat + 1) % config.playerCount
                persist(null)
                nextRound = CompletableDeferred()
                nextRound.await()
            }
        }
    }

    private var difficulty = 1

    private suspend fun persist(st: LiengState?) {
        val s = session ?: return
        val json = st?.let { saved.encode(LiengState.serializer(), it) }
        withContext(NonCancellable) { saved.saveTable(s.snapshot(difficulty, 0, firstSeat, json)) }
    }

    private suspend fun playRound(config: TableConfig<LiengRules>, s: TableSession, restored: LiengState?) {
        val stacks = (0 until config.playerCount).map { minOf(s.balance(it), config.betUnit * config.rules.stackMultiplier) }
        var st = restored ?: engine.startHand(config, SecureRandom().nextLong(), stacks, firstSeat)
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

    private suspend fun awaitHuman(st: LiengState): LiengAction {
        val d = CompletableDeferred<LiengAction>()
        pending = d
        _uiState.update { it.copy(isHumanTurn = true, turnStartedAt = System.currentTimeMillis()) }
        val legal = engine.legalActions(st, PlayerId(0))
        val action = withTimeoutOrNull(st.rules.turnSeconds * 1000L) { d.await() }
            ?: if (LiengAction.Check in legal) LiengAction.Check else LiengAction.Fold
        pending = null
        _uiState.update { it.copy(isHumanTurn = false) }
        return action
    }

    private suspend fun botAction(st: LiengState, actor: PlayerId): LiengAction {
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

    private fun sync(st: LiengState) {
        _uiState.update {
            it.copy(
                view = engine.viewOf(st, PlayerId(0)),
                legalActions = engine.legalActions(st, PlayerId(0)),
                isHumanTurn = st.currentActor == PlayerId(0) && pending != null,
            )
        }
    }

    fun onIntent(intent: LiengIntent) {
        when (intent) {
            LiengIntent.Fold -> submit(LiengAction.Fold)
            LiengIntent.Check -> submit(LiengAction.Check)
            LiengIntent.Call -> submit(LiengAction.Call)
            is LiengIntent.Raise -> submit(LiengAction.Raise(intent.amount))
            LiengIntent.AllIn -> submit(LiengAction.AllIn)
            LiengIntent.RequestSqueeze -> {
                val cards = state?.playerStates?.get(PlayerId(0))?.cards.orEmpty()
                if (cards.isNotEmpty()) _uiState.update { it.copy(showSqueezeDialog = true, squeezingCard = cards.last()) }
            }
            LiengIntent.DismissSqueeze -> _uiState.update { it.copy(showSqueezeDialog = false, squeezingCard = null) }
            LiengIntent.NextRound -> {
                if (!_uiState.value.canPlayAgain) {
                    viewModelScope.launch {
                        saved.clear(LiengRules.GAME_ID)
                        _effects.send(LiengEffect.NavigateBack)
                    }
                    return
                }
                _uiState.update { it.copy(showResultDialog = false, settlement = null, notes = emptyList()) }
                nextRound.complete(Unit)
            }
            LiengIntent.RequestExit -> {
                val st = state
                if (st == null || engine.isFinished(st)) {
                    viewModelScope.launch {
                        loop?.cancel()
                        saved.clear(LiengRules.GAME_ID)
                        _effects.send(LiengEffect.NavigateBack)
                    }
                } else {
                    _uiState.update { it.copy(showExitDialog = true) }
                }
            }
            LiengIntent.DismissExit -> _uiState.update { it.copy(showExitDialog = false) }
            LiengIntent.ConfirmExit -> {
                _uiState.update { it.copy(showExitDialog = false) }
                viewModelScope.launch {
                    val st = state
                    val s = session
                    // Thoát giữa ván = úp bài: mất phần đã cược
                    if (st != null && s != null && !engine.isFinished(st)) s.forfeit(engine.forfeit(st, PlayerId(0)))
                    loop?.cancel()
                    saved.clear(LiengRules.GAME_ID)
                    _effects.send(LiengEffect.NavigateBack)
                }
            }
        }
    }

    private fun submit(action: LiengAction) {
        val st = state ?: return
        val d = pending ?: return
        val legal = engine.legalActions(st, PlayerId(0))
        val ok = when (action) {
            is LiengAction.Raise -> legal.any { it is LiengAction.Raise && it.amount == action.amount }
            else -> action in legal
        }
        if (ok) {
            d.complete(action)
        } else {
            viewModelScope.launch { _effects.send(LiengEffect.ShowToast("Hành động không hợp lệ")) }
        }
    }
}
