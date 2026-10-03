package com.baiviet.game.samloc.ui

import com.baiviet.core.data.SavedGameRepository
import com.baiviet.core.data.SavedTable
import kotlinx.coroutines.NonCancellable
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.SettlementLine
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baiviet.core.ai.BotLevel
import com.baiviet.core.ai.BotSpeed
import com.baiviet.core.ai.ThinkTime
import com.baiviet.core.cards.Card
import com.baiviet.core.data.AppSettings
import com.baiviet.core.data.CardBackStyle
import com.baiviet.core.data.HouseRulesRepository
import com.baiviet.core.data.SettingsRepository
import com.baiviet.core.data.Speed
import com.baiviet.core.data.TableSkin
import com.baiviet.core.data.WalletRepository
import com.baiviet.core.engine.GameEvent
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.core.ui.card.CardBackDesign
import com.baiviet.core.ui.card.CardStyle
import com.baiviet.core.ui.effects.BannerData
import com.baiviet.core.ui.table.CenterPileState
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.table.PlayedSet
import com.baiviet.core.ui.table.SeatInfo
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoins
import com.baiviet.game.samloc.R
import com.baiviet.game.samloc.bot.SamLocBot
import com.baiviet.game.samloc.engine.SamLocEngine
import com.baiviet.game.samloc.engine.SamLocState
import com.baiviet.game.samloc.engine.SlAction
import com.baiviet.game.samloc.engine.SlPhase
import com.baiviet.game.samloc.rules.SamLocCombo
import com.baiviet.game.samloc.rules.SamLocComboType
import com.baiviet.game.samloc.rules.SamLocRules
import com.baiviet.game.samloc.rules.slRank
import com.baiviet.game.samloc.rules.sortedSl
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

