package com.baiviet.game.tienlen.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Deck
import com.baiviet.core.engine.GameEngine
import com.baiviet.core.engine.GameEvent
import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.SettlementLine
import com.baiviet.core.engine.TableConfig
import com.baiviet.core.engine.Transition
import com.baiviet.game.tienlen.rules.BeatRules
import com.baiviet.game.tienlen.rules.Combo
import com.baiviet.game.tienlen.rules.ComboFinder
import com.baiviet.game.tienlen.rules.InstantWin
import com.baiviet.game.tienlen.rules.InstantWinType
import com.baiviet.game.tienlen.rules.Penalties
import com.baiviet.game.tienlen.rules.TienLenRules
import com.baiviet.game.tienlen.rules.TlScoring
import com.baiviet.game.tienlen.rules.sortedTl
import com.baiviet.game.tienlen.rules.tlValue

/**
 * Engine luật Tiến lên miền Nam — Kotlin thuần, không phụ thuộc Android.
 */
class TienLenEngine : GameEngine<TienLenState, TlAction, TienLenView, TienLenRules> {

    /** Ván đầu tiên của bàn (người giữ 3♠ đi trước). */
    override fun start(table: TableConfig<TienLenRules>, seed: Long): TienLenState =
        startGame(table, seed, previousWinner = null)

    /**
     * Bắt đầu ván.
     *
     * @param previousWinner người về Nhất ván trước — đi trước, đánh tự do. Null = ván đầu tiên.
     */
    fun startGame(table: TableConfig<TienLenRules>, seed: Long, previousWinner: PlayerId?): TienLenState {
        val (_, deck) = Deck.shuffled(seed)
        val (hands, _) = Deck.deal(deck, table.playerCount, CARDS_PER_PLAYER)
        return startWithHands(table.rules, table.playerCount, table.betUnit, hands, previousWinner, seed)
    }

    /** Bắt đầu ván với bài sắp sẵn (ván tập, test). */
    fun startWithHands(
        rules: TienLenRules,
        playerCount: Int,
        betUnit: Long,
        hands: List<List<Card>>,
        previousWinner: PlayerId? = null,
        seed: Long = 0L,
    ): TienLenState {
        require(hands.size == playerCount) { "Số tay bài phải bằng số người" }
        val isFirstGame = previousWinner == null
        val sortedHands = hands.map { it.sortedTl() }

        val leader: Int
        val mustInclude: Card?
        if (!isFirstGame && previousWinner!!.seat in 0 until playerCount) {
            leader = previousWinner.seat
            mustInclude = null
        } else {
            val lowest = sortedHands.flatten().minBy { it.tlValue }
            leader = sortedHands.indexOfFirst { lowest in it }
            mustInclude = if (rules.require3SpadesOnFirstMove) lowest else null
        }

        val base = TienLenState(
            rules = rules,
            playerCount = playerCount,
            betUnit = betUnit,
            seed = seed,
            isFirstGame = isFirstGame,
            hands = sortedHands,
            turn = leader,
            mustInclude = mustInclude,
        )

        val instant = (0 until playerCount)
            .mapNotNull { seat -> InstantWin.detect(sortedHands[seat], rules, isFirstGame)?.let { seat to it } }
            .toMap()
        val winner = InstantWin.resolve(instant, sortedHands) ?: return base
        return base.copy(
            finished = true,
            turn = winner,
            instantWin = InstantWinResult(winner, instant.getValue(winner)),
            finishOrder = listOf(winner),
        )
    }

    /** Sự kiện mở đầu ván (chia bài, tới trắng). */
    fun initialEvents(state: TienLenState): List<GameEvent> = buildList {
        add(GameEvent.Dealt((0 until state.playerCount).associate { PlayerId(it) to state.hands[it].size }))
        state.instantWin?.let {
            add(GameEvent.Announced(PlayerId(it.player), "Tới trắng: ${it.type.viName}"))
        }
    }

    override fun currentActors(state: TienLenState): Set<PlayerId> =
        if (state.finished) emptySet() else setOf(PlayerId(state.turn))

