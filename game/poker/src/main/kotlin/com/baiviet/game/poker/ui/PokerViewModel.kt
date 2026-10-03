package com.baiviet.game.poker.ui

import com.baiviet.core.data.SavedGameRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
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
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.poker.bot.PokerBot
import com.baiviet.game.poker.engine.PokerAction
import com.baiviet.game.poker.engine.PokerEngine
import com.baiviet.game.poker.engine.PokerState
import com.baiviet.game.poker.rules.PokerRules
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
 * Game loop Poker: stack mang qua các ván, nút dealer xoay vòng, buy-in lấy từ ví (mặc định 100 BB).
 * Kết quả mỗi ván ghi thẳng vào ví (stack luôn ≤ số dư) nên rời bàn lúc nào cũng an toàn.
 * Hết giờ: Check nếu được, không thì Úp.
 */
@HiltViewModel
class PokerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallet: WalletRepository,
    private val settings: SettingsRepository,
    private val houseRules: HouseRulesRepository,
    private val saved: SavedGameRepository,
) : ViewModel() {

    private val engine = PokerEngine()
    private val bot = PokerBot()
    private val random = Random(SecureRandom().nextLong())
    private var state: PokerState? = null
    private var session: TableSession? = null
    private var level = BotLevel.NORMAL
    private var speed = BotSpeed.NORMAL
    private var dealerIndex = 0
    private val stacks = mutableListOf<Long>()
    private var pending: CompletableDeferred<PokerAction>? = null
    private var nextHand = CompletableDeferred<Unit>()
    private var loop: Job? = null

    private val _uiState = MutableStateFlow(PokerUiState(tableConfig = TableConfig(4, PokerRules(), 100L)))
    val uiState: StateFlow<PokerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PokerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var difficulty = 1

    private fun buyIn(config: TableConfig<PokerRules>) = config.rules.startingChipsBB * config.betUnit

    /**
     * @param buyInBB số BB mang vào bàn chọn ở màn Chọn bàn (0 = theo Luật nhà)
     */
    fun start(players: Int, difficulty: Int, bigBlind: Long, buyInBB: Int = 0) {
        if (loop != null) return
        loop = viewModelScope.launch {
            val resume = saved.loadTable(PokerRules.GAME_ID)
            this@PokerViewModel.difficulty = resume?.difficulty ?: difficulty
            level = BotLevel.entries.getOrElse(this@PokerViewModel.difficulty) { BotLevel.NORMAL }
            val houseRule = houseRules.current(PokerRules.GAME_ID, PokerRules.serializer(), PokerRules.DEFAULT)
            val bb = resume?.option ?: buyInBB
            val rules = if (bb > 0) houseRule.copy(startingChipsBB = bb) else houseRule
            speed = when (settings.settings.first().botSpeed) {
                Speed.SLOW -> BotSpeed.SLOW
                Speed.NORMAL -> BotSpeed.NORMAL
                Speed.FAST -> BotSpeed.FAST
            }
            val config = TableConfig(resume?.players ?: players.coerceIn(2, 4), rules, resume?.bet ?: bigBlind)
            val s = TableSession(context, wallet, PokerRules.GAME_ID, config.playerCount, config.betUnit, rules.minBalanceMultiplier, random)
            s.init(resume)
            session = s
            val savedStacks = resume?.extra?.let { saved.decode(ListSerializer(Long.serializer()), it) }
            if (savedStacks != null && savedStacks.size == config.playerCount) {
                stacks += savedStacks.mapIndexed { i, v -> v.coerceAtMost(s.balance(i)) }
            } else {
                repeat(config.playerCount) { stacks += minOf(s.balance(it), buyIn(config)) }
            }
            dealerIndex = resume?.round?.coerceIn(0, config.playerCount - 1) ?: random.nextInt(config.playerCount)
            var restored = saved.decode(PokerState.serializer(), resume?.stateJson)
            _uiState.update { it.copy(tableConfig = config, names = s.names(), balances = stackMap(), loading = false) }
            while (true) {
                if (stacks[0] <= 0L) {
                    // Hết stack: nạp lại từ ví hoặc rời bàn
                    _uiState.update { it.copy(showRebuyDialog = true, canPlayAgain = s.myBalance >= bigBlind) }
                    nextHand = CompletableDeferred()
                    nextHand.await()
                    if (stacks[0] <= 0L) continue
                }
                playHand(config, s, restored)
                restored = null
                nextHand = CompletableDeferred()
                nextHand.await()
                dealerIndex = (dealerIndex + 1) % config.playerCount
                persist(null)
            }
        }
    }

    private fun stackMap(): Map<PlayerId, Long> = stacks.indices.associate { PlayerId(it) to stacks[it] }

    private suspend fun persist(st: PokerState?) {
        val s = session ?: return
        val json = st?.let { saved.encode(PokerState.serializer(), it) }
        val extra = saved.encode(ListSerializer(Long.serializer()), stacks.toList())
        val bb = _uiState.value.tableConfig.rules.startingChipsBB
        withContext(NonCancellable) { saved.saveTable(s.snapshot(difficulty, bb, dealerIndex, json, extra)) }
    }

    private suspend fun playHand(config: TableConfig<PokerRules>, s: TableSession, restored: PokerState?) {
        var st = restored ?: engine.startHand(config, SecureRandom().nextLong(), stacks.toList(), dealerIndex)
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
            delay(200)
        }
        val raw = engine.settle(st)
        val (settlement, notes) = s.settle(Settlement.fromLines(raw.deltas.keys, greedyLines(raw)))
        // Stack mới = stack trong engine; bot bị thay (hết xu) thì mua lại stack mới
        for (i in stacks.indices) {
            stacks[i] = st.playerStates.getValue(PlayerId(i)).stack.coerceAtMost(s.balance(i))
            if (i > 0 && stacks[i] < config.betUnit) stacks[i] = minOf(s.balance(i), buyIn(config))
        }
        persist(null)
        _uiState.update {
            it.copy(
                settlement = settlement,
                showResultDialog = true,
                balances = stackMap(),
                names = s.names(),
                notes = notes,
                canPlayAgain = s.myBalance >= config.betUnit,
                isHumanTurn = false,
            )
        }
    }

    /** Dòng thanh toán từ deltas (người âm trả người dương). */
    private fun greedyLines(raw: Settlement) = buildList {
        val winners = ArrayDeque(raw.deltas.filter { it.value > 0 }.map { it.key to it.value })
        for ((loser, d) in raw.deltas.filter { it.value < 0 }) {
            var owe = -d
            while (owe > 0 && winners.isNotEmpty()) {
                val (w, want) = winners.removeFirst()
                val pay = minOf(owe, want)
                add(com.baiviet.core.engine.SettlementLine(loser, w, pay, "Thua pot"))
                owe -= pay
                if (want > pay) winners.addFirst(w to want - pay)
            }
        }
    }

    private suspend fun awaitHuman(st: PokerState): PokerAction {
        val d = CompletableDeferred<PokerAction>()
        pending = d
        val view = engine.viewOf(st, PlayerId(0))
        _uiState.update {
            it.copy(
                isHumanTurn = true,
                turnStartedAt = System.currentTimeMillis(),
                betSliderValue = view.minRaiseTotal.coerceAtMost(view.maxRaiseTotal),
            )
        }
        if (_uiState.value.practiceMode) updateEquity(st)
        val legal = engine.legalActions(st, PlayerId(0))
        val action = withTimeoutOrNull(st.rules.turnSeconds * 1000L) { d.await() }
            ?: if (PokerAction.Check in legal) PokerAction.Check else PokerAction.Fold
        pending = null
        _uiState.update { it.copy(isHumanTurn = false) }
        return action
    }

    private suspend fun botAction(st: PokerState, actor: PlayerId): PokerAction {
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

    private fun sync(st: PokerState) {
        _uiState.update {
            it.copy(
                view = engine.viewOf(st, PlayerId(0)),
                legalActions = engine.legalActions(st, PlayerId(0)),
            )
        }
    }

    private fun updateEquity(st: PokerState) {
        val me = st.playerStates.getValue(PlayerId(0))
        viewModelScope.launch {
            val eq = withContext(Dispatchers.Default) { bot.calculateMonteCarloEquity(me.holeCards, st.communityCards, 1500) }
            _uiState.update { it.copy(practiceEquity = eq) }
        }
    }

    fun onIntent(intent: PokerIntent) {
        when (intent) {
            PokerIntent.Fold -> submit(PokerAction.Fold)
            PokerIntent.Check -> submit(PokerAction.Check)
            PokerIntent.Call -> submit(PokerAction.Call)
            is PokerIntent.Bet -> submitSized(intent.amount)
            is PokerIntent.Raise -> submitSized(intent.totalBet)
            PokerIntent.AllIn -> submit(PokerAction.AllIn)
            is PokerIntent.SetBetSlider -> _uiState.update { it.copy(betSliderValue = intent.value) }
            PokerIntent.TogglePracticeMode -> {
                val next = !_uiState.value.practiceMode
                _uiState.update { it.copy(practiceMode = next) }
                if (next) state?.let { updateEquity(it) }
            }
            PokerIntent.RequestRebuy -> _uiState.update { it.copy(showRebuyDialog = true) }
            PokerIntent.DismissRebuy -> {
                _uiState.update { it.copy(showRebuyDialog = false) }
                if (stacks.firstOrNull() == 0L) {
                    viewModelScope.launch {
                        loop?.cancel()
                        saved.clear(PokerRules.GAME_ID)
                        _effects.send(PokerEffect.NavigateBack)
                    }
                }
            }
            PokerIntent.ConfirmRebuy -> {
                val s = session ?: return
                val config = _uiState.value.tableConfig
                val st = state
                if (st != null && !engine.isFinished(st)) {
                    _uiState.update { it.copy(showRebuyDialog = false) }
                    viewModelScope.launch { _effects.send(PokerEffect.ShowToast("Chỉ nạp được giữa các ván")) }
                    return
                }
                stacks[0] = minOf(s.myBalance, buyIn(config))
                _uiState.update { it.copy(showRebuyDialog = false, balances = stackMap()) }
                if (stacks[0] > 0) nextHand.complete(Unit)
            }
            PokerIntent.NextHand -> {
                if (!_uiState.value.canPlayAgain) {
                    viewModelScope.launch {
                        saved.clear(PokerRules.GAME_ID)
                        _effects.send(PokerEffect.NavigateBack)
                    }
                    return
                }
                _uiState.update { it.copy(showResultDialog = false, settlement = null, notes = emptyList()) }
                nextHand.complete(Unit)
            }
            PokerIntent.RequestExit -> {
                val st = state
                if (st == null || engine.isFinished(st)) {
                    viewModelScope.launch {
                        loop?.cancel()
                        saved.clear(PokerRules.GAME_ID)
                        _effects.send(PokerEffect.NavigateBack)
                    }
                } else {
                    _uiState.update { it.copy(showExitDialog = true) }
                }
            }
            PokerIntent.DismissExit -> _uiState.update { it.copy(showExitDialog = false) }
            PokerIntent.ConfirmExit -> {
                _uiState.update { it.copy(showExitDialog = false) }
                viewModelScope.launch {
                    val st = state
                    val s = session
                    // Rời bàn giữa ván = úp bài: mất phần chip đã bỏ vào pot, stack còn lại vẫn trong ví
                    if (st != null && s != null && !engine.isFinished(st)) {
                        val lost = st.playerStates.getValue(PlayerId(0)).investedThisHand
                        val receiver = st.players.firstOrNull { it.seat != 0 && !st.playerStates.getValue(it).isFolded }
                        if (receiver != null && lost > 0) {
                            s.forfeit(
                                Settlement.fromLines(
                                    st.players,
                                    listOf(com.baiviet.core.engine.SettlementLine(PlayerId(0), receiver, lost, "Rời bàn giữa ván")),
                                ),
                            )
                        }
                    }
                    loop?.cancel()
                    saved.clear(PokerRules.GAME_ID)
                    _effects.send(PokerEffect.NavigateBack)
                }
            }
        }
    }

    private fun submit(action: PokerAction) {
        val st = state ?: return
        val d = pending ?: return
        if (engine.legalActions(st, PlayerId(0)).any { it::class == action::class }) {
            d.complete(action)
        } else {
            viewModelScope.launch { _effects.send(PokerEffect.ShowToast("Hành động không hợp lệ")) }
        }
    }

    /** Cược/Tố theo thanh trượt — tự chọn Bet hay Raise tùy tình huống. */
    private fun submitSized(total: Long) {
        val st = state ?: return
        val d = pending ?: return
        val view = engine.viewOf(st, PlayerId(0))
        val legal = engine.legalActions(st, PlayerId(0))
        val amount = total.coerceIn(view.minRaiseTotal.coerceAtMost(view.maxRaiseTotal), view.maxRaiseTotal)
        val action = when {
            amount >= view.maxRaiseTotal && PokerAction.AllIn in legal -> PokerAction.AllIn
            legal.any { it is PokerAction.Bet } -> PokerAction.Bet(amount)
            legal.any { it is PokerAction.Raise } -> PokerAction.Raise(amount)
            else -> null
        }
        if (action != null) d.complete(action) else viewModelScope.launch { _effects.send(PokerEffect.ShowToast("Không thể cược lúc này")) }
    }
}
