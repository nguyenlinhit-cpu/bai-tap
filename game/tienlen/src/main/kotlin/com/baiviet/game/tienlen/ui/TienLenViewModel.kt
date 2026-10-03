package com.baiviet.game.tienlen.ui

import com.baiviet.core.data.SavedGameRepository
import com.baiviet.core.data.SavedTable
import kotlinx.coroutines.NonCancellable
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
import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.core.ui.card.CardBackDesign
import com.baiviet.core.ui.card.CardStyle
import com.baiviet.core.ui.effects.BannerData
import com.baiviet.core.ui.effects.CoinFlight
import com.baiviet.core.ui.table.CenterPileState
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.table.PlayedSet
import com.baiviet.core.ui.table.SeatInfo
import com.baiviet.core.ui.theme.formatCoins
import com.baiviet.game.tienlen.R
import com.baiviet.game.tienlen.bot.HandAnalyzer
import com.baiviet.game.tienlen.bot.TienLenBot
import com.baiviet.game.tienlen.engine.TienLenEngine
import com.baiviet.game.tienlen.engine.TienLenState
import com.baiviet.game.tienlen.engine.TlAction
import com.baiviet.game.tienlen.rules.Combo
import com.baiviet.game.tienlen.rules.TienLenRules
import com.baiviet.game.tienlen.rules.sortedTl
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
 * Game loop Tiến lên: lấy người đến lượt → người thật thì chờ Intent (có đồng hồ),
 * bot thì `decide` trên Dispatchers.Default kèm thời gian "suy nghĩ" → apply → phát sự kiện
 * cho UI chạy animation tuần tự. Hủy đúng cách khi thoát bàn (viewModelScope).
 */