    override fun legalActions(state: TienLenState, player: PlayerId): List<TlAction> {
        if (state.finished || player.seat != state.turn) return emptyList()
        val hand = state.hands[player.seat]
        val top = state.top
        if (top == null) {
            return ComboFinder.all(hand)
                .filter { state.mustInclude == null || state.mustInclude in it.cards }
                .filter { !isForbiddenFinish(state, hand, it) }
                .map { TlAction.Play(it.cards) }
        }
        val outOfTurn = player.seat in state.passed
        val plays = ComboFinder.beating(hand, top, state.rules, outOfTurnOnly = outOfTurn)
            .filter { !isForbiddenFinish(state, hand, it) }
            .map { TlAction.Play(it.cards) }
        return listOf(TlAction.Pass) + plays
    }

    /**
     * Kiểm tra một nước đánh, trả về bộ bài nếu hợp lệ, null nếu không.
     * Dùng cho UI bật/tắt nút "Đánh".
     */
    fun checkPlay(state: TienLenState, seat: Int, cards: Collection<Card>): Combo? =
        runCatching { validatePlay(state, seat, cards) }.getOrNull()

    /** Có được bỏ lượt không (không được bỏ khi đang đi tự do). */
    fun canPass(state: TienLenState, seat: Int): Boolean =
        !state.finished && state.turn == seat && state.top != null

    override fun apply(state: TienLenState, player: PlayerId, action: TlAction): Transition<TienLenState> {
        if (state.finished) throw IllegalActionException("Ván đã kết thúc")
        val seat = player.seat
        if (seat != state.turn) throw IllegalActionException("Chưa đến lượt $player")
        return when (action) {
            is TlAction.Play -> applyPlay(state, seat, action.cards)
            TlAction.Pass -> applyPass(state, seat)
        }
    }

    private fun applyPlay(state: TienLenState, seat: Int, cards: List<Card>): Transition<TienLenState> {
        val combo = validatePlay(state, seat, cards)
        val events = mutableListOf<GameEvent>()
        events += GameEvent.Played(PlayerId(seat), combo.cards, combo.description)

        var chain = state.cutChain
        var topIsCut = false
        val top = state.top
        if (top != null && BeatRules.isCut(combo, top, state.rules)) {
            val victim = state.topOwner!!
            val base = if (state.topIsCut) chain?.amount ?: 0L else 0L
            val amount = base + Penalties.comboXu(top, state.rules, state.betUnit)
            val desc = if (state.topIsCut) {
                "Chặt chồng: ${combo.description} chặt ${top.description}"
            } else {
                "${combo.description} chặt ${top.description}"
            }
            chain = CutChain(cutter = seat, victim = victim, amount = amount, description = desc)
            topIsCut = true
            events += GameEvent.Cut(PlayerId(seat), PlayerId(victim), amount, desc)
        }

        val newHand = state.hands[seat] - combo.cards.toSet()
        var s = state.copy(
            hands = state.hands.mapIndexed { i, h -> if (i == seat) newHand else h },
            top = combo,
            topOwner = seat,
            topIsCut = topIsCut,
            cutChain = chain,
            passed = state.passed - seat,
            declined = emptySet(),
            mustInclude = null,
            hasPlayed = state.hasPlayed + seat,
            playedCards = state.playedCards + combo.cards,
            moveCount = state.moveCount + 1,
        )

        if (newHand.isEmpty()) {
            s = s.copy(finishOrder = s.finishOrder + seat)
            events += GameEvent.Finished(PlayerId(seat), s.finishOrder.size)
            if (s.finishOrder.size == 1) {
                val congSeats = (0 until s.playerCount).filter { it != seat && it !in s.hasPlayed }
                congSeats.forEach { events += GameEvent.Announced(PlayerId(it), "Cóng") }
                if (s.rules.scoring == TlScoring.RANKING) s = s.copy(cong = congSeats.toSet())
            }
            if (s.rules.scoring == TlScoring.COUNT_CARDS || s.activeSeats.size <= 1) {
                return Transition(finish(s), events)
            }
        }
        return advance(s, seat, events)
    }

    private fun applyPass(state: TienLenState, seat: Int): Transition<TienLenState> {
        if (state.top == null) throw IllegalActionException("Đang đi tự do, không được bỏ lượt")
        val s = if (seat in state.passed) {
            state.copy(declined = state.declined + seat)
        } else {
            state.copy(passed = state.passed + seat)
        }
        return advance(s, seat, mutableListOf(GameEvent.Passed(PlayerId(seat))))
    }

