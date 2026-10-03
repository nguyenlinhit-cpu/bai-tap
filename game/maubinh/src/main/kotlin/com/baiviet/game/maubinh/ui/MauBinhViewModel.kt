package com.baiviet.game.maubinh.ui

import com.baiviet.core.data.SavedGameRepository
import com.baiviet.core.data.TableSession
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.SettlementLine
import kotlinx.coroutines.NonCancellable
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baiviet.core.ai.BotLevel
import com.baiviet.core.cards.Card
import com.baiviet.core.data.CardBackStyle
import com.baiviet.core.data.HouseRulesRepository
import com.baiviet.core.data.SettingsRepository
import com.baiviet.core.data.TableSkin
import com.baiviet.core.data.WalletRepository
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.core.ui.card.CardBackDesign
import com.baiviet.core.ui.card.CardStyle
import com.baiviet.core.ui.effects.BannerData
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.table.SeatInfo
import com.baiviet.game.maubinh.bot.MauBinhBot
import com.baiviet.game.maubinh.engine.MauBinhAction
import com.baiviet.game.maubinh.engine.MauBinhEngine
import com.baiviet.game.maubinh.engine.MauBinhPhase
import com.baiviet.game.maubinh.engine.MauBinhRevealStep
import com.baiviet.game.maubinh.engine.MauBinhState
import com.baiviet.game.maubinh.rules.MauBinhArrangement
import com.baiviet.game.maubinh.rules.MauBinhEvaluator
import com.baiviet.game.maubinh.rules.MauBinhHandOptimizer
import com.baiviet.game.maubinh.rules.MauBinhRules
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
import java.security.SecureRandom
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class MauBinhViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallet: WalletRepository,
    private val settings: SettingsRepository,
    private val houseRules: HouseRulesRepository,
    private val saved: SavedGameRepository,
) : ViewModel() {

    private val engine = MauBinhEngine()
    private val bot = MauBinhBot()
    private val random = Random(SecureRandom().nextLong())

    private val _state = MutableStateFlow(MauBinhUiState())
    val state: StateFlow<MauBinhUiState> = _state.asStateFlow()

    private val effects = Channel<MauBinhEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()

    private var gameJob: Job? = null
    private var timerJob: Job? = null
    private var gameState: MauBinhState? = null
    private var session: TableSession? = null
    private var level = BotLevel.NORMAL
    private var difficulty = 1
    private var started = false
    private var settled = false

    /** Bắt đầu bàn (khôi phục ván dở nếu có). */
    fun start(players: Int, difficulty: Int, bet: Long) {
        if (started) return
        started = true
        viewModelScope.launch {
            val resume = saved.loadTable(MauBinhRules.GAME_ID)
            val p = resume?.players ?: players.coerceIn(2, 4)
            this@MauBinhViewModel.difficulty = resume?.difficulty ?: difficulty
            level = BotLevel.entries.getOrElse(this@MauBinhViewModel.difficulty) { BotLevel.NORMAL }
            val rules = houseRules.current(MauBinhRules.GAME_ID, MauBinhRules.serializer(), MauBinhRules.DEFAULT)
            val s = TableSession(context, wallet, MauBinhRules.GAME_ID, p, resume?.bet ?: bet, rules.minBalanceMultiplier, random)
            s.init(resume)
            session = s
            val appSettings = settings.settings.first()
            val felt = when (appSettings.tableSkin) {
                TableSkin.GREEN -> FeltColor.GREEN
                TableSkin.RED -> FeltColor.RED
                TableSkin.BLUE -> FeltColor.BLUE
            }
            val cardBack = when (appSettings.cardBack) {
                CardBackStyle.CLASSIC_RED -> CardBackDesign.CLASSIC_RED
                CardBackStyle.ROYAL_BLUE -> CardBackDesign.ROYAL_BLUE
                CardBackStyle.JADE -> CardBackDesign.JADE
            }
            val cardStyle = CardStyle(
                fourColor = appSettings.fourColorDeck,
                large = appSettings.largeCards,
                back = cardBack,
            )
            _state.update { it.copy(felt = felt, cardStyle = cardStyle) }
            val restored = saved.decode(MauBinhState.serializer(), resume?.stateJson)
                ?.takeIf { it.players.size == p && it.phase != MauBinhPhase.FINISHED }
            startNewGame(restored)
        }
    }

    private fun persist(st: MauBinhState?) {
        val s = session ?: return
        val json = st?.let { saved.encode(MauBinhState.serializer(), it) }
        viewModelScope.launch(NonCancellable) { saved.saveTable(s.snapshot(difficulty, 0, 0, json)) }
    }

    fun startNewGame(restored: MauBinhState? = null) {
        val session = session ?: return
        gameJob?.cancel()
        timerJob?.cancel()
        settled = false
        gameJob = viewModelScope.launch {
            val rules = houseRules.current(MauBinhRules.GAME_ID, MauBinhRules.serializer(), MauBinhRules.DEFAULT)
            val playerCount = session.playerCount
            val betUnit = session.betUnit
            val table = TableConfig(playerCount = playerCount, rules = restored?.tableConfig?.rules ?: rules, betUnit = betUnit)

            val s = restored ?: engine.start(table, random.nextLong())
            gameState = s
            persist(s)

            val humanCards = s.players[0].initialCards
            val detectedIw = MauBinhEvaluator.detectInstantWin(humanCards)

            // Gợi ý tự động xếp sẵn bàn cho tiện thao tác
            val opt = MauBinhHandOptimizer.findBestArrangement(humanCards, rules)

            _state.update {
                it.copy(
                    initialCards = humanCards,
                    unassignedCards = emptyList(),
                    chi1Cards = opt.arrangement.chi1,
                    chi2Cards = opt.arrangement.chi2,
                    chi3Cards = opt.arrangement.chi3,
                    selectedCard = null,
                    chi1Hand = opt.chi1Hand,
                    chi2Hand = opt.chi2Hand,
                    chi3Hand = opt.chi3Hand,
                    isFull13 = true,
                    isFoul = false,
                    detectedInstantWin = detectedIw,
                    instantWinDialogOpen = detectedIw != null,
                    isInstantWinDeclared = false,
                    phase = MauBinhPhase.ARRANGING,
                    revealStep = MauBinhRevealStep.NOT_STARTED,
                    timeLeftSeconds = rules.turnSeconds,
                    isSubmitted = false,
                    pairwiseComparisons = emptyList(),
                    result = null,
                    banner = null,
                    rules = rules,
                    playerCount = playerCount,
                    betUnit = betUnit,
                    seats = createSeatsUi(s),
                )
            }

            val me = s.players[0]
            if (me.isSubmitted) {
                // Khôi phục: bạn đã nộp bài trước khi app bị tắt
                val arr = me.arrangement
                _state.update {
                    it.copy(
                        isSubmitted = true,
                        instantWinDialogOpen = false,
                        isInstantWinDeclared = me.instantWinDeclared != null,
                        chi1Cards = arr?.chi1 ?: it.chi1Cards,
                        chi2Cards = arr?.chi2 ?: it.chi2Cards,
                        chi3Cards = arr?.chi3 ?: it.chi3Cards,
                    )
                }
                updateUiFromGameState(s)
                if (s.phase == MauBinhPhase.REVEALING) startRevealSequence()
            } else {
                // Bắt đầu đồng hồ đếm ngược 60 giây
                startTimer(table.rules.turnSeconds)
            }

            // Kích hoạt bot xếp bài ngầm
            if (s.phase == MauBinhPhase.ARRANGING) launchBots(s)
        }
    }

    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (t in seconds downTo 0) {
                _state.update { it.copy(timeLeftSeconds = t) }
                if (t == 0) {
                    // Hết giờ -> tự động nộp bài
                    if (!_state.value.isSubmitted) {
                        onIntent(MauBinhIntent.Submit)
                    }
                    break
                }
                delay(1000L)
            }
        }
    }

    private fun launchBots(initialState: MauBinhState) {
        for (seat in 1 until initialState.players.size) {
            if (initialState.players[seat].isSubmitted) continue
            viewModelScope.launch {
                // Giả lập bot suy nghĩ từ 1.5s đến 4s
                delay(random.nextLong(1500L, 4000L))
                val cur = gameState ?: return@launch
                if (cur.phase != MauBinhPhase.ARRANGING) return@launch

                val botPlayerId = PlayerId(seat)
                val legal = engine.legalActions(cur, botPlayerId)
                if (legal.isNotEmpty()) {
                    val view = engine.viewOf(cur, botPlayerId)
                    val action = withContext(Dispatchers.Default) { bot.decide(view, legal, level) }
                    if (gameState?.phase != MauBinhPhase.ARRANGING) return@launch
                    applyEngineAction(botPlayerId, action)
                }
            }
        }
    }

    fun onIntent(intent: MauBinhIntent) {
        when (intent) {
            is MauBinhIntent.SelectCard -> {
                _state.update { current ->
                    val newSelected = if (current.selectedCard == intent.card) null else intent.card
                    current.copy(selectedCard = newSelected)
                }
            }
            is MauBinhIntent.AssignCardToChi -> {
                assignCardToChi(intent.card, intent.chiIndex)
            }
            is MauBinhIntent.SwapCards -> {
                swapTwoCards(intent.cardA, intent.cardB)
            }
            is MauBinhIntent.AutoArrange -> {
                val rules = _state.value.rules
                val cards = _state.value.initialCards
                val opt = MauBinhHandOptimizer.findBestArrangement(cards, rules)
                _state.update {
                    it.copy(
                        unassignedCards = emptyList(),
                        chi1Cards = opt.arrangement.chi1,
                        chi2Cards = opt.arrangement.chi2,
                        chi3Cards = opt.arrangement.chi3,
                        selectedCard = null,
                        chi1Hand = opt.chi1Hand,
                        chi2Hand = opt.chi2Hand,
                        chi3Hand = opt.chi3Hand,
                        isFull13 = true,
                        isFoul = false,
                    )
                }
                viewModelScope.launch { effects.send(MauBinhEffect.Haptic(strong = false)) }
            }
            is MauBinhIntent.ResetArrangement -> {
                _state.update {
                    it.copy(
                        unassignedCards = it.initialCards,
                        chi1Cards = emptyList(),
                        chi2Cards = emptyList(),
                        chi3Cards = emptyList(),
                        selectedCard = null,
                        chi1Hand = null,
                        chi2Hand = null,
                        chi3Hand = null,
                        isFull13 = false,
                        isFoul = false,
                    )
                }
            }
            is MauBinhIntent.Submit -> {
                val curState = _state.value
                if (curState.isSubmitted) return

                val arrangement = if (curState.isFull13 && !curState.isFoul) {
                    MauBinhArrangement(curState.chi1Cards, curState.chi2Cards, curState.chi3Cards)
                } else {
                    // Nếu chưa xếp xong hoặc đang lủng mà bấm nộp/hết giờ -> tự động dùng tối ưu
                    val opt = MauBinhHandOptimizer.findBestArrangement(curState.initialCards, curState.rules)
                    opt.arrangement
                }

                _state.update {
                    it.copy(
                        isSubmitted = true,
                        chi1Cards = arrangement.chi1,
                        chi2Cards = arrangement.chi2,
                        chi3Cards = arrangement.chi3,
                    )
                }
                timerJob?.cancel()

                val action = MauBinhAction.SubmitArrangement(arrangement)
                applyEngineAction(PlayerId(0), action)
            }
            is MauBinhIntent.DeclareInstantWin -> {
                val iw = _state.value.detectedInstantWin ?: return
                _state.update {
                    it.copy(
                        isSubmitted = true,
                        isInstantWinDeclared = true,
                        instantWinDialogOpen = false,
                    )
                }
                timerJob?.cancel()
                applyEngineAction(PlayerId(0), MauBinhAction.DeclareInstantWin(iw))
            }
            is MauBinhIntent.DismissInstantWinDialog -> {
                _state.update { it.copy(instantWinDialogOpen = false) }
            }
            is MauBinhIntent.NextRevealStep -> {
                val cur = gameState ?: return
                if (cur.phase == MauBinhPhase.REVEALING) {
                    applyEngineAction(PlayerId(0), MauBinhAction.NextRevealStep)
                }
            }
            is MauBinhIntent.OpenPairwiseMatrix -> {
                _state.update { it.copy(pairwiseMatrixOpen = true) }
            }
            is MauBinhIntent.DismissPairwiseMatrix -> {
                _state.update { it.copy(pairwiseMatrixOpen = false) }
            }
            is MauBinhIntent.ExitClicked -> {
                val g = gameState
                if (g == null || g.phase == MauBinhPhase.FINISHED) leave(forfeit = false)
                else _state.update { it.copy(exitConfirmOpen = true) }
            }
            is MauBinhIntent.ConfirmExit -> {
                _state.update { it.copy(exitConfirmOpen = false) }
                leave(forfeit = true)
            }
            is MauBinhIntent.DismissExit -> {
                _state.update { it.copy(exitConfirmOpen = false) }
            }
            is MauBinhIntent.OpenQuickGuide -> {
                _state.update { it.copy(quickGuideOpen = true) }
            }
            is MauBinhIntent.DismissQuickGuide -> {
                _state.update { it.copy(quickGuideOpen = false) }
            }
            is MauBinhIntent.PlayAgain -> {
                if (session?.canContinue == false) leave(forfeit = false) else startNewGame()
            }
        }
    }

    private fun leave(forfeit: Boolean) {
        viewModelScope.launch {
            gameJob?.cancel()
            timerJob?.cancel()
            val g = gameState
            val s = session
            if (forfeit && g != null && s != null && g.phase != MauBinhPhase.FINISHED) {
                // Thoát giữa ván = binh lủng: trả mỗi đối thủ số chi phạt lủng
                val perOpponent = g.tableConfig.rules.foulPenaltyChi * s.betUnit
                val lines = (1 until s.playerCount).map {
                    SettlementLine(PlayerId(0), PlayerId(it), perOpponent, "Thoát giữa ván (xử binh lủng)")
                }
                s.forfeit(Settlement.fromLines((0 until s.playerCount).map { PlayerId(it) }, lines))
            }
            saved.clear(MauBinhRules.GAME_ID)
            effects.send(MauBinhEffect.NavigateBack)
        }
    }

    private fun assignCardToChi(card: Card, targetChi: Int) {
        val s = _state.value
        val unassigned = s.unassignedCards.toMutableList()
        val c1 = s.chi1Cards.toMutableList()
        val c2 = s.chi2Cards.toMutableList()
        val c3 = s.chi3Cards.toMutableList()

        // Xóa khỏi vị trí cũ
        unassigned.remove(card)
        c1.remove(card)
        c2.remove(card)
        c3.remove(card)

        // Thêm vào vị trí mới nếu còn chỗ
        when (targetChi) {
            1 -> if (c1.size < 5) c1.add(card) else unassigned.add(card)
            2 -> if (c2.size < 5) c2.add(card) else unassigned.add(card)
            3 -> if (c3.size < 3) c3.add(card) else unassigned.add(card)
            else -> unassigned.add(card)
        }

        updateEvaluations(unassigned, c1, c2, c3)
    }

    private fun swapTwoCards(cardA: Card, cardB: Card) {
        val s = _state.value
        val unassigned = s.unassignedCards.toMutableList()
        val c1 = s.chi1Cards.toMutableList()
        val c2 = s.chi2Cards.toMutableList()
        val c3 = s.chi3Cards.toMutableList()

        fun replaceInList(list: MutableList<Card>, from: Card, to: Card) {
            val idx = list.indexOf(from)
            if (idx != -1) list[idx] = to
        }

        replaceInList(unassigned, cardA, cardB)
        replaceInList(c1, cardA, cardB)
        replaceInList(c2, cardA, cardB)
        replaceInList(c3, cardA, cardB)

        updateEvaluations(unassigned, c1, c2, c3)
    }

    private fun updateEvaluations(
        unassigned: List<Card>,
        c1: List<Card>,
        c2: List<Card>,
        c3: List<Card>,
    ) {
        val rules = _state.value.rules
        val isFull = c1.size == 5 && c2.size == 5 && c3.size == 3
        var h1: com.baiviet.game.maubinh.rules.MauBinhChiHand? = null
        var h2: com.baiviet.game.maubinh.rules.MauBinhChiHand? = null
        var h3: com.baiviet.game.maubinh.rules.MauBinhChiHand? = null
        var isFoul = false

        if (c1.size == 5) h1 = MauBinhEvaluator.evaluateChi1OrChi2(c1, rules)
        if (c2.size == 5) h2 = MauBinhEvaluator.evaluateChi1OrChi2(c2, rules)
        if (c3.size == 3) h3 = MauBinhEvaluator.evaluateChi3(c3)

        if (isFull) {
            val arr = MauBinhArrangement(c1, c2, c3)
            isFoul = arr.isFoul(rules)
        }

        _state.update {
            it.copy(
                unassignedCards = unassigned,
                chi1Cards = c1,
                chi2Cards = c2,
                chi3Cards = c3,
                selectedCard = null,
                chi1Hand = h1,
                chi2Hand = h2,
                chi3Hand = h3,
                isFull13 = isFull,
                isFoul = isFoul,
            )
        }
    }

    private fun applyEngineAction(playerId: PlayerId, action: MauBinhAction) {
        val cur = gameState ?: return
        val transition = engine.apply(cur, playerId, action)
        gameState = transition.state
        if (transition.state.phase != MauBinhPhase.FINISHED) persist(transition.state)
        updateUiFromGameState(transition.state)

        // Nếu chuyển sang REVEALING, kích hoạt chuỗi so chi tự động
        if (transition.state.phase == MauBinhPhase.REVEALING && cur.phase == MauBinhPhase.ARRANGING) {
            startRevealSequence()
        }
    }

    private fun startRevealSequence() {
        viewModelScope.launch {
            // Chi 1 đã được lật -> Chờ 2.5s rồi lật Chi 2
            delay(2500L)
            var cur = gameState ?: return@launch
            if (cur.phase == MauBinhPhase.REVEALING && cur.revealStep == MauBinhRevealStep.CHI_1) {
                applyEngineAction(PlayerId(0), MauBinhAction.NextRevealStep)
            }

            // Chi 2 -> Chờ 2.5s rồi lật Chi 3
            delay(2500L)
            cur = gameState ?: return@launch
            if (cur.phase == MauBinhPhase.REVEALING && cur.revealStep == MauBinhRevealStep.CHI_2) {
                applyEngineAction(PlayerId(0), MauBinhAction.NextRevealStep)
            }

            // Chi 3 -> Chờ 2.5s rồi tổng kết
            delay(2500L)
            cur = gameState ?: return@launch
            if (cur.phase == MauBinhPhase.REVEALING && cur.revealStep == MauBinhRevealStep.CHI_3) {
                applyEngineAction(PlayerId(0), MauBinhAction.NextRevealStep)
            }

            // SUMMARY -> Chờ 2s rồi kết thúc
            delay(2000L)
            cur = gameState ?: return@launch
            if (cur.phase == MauBinhPhase.REVEALING && cur.revealStep == MauBinhRevealStep.SUMMARY) {
                applyEngineAction(PlayerId(0), MauBinhAction.NextRevealStep)
            }
        }
    }

    private fun updateUiFromGameState(state: MauBinhState) {
        val seats = createSeatsUi(state)

        _state.update {
            it.copy(
                phase = state.phase,
                revealStep = state.revealStep,
                seats = seats,
                pairwiseComparisons = state.pairwiseComparisons,
            )
        }

        if (state.phase == MauBinhPhase.FINISHED) {
            handleGameOver(state)
        }
    }

    private fun handleGameOver(state: MauBinhState) {
        if (settled) return
        settled = true
        val s = session ?: return
        viewModelScope.launch(NonCancellable) {
            val (settlement, notes) = s.settle(engine.settle(state))
            if (s.canContinue) persist(null) else saved.clear(MauBinhRules.GAME_ID)
            showResult(state, settlement, notes, s.canContinue)
        }
    }

    private fun showResult(state: MauBinhState, settlement: Settlement, notes: List<String>, canAgain: Boolean) {
        val myDelta = settlement.deltas[PlayerId(0)] ?: 0L

        val myComparisons = state.pairwiseComparisons.filter {
            it.p1 == PlayerId(0) || it.p2 == PlayerId(0)
        }
        val totalChi = myComparisons.sumOf { pair ->
            if (pair.p1 == PlayerId(0)) pair.netChiForP1 else -pair.netChiForP1
        }

        val title = when {
            totalChi > 0 -> "CHIẾN THẮNG!"
            totalChi < 0 -> "THẤT BẠI"
            else -> "HÒA BÀI"
        }

        val resultUi = MauBinhResultUi(
            title = title,
            isWin = totalChi >= 0,
            totalChi = totalChi,
            deltaCoins = myDelta,
            pairwiseComparisons = state.pairwiseComparisons,
            settlement = settlement,
        )

        _state.update {
            it.copy(
                result = resultUi,
                seats = createSeatsUi(state),
                resultNotes = notes + if (!canAgain) listOf("Bạn không đủ xu để chơi tiếp mức cược này.") else emptyList(),
            )
        }
    }

    private fun createSeatsUi(state: MauBinhState): List<MauBinhSeatUi> {
        val rules = state.tableConfig.rules
        return state.players.map { player ->
            val seat = player.playerId.seat
            val isHuman = seat == 0
            val name = session?.name(seat) ?: if (isHuman) "Bạn" else "Máy $seat"
            val info = SeatInfo(
                name = name,
                initial = name.take(1).uppercase(),
                coins = session?.balance(seat) ?: 0L,
                cardCount = player.initialCards.size,
                status = when {
                    player.instantWinDeclared != null -> player.instantWinDeclared.viName
                    player.isSubmitted && state.phase == MauBinhPhase.ARRANGING -> "Đã xếp"
                    else -> null
                },
                isTurn = !player.isSubmitted && state.phase == MauBinhPhase.ARRANGING,
                isHuman = isHuman,
                avatarColor = MAUBINH_SEAT_COLORS[seat % MAUBINH_SEAT_COLORS.size],
            )

            val arr = player.arrangement
            val visible1 = if (state.phase == MauBinhPhase.REVEALING && state.revealStep >= MauBinhRevealStep.CHI_1 || state.phase == MauBinhPhase.FINISHED) arr?.chi1 else null
            val visible2 = if (state.phase == MauBinhPhase.REVEALING && state.revealStep >= MauBinhRevealStep.CHI_2 || state.phase == MauBinhPhase.FINISHED) arr?.chi2 else null
            val visible3 = if (state.phase == MauBinhPhase.REVEALING && state.revealStep >= MauBinhRevealStep.CHI_3 || state.phase == MauBinhPhase.FINISHED) arr?.chi3 else null

            val h1 = if (visible1 != null && arr != null) MauBinhEvaluator.evaluateChi1OrChi2(arr.chi1, rules) else null
            val h2 = if (visible2 != null && arr != null) MauBinhEvaluator.evaluateChi1OrChi2(arr.chi2, rules) else null
            val h3 = if (visible3 != null && arr != null) MauBinhEvaluator.evaluateChi3(arr.chi3) else null

            MauBinhSeatUi(
                seat = seat,
                info = info,
                isSubmitted = player.isSubmitted,
                instantWin = player.instantWinDeclared,
                visibleChi1 = visible1,
                visibleChi2 = visible2,
                visibleChi3 = visible3,
                chi1Hand = h1,
                chi2Hand = h2,
                chi3Hand = h3,
                isFoul = arr?.isFoul(rules) ?: false,
            )
        }
    }
}