@HiltViewModel
class TienLenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallet: WalletRepository,
    private val settingsRepo: SettingsRepository,
    private val houseRules: HouseRulesRepository,
    private val saved: SavedGameRepository,
) : ViewModel() {

    private val engine = TienLenEngine()
    private val bot = TienLenBot()
    private val random = Random(SecureRandom().nextLong())

    private val _state = MutableStateFlow(TlUiState())
    val state: StateFlow<TlUiState> = _state.asStateFlow()

    private val effects = Channel<TlEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()

    private var started = false
    private var loopJob: Job? = null
    private lateinit var settings: AppSettings
    private lateinit var rules: TienLenRules
    private var playerCount = 4
    private var level = BotLevel.NORMAL
    private var betUnit = 100L
    private var difficulty = 1

    private var game: TienLenState? = null
    private var pendingHuman: CompletableDeferred<TlAction>? = null
    private var newGameSignal = CompletableDeferred<Unit>()
    private var previousWinner: PlayerId? = null

    private var myBalance = 0L
    private val botNames = mutableListOf<String>()
    private val botBalances = mutableListOf<Long>()
    private val log = mutableListOf<String>()
    private var nextId = 1

    private fun str(id: Int, vararg args: Any): String = context.getString(id, *args)

    // ───────────────────────── Khởi tạo ─────────────────────────

    fun start(players: Int, difficulty: Int, bet: Long) {
        if (started) return
        started = true
        playerCount = players.coerceIn(2, 4)
        this.difficulty = difficulty
        level = BotLevel.entries.getOrElse(difficulty) { BotLevel.NORMAL }
        betUnit = bet
        loopJob = viewModelScope.launch {
            settings = settingsRepo.settings.first()
            rules = houseRules.current(TienLenRules.GAME_ID, TienLenRules.serializer(), TienLenRules.DEFAULT)
            wallet.ensureProfile()
            myBalance = wallet.current()
            // Ván dở lần trước (app bị tắt giữa chừng) → khôi phục bàn
            val resume = saved.loadTable(TienLenRules.GAME_ID)?.takeIf { it.botNames.size == it.players - 1 }
            if (resume != null) {
                playerCount = resume.players.coerceIn(2, 4)
                this@TienLenViewModel.difficulty = resume.difficulty
                level = BotLevel.entries.getOrElse(resume.difficulty) { BotLevel.NORMAL }
                betUnit = resume.bet
                botNames += resume.botNames
                botBalances += resume.botBalances
                previousWinner = resume.round.takeIf { it >= 0 }?.let { PlayerId(it) }
            } else {
                val names = context.resources.getStringArray(R.array.tl_bot_names).toMutableList().also { it.shuffle(random) }
                repeat(playerCount - 1) {
                    botNames += names[it]
                    botBalances += freshBotBalance()
                }
            }
            var restored = saved.decode(TienLenState.serializer(), resume?.stateJson)
                ?.takeIf { it.playerCount == playerCount && !it.finished }
            _state.update {
                it.copy(
                    loading = false,
                    playerCount = playerCount,
                    betUnit = betUnit,
                    rules = rules,
                    cardStyle = settings.toCardStyle(),
                    felt = settings.tableSkin.toFelt(),
                    showGuide = TienLenRules.GAME_ID !in settings.seenGuides,
                    turnMillis = rules.turnSeconds * 1000L,
                    soundOn = settings.sfx,
                )
            }
            if (TienLenRules.GAME_ID !in settings.seenGuides) {
                // Chờ người chơi đọc xong hướng dẫn nhanh lần đầu
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

    // ───────────────────────── Intent ─────────────────────────

    fun onIntent(intent: TlIntent) {
        when (intent) {
            is TlIntent.Toggle -> updateSelection { if (intent.card in it) it - intent.card else it + intent.card }
            is TlIntent.Sweep -> updateSelection { sel ->
                val all = intent.cards.toSet()
                if (all.all { it in sel }) sel - all else sel + all
            }
            TlIntent.Play -> submitPlay()
            TlIntent.Pass -> submitPass()
            TlIntent.Hint -> showHint()
            TlIntent.Sort -> {
                val mode = if (_state.value.sortMode == SortMode.BY_RANK) SortMode.BY_GROUP else SortMode.BY_RANK
                _state.update { it.copy(sortMode = mode, hand = sortHand(it.hand, mode)) }
            }
            TlIntent.NewGame -> {
                if (_state.value.result?.canPlayAgain == true) {
                    _state.update { it.copy(result = null) }
                    newGameSignal.complete(Unit)
                }
            }
            TlIntent.RequestExit -> {
                val g = game
                if (g == null || g.finished || _state.value.result != null) {
                    viewModelScope.launch {
                        loopJob?.cancel()
                        saved.clear(TienLenRules.GAME_ID)
                        effects.send(TlEffect.Exit)
                    }
                } else {
                    val penalty = -(engine.forfeit(g, PlayerId(0)).cappedBy(balances()).deltas[PlayerId(0)] ?: 0L)
                    _state.update { it.copy(showExitConfirm = true, exitPenalty = penalty) }
                }
            }
            TlIntent.CancelExit -> _state.update { it.copy(showExitConfirm = false) }
            TlIntent.ConfirmExit -> confirmExit()
            TlIntent.DismissGuide -> {
                _state.update { it.copy(showGuide = false) }
                viewModelScope.launch { settingsRepo.markGuideSeen(TienLenRules.GAME_ID) }
            }
            TlIntent.ToggleSound -> {
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
        return engine.checkPlay(g, 0, selected) != null
    }

    private fun submitPlay() {
        val g = game ?: return
        val pending = pendingHuman ?: return
        val sel = _state.value.selected
        if (sel.isEmpty()) {
            _state.update { it.copy(message = str(R.string.tl_select_cards)) }
            return
        }
        val combo = engine.checkPlay(g, 0, sel)
        if (combo == null) {
            val msg = when {
                g.mustInclude != null && g.mustInclude !in sel -> str(R.string.tl_must_include, g.mustInclude.shortName)
                Combo.classify(sel) == null -> str(R.string.tl_invalid_combo)
                else -> str(R.string.tl_cannot_beat)
            }
            _state.update { it.copy(message = msg) }
            return
        }
        pending.complete(TlAction.Play(combo.cards))
    }

    private fun submitPass() {
        val g = game ?: return
        if (!engine.canPass(g, 0)) return
        pendingHuman?.complete(TlAction.Pass)
    }

    private fun showHint() {
        val g = game ?: return
        if (pendingHuman == null || g.turn != 0) return
        viewModelScope.launch {
            val legal = engine.legalActions(g, PlayerId(0))
            val usable = legal.filterIsInstance<TlAction.Play>().flatMap { it.cards }.toSet()
            val suggestion = withContext(Dispatchers.Default) {
                bot.decide(engine.viewOf(g, PlayerId(0)), legal, BotLevel.NORMAL)
            }
            val cards = (suggestion as? TlAction.Play)?.cards?.toSet().orEmpty()
            _state.update {
                it.copy(
                    hintCards = cards,
                    selected = cards,
                    canPlay = canPlay(cards),
                    dimCards = it.hand.toSet() - usable,
                    message = if (cards.isEmpty()) str(R.string.tl_status_pass) else null,
                )
            }
        }
    }

    private fun confirmExit() {
        val g = game
        _state.update { it.copy(showExitConfirm = false) }
        viewModelScope.launch {
            if (g != null && !g.finished) {
                val settlement = engine.forfeit(g, PlayerId(0)).cappedBy(balances())
                wallet.applyGameResult(TienLenRules.GAME_ID, settlement.deltas[PlayerId(0)] ?: 0L)
            }
            loopJob?.cancel()
            saved.clear(TienLenRules.GAME_ID)
            effects.send(TlEffect.Exit)
        }
    }

    // ───────────────────────── Vòng ván ─────────────────────────

    /** Lưu bàn + ván đang chơi (null = đang nghỉ giữa hai ván). */
    private suspend fun persist(s: TienLenState?) {
        val table = SavedTable(
            gameId = TienLenRules.GAME_ID,
            players = playerCount,
            difficulty = difficulty,
            bet = betUnit,
            botNames = botNames.toList(),
            botBalances = botBalances.toList(),
            round = previousWinner?.seat ?: -1,
            stateJson = s?.let { saved.encode(TienLenState.serializer(), it) },
        )
        withContext(NonCancellable) { saved.saveTable(table) }
    }

    private suspend fun playOneGame(restored: TienLenState? = null) {
        log.clear()
        val table = TableConfig(playerCount, rules, betUnit, rules.minBalanceMultiplier)
        var s = restored ?: engine.startGame(table, SecureRandom().nextLong(), previousWinner)
        game = s
        persist(s)
        val roundId = nextId++
        _state.update {
            it.copy(
                pile = CenterPileState(roundId, emptyList()),
                hand = emptyList(),
                selected = emptySet(),
                hintCards = emptySet(),
                dimCards = emptySet(),
                result = null,
                flights = emptyList(),
                banner = null,
                dealing = true,
                message = str(R.string.tl_dealing),
                turnSeat = null,
            )
        }
        refreshSeats(s, dealt = 0)

        // Chia bài: lá bay tới từng ghế, so le
        val stagger = when (settings.dealSpeed) {
            Speed.SLOW -> 90L
            Speed.NORMAL -> 55L
            Speed.FAST -> 28L
        }
        val myCards = s.hands[0]
        if (restored == null) {
            for (i in 1..13) {
                val shown = myCards.take(i)
                _state.update { it.copy(hand = if (settings.autoSort) shown.sortedTl() else shown) }
                refreshSeats(s, dealt = i)
                delay(stagger * playerCount)
            }
        }
        _state.update { it.copy(hand = sortHand(myCards, it.sortMode), dealing = false, message = null) }
        refreshSeats(s)
        if (restored == null) {
            addLog("Chia bài. ${seatName(s.turn)} đi trước${s.mustInclude?.let { " (phải có ${it.shortName})" } ?: ""}.")
            engine.initialEvents(s).forEach { handleEvent(it, s) }
        } else {
            addLog("Tiếp tục ván đang dở.")
            val top = s.top
            val owner = s.topOwner
            if (top != null && owner != null) {
                val set = PlayedSet(nextId++, top.cards, SeatLayout.direction(playerCount, owner), 0f, Offset.Zero)
                _state.update { it.copy(pile = it.pile.copy(sets = listOf(set))) }
            }
        }

        while (!engine.isFinished(s)) {
            val seat = s.turn
            val action = if (seat == 0) awaitHuman(s) else botAction(s, seat)
            val t = try {
                engine.apply(s, PlayerId(seat), action)
            } catch (e: IllegalActionException) {
                // Phòng hờ: hành động sai luật → hành động an toàn
                engine.apply(s, PlayerId(seat), engine.timeoutAction(s, seat))
            }
            s = t.state
            game = s
            persist(s)
            for (event in t.events) handleEvent(event, s)
            refreshSeats(s)
        }
        finishGame(s)
    }

    private suspend fun awaitHuman(s: TienLenState): TlAction {
        val deferred = CompletableDeferred<TlAction>()
        pendingHuman = deferred
        _state.update {
            it.copy(
                turnSeat = 0,
                turnStartedAt = System.currentTimeMillis(),
                isMyTurn = true,
                canPass = engine.canPass(s, 0),
                canPlay = canPlay(it.selected),
                message = when {
                    s.mustInclude != null -> str(R.string.tl_must_include, s.mustInclude.shortName)
                    s.top == null -> str(R.string.tl_free_turn)
                    else -> str(R.string.tl_your_turn)
                },
            )
        }
        effects.send(TlEffect.Haptic(strong = false))
        val action = withTimeoutOrNull(rules.turnSeconds * 1000L) { deferred.await() }
            ?: engine.timeoutAction(s, 0).also { auto ->
                val what = if (auto == TlAction.Pass) str(R.string.tl_pass) else str(R.string.tl_play)
                _state.update { it.copy(message = str(R.string.tl_timeout, what.lowercase())) }
            }
        pendingHuman = null
        _state.update { it.copy(isMyTurn = false, canPlay = false, canPass = false, hintCards = emptySet(), dimCards = emptySet()) }
        return action
    }

    private suspend fun botAction(s: TienLenState, seat: Int): TlAction {
        _state.update { it.copy(turnSeat = seat, turnStartedAt = System.currentTimeMillis()) }
        val begin = System.currentTimeMillis()
        val think = ThinkTime.pick(settings.botSpeed.toBotSpeed(), random)
        val view = engine.viewOf(s, PlayerId(seat))
        val legal = engine.legalActions(s, PlayerId(seat))
        val action = withContext(Dispatchers.Default) { bot.decide(view, legal, level) }
        val elapsed = System.currentTimeMillis() - begin
        if (elapsed < think) delay(think - elapsed)
        return action
    }

    private suspend fun handleEvent(event: GameEvent, s: TienLenState) {
        when (event) {
            is GameEvent.Played -> {
                val seat = event.player.seat
                val set = PlayedSet(
                    id = nextId++,
                    cards = event.cards,
                    from = SeatLayout.direction(playerCount, seat),
                    rotation = random.nextFloat() * 16f - 8f,
                    jitter = Offset(random.nextFloat() * 16f - 8f, random.nextFloat() * 12f - 6f),
                )
                _state.update { st ->
                    val hand = if (seat == 0) st.hand - event.cards.toSet() else st.hand
                    st.copy(
                        pile = st.pile.copy(sets = st.pile.sets + set),
                        hand = hand,
                        selected = if (seat == 0) emptySet() else st.selected,
                        message = null,
                    )
                }
                addLog("${seatName(seat)} đánh ${event.description}: ${event.cards.joinToString(" ")}")
                delay(360)
            }
            is GameEvent.Passed -> {
                addLog("${seatName(event.player.seat)} bỏ lượt")
                delay(220)
            }
            is GameEvent.Cut -> {
                val involvesTwo = event.description.contains("Heo") || event.description.contains("heo")
                val title = when {
                    event.description.startsWith("Chặt chồng") -> str(R.string.tl_banner_chain)
                    involvesTwo -> str(R.string.tl_banner_cut_two)
                    else -> str(R.string.tl_banner_cut)
                }
                showBanner(
                    title,
                    "${seatName(event.cutter.seat)} +${formatCoins(event.amount)}",
                    positive = event.cutter.seat == 0 || event.victim.seat != 0,
                )
                addLog("${event.description} — ${seatName(event.victim.seat)} sẽ trả ${formatCoins(event.amount)} xu")
                effects.send(TlEffect.Haptic(strong = true))
                delay(1300)
            }
            is GameEvent.RoundStarted -> {
                delay(350)
                _state.update { it.copy(pile = CenterPileState(nextId++, emptyList())) }
                addLog("Vòng mới — ${seatName(event.leader.seat)} đi tự do")
                delay(250)
            }
            is GameEvent.Finished -> {
                addLog("${seatName(event.player.seat)} về ${rankLabel(event.rank - 1)}")
                if (event.player.seat == 0 && event.rank == 1) effects.send(TlEffect.Haptic(strong = true))
            }
            is GameEvent.Announced -> {
                if (event.message.startsWith("Tới trắng")) {
                    showBanner(str(R.string.tl_banner_instant), "${seatName(event.player.seat)}: ${event.message.substringAfter(": ")}", true)
                    effects.send(TlEffect.Haptic(strong = true))
                    delay(1800)
                } else if (event.message == "Cóng") {
                    showBanner(str(R.string.tl_banner_cong), seatName(event.player.seat), positive = false)
                    delay(1200)
                }
                addLog("${seatName(event.player.seat)}: ${event.message}")
            }
            else -> Unit
        }
    }

    private suspend fun finishGame(s: TienLenState) {
        _state.update { it.copy(turnSeat = null, isMyTurn = false, canPass = false, canPlay = false) }
        val raw = engine.settle(s)
        val settlement = raw.cappedBy(balances())
        val capped = settlement.lines != raw.lines

        // Cập nhật ví
        val myDelta = settlement.deltas[PlayerId(0)] ?: 0L
        myBalance = wallet.applyGameResult(TienLenRules.GAME_ID, myDelta)
        for (i in 1 until playerCount) botBalances[i - 1] = (botBalances[i - 1] + (settlement.deltas[PlayerId(i)] ?: 0L)).coerceAtLeast(0L)
        persist(null)

        // Lật bài còn lại của bot
        refreshSeats(s, reveal = true)

        // Chip bay
        val anchors = SeatLayout.anchors(playerCount)
        val flights = settlement.lines.filter { it.from != null && it.to != null }.take(12).flatMapIndexed { idx, line ->
            List(3) { k ->
                CoinFlight(nextId++, anchors[line.from!!.seat], anchors[line.to!!.seat], idx * 120 + k * 70)
            }
        }
        _state.update { it.copy(flights = flights) }

        val ranking = s.instantWin?.let { listOf(it.player) + (0 until playerCount).filter { p -> p != it.player } }
            ?: engine.finalRanking(s)
        previousWinner = PlayerId(ranking.first())
        if (ranking.first() == 0) {
            showBanner(str(R.string.tl_banner_win), "+${formatCoins(myDelta)}", true)
        } else if (myDelta < 0) {
            showBanner(str(R.string.tl_banner_lose), formatCoins(myDelta), false)
        }
        addLog("Kết thúc ván: " + ranking.joinToString(" › ") { seatName(it) })
        delay(1600)

        // Bot hết xu → thay bot mới
        val notes = mutableListOf<String>()
        val minBalance = betUnit * rules.minBalanceMultiplier
        for (i in botBalances.indices) {
            if (botBalances[i] < minBalance) {
                val old = botNames[i]
                val pool = context.resources.getStringArray(R.array.tl_bot_names).filter { it !in botNames }
                botNames[i] = pool.randomOrNull(random) ?: old
                botBalances[i] = freshBotBalance()
                notes += str(R.string.tl_bot_replaced, old, botNames[i])
            }
        }
        val canAgain = myBalance >= minBalance
        if (canAgain) persist(null) else withContext(NonCancellable) { saved.clear(TienLenRules.GAME_ID) }
        if (!canAgain) notes += str(R.string.tl_result_broke)
        refreshSeats(s, reveal = true)

        newGameSignal = CompletableDeferred()
        val rows = ranking.mapIndexed { pos, seat ->
            ResultRow(
                seat = seat,
                name = seatName(seat),
                rankLabel = rankLabel(pos),
                delta = settlement.deltas[PlayerId(seat)] ?: 0L,
                isHuman = seat == 0,
                reasons = settlement.lines.filter { it.from?.seat == seat || it.to?.seat == seat }.map { line ->
                    val sign = if (line.to?.seat == seat) "+" else "−"
                    val other = if (line.to?.seat == seat) line.from else line.to
                    "${line.reason} ${other?.let { "(${seatName(it.seat)})" } ?: ""}: $sign${formatCoins(line.amount)}"
                },
            )
        }
        _state.update {
            it.copy(
                result = ResultUi(rows, capped, canAgain, notes.joinToString("\n").ifEmpty { null }),
                banner = null,
                flights = emptyList(),
            )
        }
    }

    private fun balances(): Map<PlayerId, Long> =
        (0 until playerCount).associate { PlayerId(it) to if (it == 0) myBalance else botBalances[it - 1] }

    // ───────────────────────── Hiển thị ─────────────────────────

    private fun refreshSeats(s: TienLenState, dealt: Int? = null, reveal: Boolean = false) {
        val ranking = s.finishOrder
        val seats = (0 until playerCount).map { seat ->
            val count = dealt ?: s.hands[seat].size
            val status = when {
                dealt != null -> null
                seat in s.cong -> str(R.string.tl_status_cong)
                seat in ranking -> str(R.string.tl_status_rank, rankLabel(ranking.indexOf(seat)))
                s.finished && seat !in s.hasPlayed -> str(R.string.tl_status_cong)
                seat in s.passed && !s.finished -> str(R.string.tl_status_pass)
                count == 1 && !s.finished -> str(R.string.tl_status_last_card)
                else -> null
            }
            SeatUi(
                seat = seat,
                info = SeatInfo(
                    name = seatName(seat),
                    initial = seatName(seat).first().uppercase(),
                    coins = if (seat == 0) myBalance else botBalances[seat - 1],
                    cardCount = count,
                    status = status,
                    isTurn = !s.finished && dealt == null && s.turn == seat,
                    isHuman = seat == 0,
                    avatarColor = SEAT_COLORS[seat % SEAT_COLORS.size],
                    highlight = seat in ranking && ranking.indexOf(seat) == 0,
                ),
                revealed = if (reveal && seat != 0) s.hands[seat] else emptyList(),
            )
        }
        _state.update { it.copy(seats = seats) }
    }

    private fun sortHand(hand: List<Card>, mode: SortMode): List<Card> = when (mode) {
        SortMode.BY_RANK -> hand.sortedTl()
        SortMode.BY_GROUP -> HandAnalyzer.decompose(hand).flatMap { it.cards }
    }

    private fun seatName(seat: Int): String = if (seat == 0) str(R.string.tl_you) else botNames[seat - 1]

    private fun rankLabel(position: Int): String = when {
        position == 0 -> str(R.string.tl_rank_first)
        position == playerCount - 1 -> str(R.string.tl_rank_last)
        position == 1 -> str(R.string.tl_rank_second)
        else -> str(R.string.tl_rank_third)
    }

    private fun showBanner(title: String, subtitle: String?, positive: Boolean) {
        _state.update { it.copy(banner = BannerData(nextId++, title, subtitle, positive)) }
        viewModelScope.launch {
            val id = _state.value.banner?.id
            delay(1700)
            _state.update { if (it.banner?.id == id) it.copy(banner = null) else it }
        }
    }

    private fun addLog(line: String) {
        log += line
        _state.update { it.copy(log = log.toList()) }
    }

    override fun onCleared() {
        loopJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun Speed.toBotSpeed(): BotSpeed = when (this) {
            Speed.SLOW -> BotSpeed.SLOW
            Speed.NORMAL -> BotSpeed.NORMAL
            Speed.FAST -> BotSpeed.FAST
        }

        fun AppSettings.toCardStyle(): CardStyle = CardStyle(
            fourColor = fourColorDeck,
            large = largeCards,
            back = when (cardBack) {
                CardBackStyle.CLASSIC_RED -> CardBackDesign.CLASSIC_RED
                CardBackStyle.ROYAL_BLUE -> CardBackDesign.ROYAL_BLUE
                CardBackStyle.JADE -> CardBackDesign.JADE
            },
        )

        fun TableSkin.toFelt(): FeltColor = when (this) {
            TableSkin.GREEN -> FeltColor.GREEN
            TableSkin.RED -> FeltColor.RED
            TableSkin.BLUE -> FeltColor.BLUE
        }
    }
}