    private fun validatePlay(state: TienLenState, seat: Int, cards: Collection<Card>): Combo {
        val hand = state.hands[seat]
        if (cards.toSet().size != cards.size) throw IllegalActionException("Lá bài trùng")
        if (!hand.containsAll(cards)) throw IllegalActionException("Không có các lá này trên tay")
        val combo = Combo.classify(cards) ?: throw IllegalActionException("Không phải bộ hợp lệ")
        state.mustInclude?.let {
            if (it !in combo.cards) throw IllegalActionException("Nước đầu phải có ${it.shortName}")
        }
        if (isForbiddenFinish(state, hand, combo)) throw IllegalActionException("Không được về bằng heo")
        val top = state.top ?: return combo
        val ok = if (seat in state.passed) {
            BeatRules.isOutOfTurnCut(combo, top, state.rules)
        } else {
            BeatRules.canBeat(combo, top, state.rules)
        }
        if (!ok) throw IllegalActionException("${combo.description} không chặn được ${top.description}")
        return combo
    }

    private fun isForbiddenFinish(state: TienLenState, hand: List<Card>, combo: Combo): Boolean =
        state.rules.forbidFinishWithTwo &&
            combo.size == hand.size &&
            combo.hasTwo &&
            !(state.top == null && hand.size == 1)

    /** Chuyển lượt cho người kế tiếp, hoặc mở vòng mới nếu mọi người đã bỏ. */
    private fun advance(s: TienLenState, from: Int, events: MutableList<GameEvent>): Transition<TienLenState> {
        val owner = s.topOwner ?: return Transition(s, events)
        for (k in 1..s.playerCount) {
            val i = (from + k) % s.playerCount
            if (i == owner) break
            if (!s.isActive(i)) continue
            if (i !in s.passed) return Transition(s.copy(turn = i), events)
            if (i !in s.declined && canCutOutOfTurn(s, i)) return Transition(s.copy(turn = i), events)
        }
        return newRound(s, owner, events)
    }

    private fun canCutOutOfTurn(s: TienLenState, seat: Int): Boolean {
        val top = s.top ?: return false
        if (!s.rules.fourPairsCutWithoutTurn) return false
        return ComboFinder.beating(s.hands[seat], top, s.rules, outOfTurnOnly = true).isNotEmpty()
    }

    private fun newRound(s: TienLenState, owner: Int, events: MutableList<GameEvent>): Transition<TienLenState> {
        val leader = if (s.isActive(owner)) {
            owner
        } else {
            (1..s.playerCount).map { (owner + it) % s.playerCount }.firstOrNull { s.isActive(it) }
        } ?: return Transition(finish(s), events)
        events += GameEvent.RoundStarted(PlayerId(leader))
        val closed = closeChain(s)
        return Transition(
            closed.copy(
                top = null,
                topOwner = null,
                topIsCut = false,
                passed = emptySet(),
                declined = emptySet(),
                turn = leader,
            ),
            events,
        )
    }

    private fun closeChain(s: TienLenState): TienLenState {
        val chain = s.cutChain ?: return s
        val line = SettlementLine(PlayerId(chain.victim), PlayerId(chain.cutter), chain.amount, chain.description)
        return s.copy(cutChain = null, cutLines = s.cutLines + line)
    }

    private fun finish(s: TienLenState): TienLenState = closeChain(s).copy(finished = true)

    override fun isFinished(state: TienLenState): Boolean = state.finished