@HiltViewModel
class SamLocViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val wallet: WalletRepository,
        private val settingsRepo: SettingsRepository,
        private val houseRules: HouseRulesRepository,
        private val saved: SavedGameRepository,
    ) : ViewModel() {
        private val engine = SamLocEngine()
        private val bot = SamLocBot()
        private val random = Random(SecureRandom().nextLong())

        private val _state = MutableStateFlow(SlUiState())
        val state: StateFlow<SlUiState> = _state.asStateFlow()

        private val effects = Channel<SlEffect>(Channel.BUFFERED)
        val effectFlow = effects.receiveAsFlow()

        private var started = false
        private var loopJob: Job? = null
        private lateinit var settings: AppSettings
        private lateinit var rules: SamLocRules
        private var playerCount = 4
        private var level = BotLevel.NORMAL
        private var betUnit = 100L
        private var difficulty = 1

        private var game: SamLocState? = null
        private var pendingHuman: CompletableDeferred<SlAction>? = null
        private var newGameSignal = CompletableDeferred<Unit>()
        private var previousWinner: PlayerId? = null

        private var myBalance = 0L
        private val botNames = mutableListOf<String>()
        private val botBalances = mutableListOf<Long>()
        private val log = mutableListOf<String>()
        private var nextId = 1

        private fun str(
            id: Int,
            vararg args: Any,
        ): String = context.getString(id, *args)

        fun start(
            players: Int,
            difficulty: Int,
            bet: Long,
        ) {
            if (started) return
            started = true
            playerCount = players.coerceIn(2, 4)
            this.difficulty = difficulty
            level = BotLevel.entries.getOrElse(difficulty) { BotLevel.NORMAL }
            betUnit = bet
            loopJob =
                viewModelScope.launch {
                    settings = settingsRepo.settings.first()
                    rules = houseRules.current(SamLocRules.GAME_ID, SamLocRules.serializer(), SamLocRules.DEFAULT)
                    wallet.ensureProfile()
                    myBalance = wallet.current()

                    // Ván dở lần trước (app bị tắt giữa chừng) → khôi phục bàn
                    val resume = saved.loadTable(SamLocRules.GAME_ID)?.takeIf { it.botNames.size == it.players - 1 }
                    if (resume != null) {
                        playerCount = resume.players.coerceIn(2, 4)
                        this@SamLocViewModel.difficulty = resume.difficulty
                        level = BotLevel.entries.getOrElse(resume.difficulty) { BotLevel.NORMAL }
                        betUnit = resume.bet
                        botNames += resume.botNames
                        botBalances += resume.botBalances
                        previousWinner = resume.round.takeIf { it >= 0 }?.let { PlayerId(it) }
                    } else {
                        val names = BOT_NAMES.shuffled(random)
                        repeat(playerCount - 1) {
                            botNames += names[it]
                            botBalances += freshBotBalance()
                        }
                    }
                    restored = saved.decode(SamLocState.serializer(), resume?.stateJson)
                        ?.takeIf { it.hands.size == playerCount && !it.finished }

                    _state.update {
                        it.copy(
                            loading = false,
                            playerCount = playerCount,
                            betUnit = betUnit,
                            rules = rules,
                            cardStyle = settings.toCardStyle(),
                            felt = settings.tableSkin.toFelt(),
                            showGuide = SamLocRules.GAME_ID !in settings.seenGuides,
                            turnMillis = rules.turnSeconds * 1000L,
                            soundOn = settings.sfx,
                        )
                    }

                    if (SamLocRules.GAME_ID !in settings.seenGuides) {
                        while (_state.value.showGuide) delay(100)
                    }

                    while (true) {
                        playOneGame(restored)
                        restored = null
                        newGameSignal.await()
                    }
                }
        }

        private fun freshBotBalance(): Long = betUnit * rules.minBalanceMultiplier * random.nextLong(3, 12)

        fun onIntent(intent: SlIntent) {
            when (intent) {
                is SlIntent.Toggle -> {
                    updateSelection { if (intent.card in it) it - intent.card else it + intent.card }
                }

                is SlIntent.Sweep -> {
                    updateSelection { sel ->
                        val all = intent.cards.toSet()
                        if (all.all { it in sel }) sel - all else sel + all
                    }
                }

                SlIntent.Play -> {
                    submitPlay()
                }

                SlIntent.Pass -> {
                    submitPass()
                }

                SlIntent.CallSam -> {
                    submitBaoSam(true)
                }

                SlIntent.SkipSam -> {
                    submitBaoSam(false)
                }

                SlIntent.ConfirmBao1Play -> {
                    confirmBao1Play()
                }

                SlIntent.CancelBao1Play -> {
                    _state.update { it.copy(showBao1Warning = false, pendingBao1Cards = null) }
                }

                SlIntent.Hint -> {
                    showHint()
                }

                SlIntent.Sort -> {
                    val mode = if (_state.value.sortMode == SlSortMode.BY_RANK) SlSortMode.BY_GROUP else SlSortMode.BY_RANK
                    _state.update { it.copy(sortMode = mode, hand = sortHand(it.hand, mode)) }
                }

                SlIntent.NewGame -> {
                    if (_state.value.result?.canPlayAgain == true) {
                        _state.update { it.copy(result = null) }
                        newGameSignal.complete(Unit)
                    }
                }

                SlIntent.RequestExit -> {
                    val g = game
                    if (g == null || g.finished || _state.value.result != null) {
                        viewModelScope.launch {
                            loopJob?.cancel()
                            saved.clear(SamLocRules.GAME_ID)
                            effects.send(SlEffect.Exit)
                        }
                    } else {
                        val penalty = exitPenalty().coerceAtMost(myBalance)
                        _state.update { it.copy(showExitConfirm = true, exitPenalty = penalty) }
                    }
                }

                SlIntent.CancelExit -> {
                    _state.update { it.copy(showExitConfirm = false) }
                }

                SlIntent.ConfirmExit -> {
                    confirmExit()
                }

                SlIntent.DismissGuide -> {
                    _state.update { it.copy(showGuide = false) }
                    viewModelScope.launch { settingsRepo.markGuideSeen(SamLocRules.GAME_ID) }
                }

                SlIntent.ToggleSound -> {
                    val on = !_state.value.soundOn
                    _state.update { it.copy(soundOn = on) }
                    viewModelScope.launch { settingsRepo.update { it.copy(sfx = on, music = on) } }
                }
            }
        }

        private fun updateSelection(transform: (Set<Card>) -> Set<Card>) {
            _state.update { s ->
                val sel = transform(s.selected).intersect(s.hand.toSet())
                s.copy(selected = sel, hintCards = emptySet(), message = null, canPlay = canPlay(sel))
            }
        }

        private fun canPlay(selected: Set<Card>): Boolean {
            val g = game ?: return false
            if (pendingHuman == null || g.turn != 0 || selected.isEmpty()) return false
            val combo = SamLocCombo.classify(selected) ?: return false
            val top = g.top ?: return true
            return com.baiviet.game.samloc.rules.SamLocBeatRules
                .canBeat(combo, top, g.rules)
        }

        private fun submitPlay() {
            val g = game ?: return
            val pending = pendingHuman ?: return
            val sel = _state.value.selected
            if (sel.isEmpty()) {
                _state.update { it.copy(message = str(R.string.sl_select_cards)) }
                return
            }
            val combo = SamLocCombo.classify(sel)
            if (combo == null) {
                _state.update { it.copy(message = str(R.string.sl_invalid_combo)) }
                return
            }
            val top = g.top
            if (top != null &&
                !com.baiviet.game.samloc.rules.SamLocBeatRules
                    .canBeat(combo, top, g.rules)
            ) {
                _state.update { it.copy(message = str(R.string.sl_cannot_beat)) }
                return
            }

            // Kiểm tra cảnh báo Báo 1
            val nextSeat = (0 + 1) % g.playerCount
            if (g.samCaller == null && nextSeat in g.baoMot && combo.type == SamLocComboType.SINGLE) {
                val maxSingleRank = g.hands[0].maxOfOrNull { it.slRank } ?: -1
                if (combo.top.slRank < maxSingleRank) {
                    // Hiển thị dialog cảnh báo
                    _state.update {
                        it.copy(
                            showBao1Warning = true,
                            pendingBao1Cards = combo.cards,
                        )
                    }
                    return
                }
            }

            pending.complete(SlAction.Play(combo.cards))
        }

        private fun confirmBao1Play() {
            val cards = _state.value.pendingBao1Cards ?: return
            _state.update { it.copy(showBao1Warning = false, pendingBao1Cards = null) }
            pendingHuman?.complete(SlAction.Play(cards))
        }

        private fun submitPass() {
            val g = game ?: return
            if (g.top == null && g.samCaller == null) return
            pendingHuman?.complete(SlAction.Pass)
        }

        private fun submitBaoSam(call: Boolean) {
            val pending = pendingHuman ?: return
            pending.complete(if (call) SlAction.CallSam else SlAction.SkipSam)
        }

        private fun showHint() {
            val g = game ?: return
            if (pendingHuman == null || g.turn != 0) return
            viewModelScope.launch {
                val legal = engine.legalActions(g, PlayerId(0))
                val suggestion =
                    withContext(Dispatchers.Default) {
                        bot.decide(engine.viewOf(g, PlayerId(0)), legal, BotLevel.NORMAL)
                    }
                val cards = (suggestion as? SlAction.Play)?.cards?.toSet().orEmpty()
                _state.update {
                    it.copy(
                        hintCards = cards,
                        selected = cards,
                        canPlay = canPlay(cards),
                        message = if (cards.isEmpty()) str(R.string.sl_status_pass) else null,
                    )
                }
            }
        }

        private var restored: SamLocState? = null

        /** Lưu bàn + ván đang chơi (null = đang nghỉ giữa hai ván). */
        private suspend fun persist(s: SamLocState?) {
            val table =
                SavedTable(
                    gameId = SamLocRules.GAME_ID,
                    players = playerCount,
                    difficulty = difficulty,
                    bet = betUnit,
                    botNames = botNames.toList(),
                    botBalances = botBalances.toList(),
                    round = previousWinner?.seat ?: -1,
                    stateJson = s?.let { saved.encode(SamLocState.serializer(), it) },
                )
            withContext(NonCancellable) { saved.saveTable(table) }
        }

        private fun balances(): Map<PlayerId, Long> =
            (0 until playerCount).associate { PlayerId(it) to if (it == 0) myBalance else botBalances[it - 1] }

        private suspend fun playOneGame(resumeFrom: SamLocState? = null) {
            newGameSignal = CompletableDeferred()
            log.clear()
            val seed = SecureRandom().nextLong()
            val table =
                TableConfig(
                    playerCount = playerCount,
                    rules = rules,
                    betUnit = betUnit,
                    minBalanceMultiplier = rules.minBalanceMultiplier,
                )
            val initial = resumeFrom ?: engine.startGame(table, seed, previousWinner)
            game = initial
            persist(initial)

            _state.update {
                it.copy(
                    dealing = true,
                    hand = initial.hands[0],
                    selected = emptySet(),
                    hintCards = emptySet(),
                    pile = CenterPileState(0, emptyList()),
                    banner = null,
                    flights = emptyList(),
                    result = null,
                    message = null,
                    phase = initial.phase,
                    samCaller = initial.samCaller,
                    seats = buildSeats(initial),
                    turnSeat = null,
                )
            }

            // Hiệu ứng chia bài
            delay(400)
            _state.update { it.copy(dealing = false) }

            // Sự kiện khởi đầu
            if (resumeFrom == null) consumeEvents(initial, engine.initialEvents(initial))

            if (initial.finished) {
                handleFinished(initial)
                return
            }

            // Vòng lặp ván bài
            var current = initial
            while (!current.finished) {
                val actor = current.turn
                val isHuman = actor == 0
                val timeoutMs =
                    if (current.phase == SlPhase.BAO_SAM) {
                        rules.samPhaseSeconds * 1000L
                    } else {
                        rules.turnSeconds * 1000L
                    }

                _state.update {
                    it.copy(
                        turnSeat = actor,
                        turnStartedAt = System.currentTimeMillis(),
                        turnMillis = timeoutMs,
                        isMyTurn = isHuman,
                        phase = current.phase,
                        samCaller = current.samCaller,
                        canCallSam = isHuman && current.phase == SlPhase.BAO_SAM,
                        canSkipSam = isHuman && current.phase == SlPhase.BAO_SAM,
                        canPass = isHuman && current.phase == SlPhase.PLAYING && current.top != null,
                        canPlay = isHuman && canPlay(it.selected),
                        seats = updateTurnInSeats(it.seats, actor),
                    )
                }

                val action: SlAction =
                    if (isHuman) {
                        pendingHuman = CompletableDeferred()
                        val chosen = withTimeoutOrNull(timeoutMs) { pendingHuman!!.await() }
                        pendingHuman = null
                        chosen ?: autoAction(current)
                    } else {
                        val delayTime = thinkDelay()
                        delay(delayTime)
                        val legal = engine.legalActions(current, PlayerId(actor))
                        withContext(Dispatchers.Default) {
                            bot.decide(engine.viewOf(current, PlayerId(actor)), legal, level)
                        }
                    }

                val transition = engine.apply(current, PlayerId(actor), action)
                current = transition.state
                game = current
                persist(current)

                _state.update {
                    it.copy(
                        hand = current.hands[0],
                        selected = emptySet(),
                        canPlay = false,
                        phase = current.phase,
                        samCaller = current.samCaller,
                        seats = buildSeats(current),
                    )
                }

                consumeEvents(current, transition.events)
            }

            handleFinished(current)
        }

        private fun autoAction(state: SamLocState): SlAction {
            if (state.phase == SlPhase.BAO_SAM) return SlAction.SkipSam
            val hand = state.hands[0]
            val top = state.top
            return if (top == null) {
                val minCard = hand.minByOrNull { it.slRank } ?: hand.first()
                SlAction.Play(listOf(minCard))
            } else {
                SlAction.Pass
            }
        }

        private fun thinkDelay(): Long {
            val think = ThinkTime.pick(settings.botSpeed.toBotSpeed(), random)
            return think
        }

        private suspend fun consumeEvents(
            state: SamLocState,
            events: List<GameEvent>,
        ) {
            for (ev in events) {
                when (ev) {
                    is GameEvent.Announced -> {
                        log += "${seatName(ev.player.seat)}: ${ev.message}"
                        showBanner(ev.message, null, true)
                        delay(800)
                    }

                    is GameEvent.Played -> {
                        log += "${seatName(ev.player.seat)} đánh: ${ev.description}"
                        val playedCards = ev.cards
                        val seat = ev.player.seat
                        _state.update {
                            it.copy(
                                pile =
                                    it.pile.copy(
                                        sets =
                                            it.pile.sets +
                                                PlayedSet(
                                                    id = nextId++,
                                                    cards = playedCards,
                                                    from = SlSeatLayout.direction(playerCount, seat),
                                                    rotation = random.nextFloat() * 16f - 8f,
                                                    jitter = Offset(random.nextFloat() * 16f - 8f, random.nextFloat() * 12f - 6f),
                                                ),
                                    ),
                            )
                        }
                        effects.send(SlEffect.Haptic(strong = false))
                        delay(300)
                    }

                    is GameEvent.Passed -> {
                        log += "${seatName(ev.player.seat)} bỏ lượt"
                    }

                    is GameEvent.Cut -> {
                        log += "${seatName(ev.cutter.seat)} chặt ${seatName(ev.victim.seat)} (${formatCoins(ev.amount)})"
                        showBanner(str(R.string.sl_banner_cut), "${seatName(ev.cutter.seat)} +${formatCoins(ev.amount)}", true)
                        effects.send(SlEffect.Haptic(strong = true))
                        delay(700)
                    }

                    is GameEvent.RoundStarted -> {
                        delay(400)
                        _state.update { it.copy(pile = CenterPileState(nextId++, emptyList())) }
                    }

                    is GameEvent.Finished -> {
                        log += "${seatName(ev.player.seat)} về Nhất!"
                    }

                    is GameEvent.Settled -> {
                        Unit
                    }

                    else -> {
                        Unit
                    }
                }
            }
        }

        private fun showBanner(
            title: String,
            subtitle: String?,
            positive: Boolean,
        ) {
            val banner = BannerData(nextId++, title, subtitle, positive)
            _state.update { it.copy(banner = banner) }
            viewModelScope.launch {
                delay(1500)
                _state.update { if (it.banner == banner) it.copy(banner = null) else it }
            }
        }

        private suspend fun handleFinished(finalState: SamLocState) {
            val raw = engine.settle(finalState)
            // Không ai mất quá số xu đang có (vẫn zero-sum)
            val settlement = raw.cappedBy(balances())
            val capped = settlement.lines != raw.lines
            previousWinner = if (finalState.winner != null) PlayerId(finalState.winner) else null

            // Cập nhật số dư xu
            val deltaHuman = settlement.deltas[PlayerId(0)] ?: 0L
            myBalance = wallet.applyGameResult(SamLocRules.GAME_ID, deltaHuman)

            for (i in 1 until playerCount) {
                val delta = settlement.deltas[PlayerId(i)] ?: 0L
                botBalances[i - 1] = (botBalances[i - 1] + delta).coerceAtLeast(0L)
            }
            // Bot hết xu → thay bot mới
            val notes = mutableListOf<String>()
            val minBalance = betUnit * rules.minBalanceMultiplier
            for (i in botBalances.indices) {
                if (botBalances[i] < minBalance) {
                    val old = botNames[i]
                    botNames[i] = BOT_NAMES.filter { it !in botNames }.randomOrNull(random) ?: old
                    botBalances[i] = freshBotBalance()
                    notes += "$old hết xu, ${botNames[i]} vào thay."
                }
            }

            val rows =
                (0 until playerCount).map { seat ->
                    val p = PlayerId(seat)
                    val d = settlement.deltas[p] ?: 0L
                    val isWin = finalState.winner == seat
                    val rankLabel = if (isWin) str(R.string.sl_rank_first) else str(R.string.sl_rank_last)
                    val linesForPlayer = settlement.lines.filter { it.from == p || it.to == p }
                    SlResultRow(
                        seat = seat,
                        name = seatName(seat),
                        rankLabel = rankLabel,
                        delta = d,
                        isHuman = seat == 0,
                        reasons = linesForPlayer.map { it.reason },
                    )
                }

            val canPlayAgain = myBalance >= minBalance
            if (!canPlayAgain) notes += str(R.string.sl_result_broke)
            if (canPlayAgain) persist(null) else withContext(NonCancellable) { saved.clear(SamLocRules.GAME_ID) }
            _state.update {
                it.copy(
                    result =
                        SlResultUi(
                            rows = rows,
                            capped = capped,
                            canPlayAgain = canPlayAgain,
                            note = notes.joinToString("\n").ifEmpty { null },
                        ),
                    turnSeat = null,
                    isMyTurn = false,
                    seats = buildFinishedSeats(finalState),
                )
            }
        }

        private fun buildSeats(state: SamLocState): List<SlSeatUi> =
            (0 until playerCount).map { seat ->
                val isHuman = seat == 0
                val isTurn = state.turn == seat
                val hasPassed = seat in state.passed
                val isBao1 = seat in state.baoMot
                val isBaoSam = state.samCaller == seat

                val status =
                    when {
                        isBaoSam -> str(R.string.sl_status_bao_sam)
                        isBao1 -> str(R.string.sl_status_bao_1)
                        hasPassed -> str(R.string.sl_status_pass)
                        else -> null
                    }

                SlSeatUi(
                    seat = seat,
                    info =
                        SeatInfo(
                            name = seatName(seat),
                            initial = seatName(seat).first().uppercase(),
                            coins = if (isHuman) myBalance else botBalances.getOrElse(seat - 1) { 0L },
                            cardCount = state.hands[seat].size,
                            status = status,
                            isTurn = isTurn,
                            isHuman = isHuman,
                            avatarColor = SL_SEAT_COLORS[seat % SL_SEAT_COLORS.size],
                        ),
                )
            }

        private fun buildFinishedSeats(state: SamLocState): List<SlSeatUi> =
            (0 until playerCount).map { seat ->
                val isHuman = seat == 0
                SlSeatUi(
                    seat = seat,
                    info =
                        SeatInfo(
                            name = seatName(seat),
                            initial = seatName(seat).first().uppercase(),
                            coins = if (isHuman) myBalance else botBalances.getOrElse(seat - 1) { 0L },
                            cardCount = state.hands[seat].size,
                            status = if (state.winner == seat) str(R.string.sl_status_win) else null,
                            isTurn = false,
                            isHuman = isHuman,
                            avatarColor = SL_SEAT_COLORS[seat % SL_SEAT_COLORS.size],
                        ),
                    revealed = if (isHuman) emptyList() else state.hands[seat],
                )
            }

        private fun updateTurnInSeats(
            seats: List<SlSeatUi>,
            turn: Int,
        ): List<SlSeatUi> = seats.map { it.copy(info = it.info.copy(isTurn = it.seat == turn)) }

        private fun seatName(seat: Int): String = if (seat == 0) str(R.string.sl_you) else botNames.getOrElse(seat - 1) { "Máy $seat" }

        private fun sortHand(
            hand: List<Card>,
            mode: SlSortMode,
        ): List<Card> =
            when (mode) {
                SlSortMode.BY_RANK -> hand.sortedSl()
                SlSortMode.BY_GROUP -> hand.sortedSl()
            }

        /** Thoát giữa ván = bị xử như cóng: trả mỗi đối thủ (cóng + heo mặc định) lá. */
        private fun exitPenalty(): Long = (rules.congCards + rules.defaultTwoCards) * betUnit * (playerCount - 1)

        private fun confirmExit() {
            val g = game
            _state.update { it.copy(showExitConfirm = false) }
            viewModelScope.launch {
                loopJob?.cancel()
                if (g != null && !g.finished) {
                    val perOpponent = (rules.congCards + rules.defaultTwoCards) * betUnit
                    val lines = (1 until playerCount).map {
                        SettlementLine(PlayerId(0), PlayerId(it), perOpponent, "Thoát giữa ván (xử cóng)")
                    }
                    val settlement = Settlement.fromLines((0 until playerCount).map { PlayerId(it) }, lines).cappedBy(balances())
                    myBalance = wallet.applyGameResult(SamLocRules.GAME_ID, settlement.deltas[PlayerId(0)] ?: 0L)
                }
                saved.clear(SamLocRules.GAME_ID)
                effects.send(SlEffect.Exit)
            }
        }

        override fun onCleared() {
            loopJob?.cancel()
            super.onCleared()
        }

        companion object {
            private val BOT_NAMES = listOf("Bảo Long", "Minh Hùng", "Tuấn Tú", "Thanh Sơn", "Huy Hoàng", "Đức Anh", "Quốc Bảo", "Gia Huy")

            fun Speed.toBotSpeed(): BotSpeed =
                when (this) {
                    Speed.SLOW -> BotSpeed.SLOW
                    Speed.NORMAL -> BotSpeed.NORMAL
                    Speed.FAST -> BotSpeed.FAST
                }

            fun AppSettings.toCardStyle(): CardStyle =
                CardStyle(
                    fourColor = fourColorDeck,
                    large = largeCards,
                    back =
                        when (cardBack) {
                            CardBackStyle.CLASSIC_RED -> CardBackDesign.CLASSIC_RED
                            CardBackStyle.ROYAL_BLUE -> CardBackDesign.ROYAL_BLUE
                            CardBackStyle.JADE -> CardBackDesign.JADE
                        },
                )

            fun TableSkin.toFelt(): FeltColor =
                when (this) {
                    TableSkin.GREEN -> FeltColor.GREEN
                    TableSkin.RED -> FeltColor.RED
                    TableSkin.BLUE -> FeltColor.BLUE
                }
        }
    }
