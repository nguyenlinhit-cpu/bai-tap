package com.baiviet.game.tienlen.bot

import com.baiviet.core.ai.Bot
import com.baiviet.core.ai.BotLevel
import com.baiviet.core.ai.MonteCarlo
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Deck
import com.baiviet.core.engine.PlayerId
import com.baiviet.game.tienlen.engine.TienLenEngine
import com.baiviet.game.tienlen.engine.TienLenState
import com.baiviet.game.tienlen.engine.TienLenView
import com.baiviet.game.tienlen.engine.TlAction
import com.baiviet.game.tienlen.rules.BeatRules
import com.baiviet.game.tienlen.rules.Combo
import com.baiviet.game.tienlen.rules.ComboFinder
import com.baiviet.game.tienlen.rules.ComboType
import com.baiviet.game.tienlen.rules.tlRank
import com.baiviet.game.tienlen.rules.tlValue
import kotlin.random.Random

/**
 * Bot Tiến lên. Chỉ dùng [TienLenView] (bài của mình + bài đã lộ), không bao giờ thấy bài người khác.
 *
 * - Dễ: đánh lá nhỏ nhất chặn được, hay bỏ lượt, ít khi chặt.
 * - Thường: tách bài tối ưu, giữ heo/hàng để chặt, chặn chắc khi đối thủ sắp về.
 * - Khó: như Thường + đếm bài (lấy mẫu bài ẩn từ các lá chưa lộ) + mô phỏng Monte Carlo ≤ 800ms.
 */