    override fun settle(state: TienLenState): Settlement {
        require(state.finished) { "Ván chưa kết thúc" }
        val players = (0 until state.playerCount).map { PlayerId(it) }
        val lines = state.cutLines.toMutableList()
        val rules = state.rules
        val b = state.betUnit

        val iw = state.instantWin
        if (iw != null) {
            val unit = if (rules.scoring == TlScoring.COUNT_CARDS) {
                rules.instantWinCards * b
            } else {
                rules.rankingInstantWinHalves * b / 2
            }
            for (i in 0 until state.playerCount) {
                if (i == iw.player) continue
                lines += SettlementLine(PlayerId(i), PlayerId(iw.player), unit, "Thua tới trắng (${iw.type.viName})")
                if (rules.instantWinCountsPenalties) lines += penaltyLines(state, i, iw.player)
            }
            return Settlement.fromLines(players, lines)
        }

        when (rules.scoring) {
            TlScoring.COUNT_CARDS -> {
                val winner = state.finishOrder.first()
                for (i in 0 until state.playerCount) {
                    if (i == winner) continue
                    val analysis = Penalties.analyze(state.hands[i], rules)
                    if (i !in state.hasPlayed) {
                        lines += SettlementLine(
                            PlayerId(i), PlayerId(winner), rules.congCards * b, "Cóng: ${rules.congCards} lá",
                        )
                    } else if (analysis.normalCards.isNotEmpty()) {
                        val n = analysis.normalCards.size
                        lines += SettlementLine(PlayerId(i), PlayerId(winner), n * b, "Còn $n lá")
                    }
                    lines += penaltyLines(state, i, winner)
                }
            }
            TlScoring.RANKING -> lines += rankingLines(state)
        }
        return Settlement.fromLines(players, lines)
    }

    /** Thứ tự xếp hạng cuối ván: người về trước → người còn lại → người bị cóng. */
    fun finalRanking(state: TienLenState): List<Int> {
        val remaining = (0 until state.playerCount)
            .filter { it !in state.finishOrder && it !in state.cong }
            .sortedBy { state.hands[it].size }
        val first = state.finishOrder.firstOrNull() ?: state.turn
        val congOrdered = (1..state.playerCount).map { (first + it) % state.playerCount }.filter { it in state.cong }
        return state.finishOrder + remaining + congOrdered
    }

    private fun rankingLines(state: TienLenState): List<SettlementLine> {
        val rules = state.rules
        val b = state.betUnit
        val order = finalRanking(state)
        val payout = rules.rankingPayout(state.playerCount)
        val winner = order.first()
        val lines = mutableListOf<SettlementLine>()

        // Tiền theo hạng (bỏ qua người cóng — họ trả tiền cóng thay vào)
        val winners = ArrayDeque(order.indices.filter { payout[it] > 0 }.map { order[it] to payout[it] * b })
        val losers = order.indices.reversed()
            .filter { payout[it] < 0 && order[it] !in state.cong }
            .map { Triple(order[it], -payout[it] * b, it) }
        for ((seat, owe, pos) in losers) {
            var left = owe
            while (left > 0 && winners.isNotEmpty()) {
                val (w, want) = winners.removeFirst()
                val pay = minOf(left, want)
                lines += SettlementLine(PlayerId(seat), PlayerId(w), pay, "Về ${rankName(pos, state.playerCount)}")
                left -= pay
                if (want > pay) winners.addFirst(w to want - pay)
            }
        }
        // Cóng
        for (c in state.cong) {
            lines += SettlementLine(PlayerId(c), PlayerId(winner), rules.rankingCongHalves * b / 2, "Cóng")
            lines += penaltyLines(state, c, winner)
        }
        // Thối của người về Bét (còn bài)
        order.filter { it !in state.cong && state.hands[it].isNotEmpty() && it != winner }
            .forEach { lines += penaltyLines(state, it, winner) }
        return lines
    }

    private fun penaltyLines(state: TienLenState, from: Int, to: Int): List<SettlementLine> {
        val rules = state.rules
        return Penalties.analyze(state.hands[from], rules).items.map { item ->
            val amount = Penalties.xu(item.kind, rules, state.betUnit)
            val unit = if (rules.scoring == TlScoring.COUNT_CARDS) {
                "${Penalties.cards(item.kind, rules)} lá"
            } else {
                "${formatHalves(Penalties.halves(item.kind, rules))}B"
            }
            SettlementLine(PlayerId(from), PlayerId(to), amount, "Thối ${item.kind.viName}: $unit")
        }
    }

    /**
     * Thanh toán khi [player] thoát giữa ván: xử thua như người về cuối,
     * trả cho đối thủ còn ít bài nhất.
     */
    fun forfeit(state: TienLenState, player: PlayerId): Settlement {
        if (state.finished) return settle(state)
        val seat = player.seat
        val s = closeChain(state)
        val players = (0 until s.playerCount).map { PlayerId(it) }
        val lines = s.cutLines.toMutableList()
        val receiver = (1 until s.playerCount).map { (seat + it) % s.playerCount }
            .filter { s.isActive(it) }
            .minByOrNull { s.hands[it].size }
            ?: s.finishOrder.firstOrNull { it != seat }
            ?: ((seat + 1) % s.playerCount)
        val rules = s.rules
        val b = s.betUnit
        when (rules.scoring) {
            TlScoring.COUNT_CARDS -> {
                val analysis = Penalties.analyze(s.hands[seat], rules)
                if (seat !in s.hasPlayed) {
                    lines += SettlementLine(player, PlayerId(receiver), rules.congCards * b, "Thoát bàn (cóng)")
                } else if (analysis.normalCards.isNotEmpty()) {
                    val n = analysis.normalCards.size
                    lines += SettlementLine(player, PlayerId(receiver), n * b, "Thoát bàn: còn $n lá")
                }
            }
            TlScoring.RANKING -> {
                val last = -rules.rankingPayout(s.playerCount).last()
                lines += SettlementLine(player, PlayerId(receiver), last * b, "Thoát bàn: xử Bét")
            }
        }
        lines += penaltyLines(s, seat, receiver)
        return Settlement.fromLines(players, lines)
    }

    override fun viewOf(state: TienLenState, player: PlayerId): TienLenView {
        val me = player.seat
        val myHand = state.hands[me]
        return TienLenView(
            me = me,
            playerCount = state.playerCount,
            rules = state.rules,
            betUnit = state.betUnit,
            isFirstGame = state.isFirstGame,
            myHand = myHand,
            handCounts = state.hands.map { it.size },
            turn = state.turn,
            top = state.top,
            topOwner = state.topOwner,
            topIsCut = state.topIsCut,
            passed = state.passed,
            declined = state.declined,
            mustInclude = state.mustInclude?.takeIf { it in myHand },
            finishOrder = state.finishOrder,
            hasPlayed = state.hasPlayed,
            cong = state.cong,
            playedCards = state.playedCards,
            cutAmount = state.cutChain?.amount ?: 0L,
            cutVictim = state.cutChain?.victim,
            finished = state.finished,
        )
    }

    /**
     * Hành động tự động khi hết giờ: bỏ lượt; nếu đang đi tự do thì đánh lá nhỏ nhất
     * (lá bắt buộc nếu có).
     */
    fun timeoutAction(state: TienLenState, seat: Int): TlAction {
        if (state.top != null) return TlAction.Pass
        val card = state.mustInclude ?: state.hands[seat].minBy { it.tlValue }
        return TlAction.Play(listOf(card))
    }

    companion object {
        const val CARDS_PER_PLAYER = 13

        /**
         * Dựng lại một trạng thái đầy đủ từ [view] và bài giả định của đối thủ
         * (dùng cho mô phỏng Monte Carlo của bot — bài giả định là mẫu ngẫu nhiên, không phải bài thật).
         */
        fun determinize(view: TienLenView, hands: List<List<Card>>): TienLenState = TienLenState(
            rules = view.rules,
            playerCount = view.playerCount,
            betUnit = view.betUnit,
            seed = 0L,
            isFirstGame = view.isFirstGame,
            hands = hands.map { it.sortedTl() },
            turn = view.turn,
            top = view.top,
            topOwner = view.topOwner,
            topIsCut = view.topIsCut,
            passed = view.passed,
            declined = view.declined,
            mustInclude = view.mustInclude,
            finishOrder = view.finishOrder,
            hasPlayed = view.hasPlayed,
            cong = view.cong,
            cutChain = if (view.topIsCut && view.topOwner != null && view.cutVictim != null) {
                CutChain(view.topOwner, view.cutVictim, view.cutAmount, "")
            } else {
                null
            },
            playedCards = view.playedCards,
            finished = view.finished,
        )

        fun rankName(position: Int, playerCount: Int): String = when {
            position == 0 -> "Nhất"
            position == playerCount - 1 -> "Bét"
            position == 1 -> "Nhì"
            else -> "Ba"
        }

        private fun formatHalves(h: Int): String = if (h % 2 == 0) "${h / 2}" else "${h / 2},5"
    }
}