class TienLenBot(
    private val random: Random = Random.Default,
    private val timeBudgetMs: Long = 700L,
) : Bot<TienLenView, TlAction> {

    private val engine = TienLenEngine()

    override suspend fun decide(view: TienLenView, legal: List<TlAction>, level: BotLevel): TlAction {
        require(legal.isNotEmpty()) { "Không có hành động hợp lệ" }
        if (legal.size == 1) return legal.first()
        return when (level) {
            BotLevel.EASY -> easy(view, legal)
            BotLevel.NORMAL -> normal(view, legal)
            BotLevel.HARD -> hard(view, legal)
        }
    }

    // ───────────────────────── Dễ ─────────────────────────

    private fun easy(view: TienLenView, legal: List<TlAction>): TlAction {
        val plays = combos(legal)
        plays.firstOrNull { it.size == view.myHand.size }?.let { return it.toAction() }
        val top = view.top
        if (top == null) {
            val pool = plays.filter { it.size <= 2 }.ifEmpty { plays }
            return pool.sortedBy { it.top.tlValue }.take(3).random(random).toAction()
        }
        if (TlAction.Pass in legal && random.nextDouble() < 0.3) return TlAction.Pass
        plays.filter { !it.isBomb }.minByOrNull { it.top.tlValue }?.let { return it.toAction() }
        if (plays.isNotEmpty() && random.nextDouble() < 0.3) return plays.first().toAction()
        return if (TlAction.Pass in legal) TlAction.Pass else legal.first()
    }

    // ───────────────────────── Thường ─────────────────────────

    internal fun normal(view: TienLenView, legal: List<TlAction>): TlAction {
        val plays = combos(legal)
        val hand = view.myHand
        val canPass = TlAction.Pass in legal
        if (plays.isEmpty()) return TlAction.Pass

        // Về được thì về luôn
        plays.firstOrNull { it.size == hand.size }?.let { return it.toAction() }

        val groups = HandAnalyzer.decompose(hand)
        val minOpp = view.opponents.minOfOrNull { view.handCounts[it] } ?: 13
        val top = view.top ?: return chooseLead(view, groups, plays, minOpp).toAction()

        // Chặt heo / hàng khi có thể — rẻ nhất trước (3 đôi thông → tứ quý → 4 đôi thông)
        val cuts = plays.filter { BeatRules.isCut(it, top, view.rules) }
        if (cuts.isNotEmpty() && (top.hasTwo || top.isBomb)) {
            return cuts.minWith(compareBy<Combo>({ bombRank(it) }, { it.top.tlValue })).toAction()
        }
        // Đang được hỏi chặt ngoài lượt nhưng không đáng → bỏ
        if (view.me in view.passed) return TlAction.Pass

        val groupSets = groups.map { it.cards.toSet() }.toSet()
        val urgent = minOpp <= 2 || (minOpp <= 4 && top.size >= minOpp - 1 && top.size > 1)
        val clean = plays.filter { it.cards.toSet() in groupSets && !it.isBomb }.sortedBy { it.top.tlValue }
        for (c in clean) {
            if (!c.hasTwo) return c.toAction()
            if (urgent || groups.size <= 3 || top.top.tlRank >= 10) return c.toAction()
        }
        if (urgent) {
            plays.filter { !it.isBomb }.minByOrNull { it.top.tlValue }?.let { return it.toAction() }
            plays.minWithOrNull(compareBy<Combo>({ bombRank(it) }, { it.top.tlValue }))?.let { return it.toAction() }
        }
        // Tách đôi/sám để chặn rác lớn khi còn ít bộ
        if (top.type == ComboType.SINGLE && groups.size <= 4) {
            plays.filter { !it.isBomb && !it.hasTwo }.minByOrNull { it.top.tlValue }?.let { return it.toAction() }
        }
        return if (canPass) TlAction.Pass else plays.first().toAction()
    }

    private fun chooseLead(view: TienLenView, groups: List<Combo>, plays: List<Combo>, minOpp: Int): Combo {
        val legalSets = plays.map { it.cards.toSet() }.toSet()
        val candidates = groups.filter { it.cards.toSet() in legalSets }

        view.mustInclude?.let { must ->
            candidates.filter { must in it.cards }.maxByOrNull { it.size }?.let { return it }
            return plays.filter { must in it.cards && !it.isBomb }.maxByOrNull { it.size }
                ?: plays.first { must in it.cards }
        }
        // Còn 2 bộ, một bộ chắc thắng (heo/hàng) → đánh bộ chắc trước để giành quyền đi
        if (candidates.size == 2) {
            candidates.firstOrNull { it.hasTwo || it.isBomb }?.let { return it }
        }
        val nonControl = candidates.filter { !it.hasTwo && !it.isBomb }
        val pool = nonControl.ifEmpty { candidates }.ifEmpty { plays }
        if (minOpp == 1) {
            pool.filter { it.size > 1 }.minByOrNull { it.top.tlValue }?.let { return it }
            return pool.maxBy { it.top.tlValue }
        }
        if (minOpp == 2) {
            pool.filter { it.type != ComboType.PAIR }.minByOrNull { leadScore(it) }?.let { return it }
            return pool.maxBy { it.top.tlValue }
        }
        return pool.minBy { leadScore(it) }
    }

    /** Ưu tiên xả bộ có lá thấp; bộ dài hơn được ưu tiên nhẹ. */
    private fun leadScore(c: Combo): Double = c.cards.first().tlValue - c.size * 1.5

    private fun bombRank(c: Combo): Int = when {
        c.type == ComboType.PAIR_SEQUENCE && c.pairCount == 3 -> 0
        c.type == ComboType.QUAD -> 1
        else -> 2
    }

    // ───────────────────────── Khó ─────────────────────────

    private suspend fun hard(view: TienLenView, legal: List<TlAction>): TlAction {
        val base = normal(view, legal)
        val plays = combos(legal)
        plays.firstOrNull { it.size == view.myHand.size }?.let { return it.toAction() }

        val candidates = LinkedHashSet<TlAction>()
        candidates += base
        if (TlAction.Pass in legal) candidates += TlAction.Pass
        val groupSets = HandAnalyzer.decompose(view.myHand).map { it.cards.toSet() }.toSet()
        plays.filter { it.cards.toSet() in groupSets }.sortedBy { it.top.tlValue }.take(3)
            .forEach { candidates += it.toAction() }
        plays.sortedBy { it.top.tlValue }.take(2).forEach { candidates += it.toAction() }
        if (candidates.size == 1) return base

        val unseen = unseenCards(view)
        return MonteCarlo.best(candidates.toList().take(6), random, timeBudgetMs, maxIterations = 1_200) { action, rnd ->
            simulate(view, unseen, action, rnd)
        }
    }

    /** Đếm bài: các lá chưa lộ và không nằm trên tay mình. */
    private fun unseenCards(view: TienLenView): List<Card> {
        val known = (view.myHand + view.playedCards).toSet()
        return Deck.FULL_DECK.filter { it !in known }
    }

    private fun simulate(view: TienLenView, unseen: List<Card>, action: TlAction, rnd: Random): Double {
        val others = (0 until view.playerCount).filter { it != view.me }
        val sampled = MonteCarlo.deal(unseen, others.map { view.handCounts[it] }, rnd)
        val hands = MutableList(view.playerCount) { emptyList<Card>() }
        hands[view.me] = view.myHand
        others.forEachIndexed { i, seat -> hands[seat] = sampled[i] }

        var s = TienLenEngine.determinize(view, hands)
        s = engine.apply(s, PlayerId(view.me), action).state
        var steps = 0
        while (!s.finished && steps < MAX_PLAYOUT_STEPS) {
            val seat = s.turn
            s = engine.apply(s, PlayerId(seat), fastPolicy(s, seat, rnd)).state
            steps++
        }
        if (!s.finished) return 0.0
        return engine.settle(s).deltas[PlayerId(view.me)]!!.toDouble() / view.betUnit
    }

    /** Chính sách nhanh dùng trong mô phỏng (đủ tốt, rẻ). */
    private fun fastPolicy(s: TienLenState, seat: Int, rnd: Random): TlAction {
        val hand = s.hands[seat]
        val top = s.top
        if (top == null) {
            engine.checkPlay(s, seat, hand)?.let { return TlAction.Play(it.cards) }
            val first = s.mustInclude ?: hand.first()
            val same = hand.filter { it.rank == first.rank }.take(3)
            val try1 = if (s.mustInclude != null) listOf(first) + same.filter { it != first }.take(1) else same
            engine.checkPlay(s, seat, try1)?.let { return TlAction.Play(it.cards) }
            engine.checkPlay(s, seat, listOf(first))?.let { return TlAction.Play(it.cards) }
            return engine.legalActions(s, PlayerId(seat)).first()
        }
        val beats = ComboFinder.beating(hand, top, s.rules, outOfTurnOnly = seat in s.passed)
            .filter { engine.checkPlay(s, seat, it.cards) != null }
        if (beats.isEmpty()) return TlAction.Pass
        beats.firstOrNull { it.size == hand.size }?.let { return TlAction.Play(it.cards) }
        if (top.hasTwo) beats.firstOrNull { it.isBomb }?.let { return TlAction.Play(it.cards) }
        val minOpp = (0 until s.playerCount).filter { it != seat && s.isActive(it) }.minOfOrNull { s.hands[it].size } ?: 13
        val normalBeats = beats.filter { !it.isBomb && !it.hasTwo }
        if (normalBeats.isNotEmpty() && (rnd.nextDouble() < 0.85 || minOpp <= 2)) {
            return TlAction.Play(normalBeats.minBy { it.top.tlValue }.cards)
        }
        if (minOpp <= 2 || rnd.nextDouble() < 0.25) return TlAction.Play(beats.minBy { it.top.tlValue }.cards)
        return TlAction.Pass
    }

    // ───────────────────────── Tiện ích ─────────────────────────

    private fun combos(legal: List<TlAction>): List<Combo> =
        legal.filterIsInstance<TlAction.Play>().mapNotNull { Combo.classify(it.cards) }

    private fun Combo.toAction(): TlAction = TlAction.Play(cards)

    companion object {
        private const val MAX_PLAYOUT_STEPS = 400
    }
}
