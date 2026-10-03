package com.baiviet.game.tienlen

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.GameEvent
import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.SettlementLine
import com.baiviet.game.tienlen.engine.TienLenEngine
import com.baiviet.game.tienlen.engine.TienLenState
import com.baiviet.game.tienlen.engine.TlAction
import com.baiviet.game.tienlen.rules.Penalties
import com.baiviet.game.tienlen.rules.PenaltyKind
import com.baiviet.game.tienlen.rules.TienLenRules
import com.baiviet.game.tienlen.rules.TlScoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {

    private val engine = TienLenEngine()
    private val b = 100L

    private fun state(
        vararg hands: String,
        turn: Int = 0,
        rules: TienLenRules = TienLenRules(),
        hasPlayed: Set<Int> = hands.indices.toSet(),
    ) = TienLenState(
        rules = rules,
        playerCount = hands.size,
        betUnit = b,
        seed = 0L,
        isFirstGame = false,
        hands = hands.map { cards(it) },
        turn = turn,
        hasPlayed = hasPlayed,
    )

    private fun TienLenState.play(seat: Int, codes: String) =
        engine.apply(this, PlayerId(seat), TlAction.Play(cards(codes)))

    private fun TienLenState.pass(seat: Int) = engine.apply(this, PlayerId(seat), TlAction.Pass)

    // ───────── Người đi đầu ─────────

    @Test
    fun `first game holder of 3 spades leads and must play it`() {
        val deck = com.baiviet.core.cards.Deck.shuffled(7L).second
        val hands = deck.chunked(13)
        val s = engine.startWithHands(TienLenRules(), 4, b, hands)
        if (s.finished) return // tới trắng hiếm gặp với seed này
        val holder = hands.indexOfFirst { c("3s") in it }
        assertEquals(holder, s.turn)
        assertEquals(c("3s"), s.mustInclude)
        val legal = engine.legalActions(s, PlayerId(holder))
        assertTrue(legal.all { it is TlAction.Play && c("3s") in it.cards })
    }

    @Test(expected = IllegalActionException::class)
    fun `first move without 3 spades is rejected`() {
        val s = engine.startWithHands(
            TienLenRules(), 2, b,
            listOf(cards("3s 5h 6d 8c 9s 10h Jd Qc Ks Ah 2d 4c 4h"), cards("3h 5s 6h 8d 9c 10s Jh Qs Kc Ad 2c 4d 6c")),
        )
        s.play(0, "5h")
    }

    @Test
    fun `without 3 spades smallest card leads`() {
        val s = engine.startWithHands(
            TienLenRules(), 2, b,
            listOf(cards("4s 5h 6d 8c 9s 10h Jd Qc Ks Ah 2d 4c 7s"), cards("3h 5s 6h 8d 9c 10s Jh Qs Kc Ad 2c 4d 6c")),
        )
        assertEquals(1, s.turn)
        assertEquals(c("3h"), s.mustInclude)
    }

    @Test
    fun `later game previous winner leads freely`() {
        val s = engine.startWithHands(
            TienLenRules(), 2, b,
            listOf(cards("3s 5h 6d 8c 9s 10h Jd Qc Ks Ah 2d 4c 4h"), cards("3h 5s 6h 8d 9c 10s Jh Qs Kc Ad 2c 4d 6c")),
            previousWinner = PlayerId(1),
        )
        assertEquals(1, s.turn)
        assertNull(s.mustInclude)
    }

    // ───────── Vòng & bỏ lượt ─────────

    @Test
    fun `all others pass gives last player a free round`() {
        var s = state("5s 9h", "6s 7s", "4h 8h", turn = 0)
        s = s.play(0, "9h").state
        s = s.pass(1).state
        val t = s.pass(2)
        assertEquals(0, t.state.turn)
        assertNull(t.state.top)
        assertTrue(t.events.any { it is GameEvent.RoundStarted })
    }

    @Test
    fun `passed player cannot play again in the round`() {
        var s = state("5s 9h Js", "6s 10s", "4h 8h", turn = 0)
        s = s.play(0, "5s").state
        s = s.pass(1).state
        s = s.play(2, "8h").state
        // P1 đã bỏ → lượt nhảy về P0
        assertEquals(0, s.turn)
    }

    @Test
    fun `cannot pass when leading`() {
        val s = state("5s 9h", "6s 7s", turn = 0)
        assertFalse(engine.canPass(s, 0))
        assertTrue(engine.legalActions(s, PlayerId(0)).none { it == TlAction.Pass })
    }

    @Test
    fun `finisher holding the round passes free lead to next player`() {
        val rules = TienLenRules(scoring = TlScoring.RANKING)
        var s = state("Ac", "3s 4s", "5d 6d", turn = 0, rules = rules)
        s = s.play(0, "Ac").state
        assertFalse(s.finished)
        s = s.pass(1).state
        s = s.pass(2).state
        assertEquals(1, s.turn)
        assertNull(s.top)
    }

    // ───────── Chặt ─────────

    @Test
    fun `chained cut transfers whole amount from last victim to last cutter`() {
        var s = state("2h 9c 10c", "5s 5h 6s 6h 7c 7d Jc", "8s 8h 8c 8d Qc", turn = 0)
        s = s.play(0, "2h").state
        val t1 = s.play(1, "5s 5h 6s 6h 7c 7d")
        val cut1 = t1.events.filterIsInstance<GameEvent.Cut>().single()
        assertEquals(600L, cut1.amount)
        val t2 = t1.state.play(2, "8s 8h 8c 8d")
        val cut2 = t2.events.filterIsInstance<GameEvent.Cut>().single()
        assertEquals(PlayerId(1), cut2.victim)
        assertEquals(1500L, cut2.amount)
        s = t2.state.pass(0).state
        s = s.pass(1).state
        assertEquals(2, s.turn)
        assertEquals(1, s.cutLines.size)
        val line = s.cutLines.single()
        assertEquals(PlayerId(1), line.from)
        assertEquals(PlayerId(2), line.to)
        assertEquals(1500L, line.amount)
    }

    @Test
    fun `three pairs cannot beat pair of twos but quad can`() {
        val s = state("2h 2s 9c", "5s 5h 6s 6h 7c 7d Jc", "8s 8h 8c 8d Qc", turn = 0).play(0, "2h 2s").state
        assertTrue(engine.legalActions(s, PlayerId(1)).none { it is TlAction.Play })
        val s2 = s.pass(1).state
        assertTrue(engine.legalActions(s2, PlayerId(2)).any { it is TlAction.Play && it.cards.size == 4 })
    }

    @Test
    fun `four pairs can cut after passing`() {
        var s = state("2s 9c", "5s 5h 6s 6h 7c 7d 8s 8h Jc", "2h Qc", turn = 0)
        s = s.play(0, "2s").state
        s = s.pass(1).state
        s = s.play(2, "2h").state
        assertEquals(0, s.turn)
        s = s.pass(0).state
        assertEquals("P1 đã bỏ nhưng có 4 đôi thông → được hỏi chặt", 1, s.turn)
        val t = s.play(1, "5s 5h 6s 6h 7c 7d 8s 8h")
        val cut = t.events.filterIsInstance<GameEvent.Cut>().single()
        assertEquals(PlayerId(2), cut.victim)
        assertEquals(600L, cut.amount)
    }

    @Test
    fun `four pairs out of turn disabled by house rule`() {
        val rules = TienLenRules(fourPairsCutWithoutTurn = false)
        var s = state("2s 9c", "5s 5h 6s 6h 7c 7d 8s 8h Jc", "2h Qc", turn = 0, rules = rules)
        s = s.play(0, "2s").state
        s = s.pass(1).state
        s = s.play(2, "2h").state
        s = s.pass(0).state
        // P1 không được hỏi → vòng kết thúc, P2 đi tự do
        assertEquals(2, s.turn)
        assertNull(s.top)
    }

    @Test
    fun `declining out of turn cut is not asked again for same top`() {
        var s = state("2s 9c", "5s 5h 6s 6h 7c 7d 8s 8h Jc", "2h Qc", turn = 0)
        s = s.play(0, "2s").state
        s = s.pass(1).state
        s = s.play(2, "2h").state
        s = s.pass(0).state
        assertEquals(1, s.turn)
        s = s.pass(1).state // từ chối chặt
        assertEquals(2, s.turn)
        assertNull(s.top)
    }

    // ───────── Tính tiền ─────────

    @Test
    fun `count mode settles remaining cards cong and penalties`() {
        var s = state(
            "Ac",
            "2h 5c 6c",
            "2s 3c 4c 5d 7d 9d Jd Kd",
            turn = 0,
            hasPlayed = setOf(0, 1),
        )
        s = s.play(0, "Ac").state
        assertTrue(s.finished)
        val st = engine.settle(s)
        assertEquals(3700L, st.deltas[PlayerId(0)])
        assertEquals(-800L, st.deltas[PlayerId(1)])
        assertEquals(-2900L, st.deltas[PlayerId(2)])
        assertEquals(0L, st.deltas.values.sum())
        assertTrue(st.lines.any { it.reason.startsWith("Cóng") })
        assertTrue(st.lines.any { it.reason.contains("heo đỏ") })
    }

    @Test
    fun `stuck three pairs counts 9 instead of 6 cards`() {
        val analysis = Penalties.analyze(cards("5s 5h 6s 6h 7c 7d 9c"), TienLenRules())
        assertEquals(listOf(PenaltyKind.THREE_PAIRS), analysis.items.map { it.kind })
        assertEquals(1, analysis.normalCards.size)
    }

    @Test
    fun `stuck quad and four pairs`() {
        val analysis = Penalties.analyze(cards("9s 9h 9c 9d 3s 3h 4s 4h 5c 5d 6c 6d"), TienLenRules())
        assertEquals(setOf(PenaltyKind.QUAD, PenaltyKind.FOUR_PAIRS), analysis.items.map { it.kind }.toSet())
        assertTrue(analysis.normalCards.isEmpty())
    }

    @Test
    fun `cut lines included in final settlement`() {
        var s = state("2h 9c", "5s 5h 6s 6h 7c 7d", "Qc Kc", turn = 0)
        s = s.play(0, "2h").state
        s = s.play(1, "5s 5h 6s 6h 7c 7d").state // P1 hết bài khi chặt
        assertTrue(s.finished)
        val st = engine.settle(s)
        assertTrue(st.lines.contains(SettlementLine(PlayerId(0), PlayerId(1), 600L, "3 đôi thông chặt Heo 2♥")))
        // P0: chặt 6 lá + còn 1 lá = 7 lá; P2: 2 lá
        assertEquals(-700L, st.deltas[PlayerId(0)])
        assertEquals(-200L, st.deltas[PlayerId(2)])
        assertEquals(900L, st.deltas[PlayerId(1)])
    }

    @Test
    fun `instant win each loser pays 26 cards`() {
        val hands = listOf(
            cards("3s 4h 5d 6c 7s 8h 9d 10c Js Qh Kd Ac 9s"),
            cards("3h 5s 6h 8d 9c 10s Jh Qs Kc Ad 2c 4d 6c"),
        )
        val s = engine.startWithHands(TienLenRules(), 2, b, hands)
        assertTrue(s.finished)
        assertEquals(0, s.instantWin?.player)
        val st = engine.settle(s)
        assertEquals(2600L, st.deltas[PlayerId(0)])
    }

    @Test
    fun `instant win with penalties house rule`() {
        val hands = listOf(
            cards("3s 4h 5d 6c 7s 8h 9d 10c Js Qh Kd Ac 9s"),
            cards("3h 5s 6h 8d 9c 10s Jh Qs Kc Ad 2h 4d 6c"),
        )
        val s = engine.startWithHands(TienLenRules(instantWinCountsPenalties = true), 2, b, hands)
        assertEquals(3200L, engine.settle(s).deltas[PlayerId(0)])
    }

    @Test
    fun `ranking mode payouts and penalties`() {
        val rules = TienLenRules(scoring = TlScoring.RANKING)
        val s = state("Ac", "2h 5c", "Kc", "Qc 4d", turn = 2, rules = rules)
        // Thứ tự về: P2 Nhất, P0 Nhì, P3 Ba, P1 Bét (còn 2♥ 5♣)
        val end = s.copy(
            hands = listOf(emptyList(), cards("2h 5c"), emptyList(), emptyList()),
            finishOrder = listOf(2, 0, 3),
            finished = true,
        )
        val st = engine.settle(end)
        assertEquals(200L + 100L, st.deltas[PlayerId(2)]) // 2B + thối heo đỏ 1B
        assertEquals(100L, st.deltas[PlayerId(0)])
        assertEquals(-100L, st.deltas[PlayerId(3)])
        assertEquals(-300L, st.deltas[PlayerId(1)])
    }

    @Test
    fun `forbid finishing with two`() {
        val rules = TienLenRules(forbidFinishWithTwo = true)
        var s = state("5s 9h", "2h 3c", turn = 0, rules = rules)
        s = s.play(0, "5s").state
        // P1: đánh 2h sẽ còn 3c → được; nhưng nếu chỉ còn 2h thì không được về
        assertTrue(engine.legalActions(s, PlayerId(1)).any { it is TlAction.Play })
        val s2 = state("5s 9h", "2h", turn = 0, rules = rules).play(0, "5s").state
        assertTrue(engine.legalActions(s2, PlayerId(1)).none { it is TlAction.Play })
    }

    @Test
    fun `forfeit pays as last place to opponent with fewest cards`() {
        val s = state("3s 4s 5d", "6c", "7c 8c", turn = 0)
        val st = engine.forfeit(s, PlayerId(0))
        assertEquals(-300L, st.deltas[PlayerId(0)])
        assertEquals(300L, st.deltas[PlayerId(1)])
    }

    // ───────── View ─────────

    @Test
    fun `view never contains other players hidden cards`() {
        val deck = com.baiviet.core.cards.Deck.shuffled(99L).second
        val hands = deck.chunked(13)
        var s = engine.startWithHands(TienLenRules(), 4, b, hands)
        if (s.finished) return
        // đánh vài nước
        repeat(6) {
            if (s.finished) return@repeat
            val a = engine.legalActions(s, PlayerId(s.turn)).first()
            s = engine.apply(s, PlayerId(s.turn), a).state
        }
        for (p in 0 until 4) {
            val v = engine.viewOf(s, PlayerId(p))
            val visible: Set<Card> = buildSet {
                addAll(v.myHand)
                addAll(v.playedCards)
                v.top?.let { addAll(it.cards) }
                v.mustInclude?.let { add(it) }
            }
            val hidden = (0 until 4).filter { it != p }.flatMap { s.hands[it] }.toSet()
            assertTrue("Ghế $p thấy bài ẩn", visible.intersect(hidden).isEmpty())
            assertEquals(s.hands[p], v.myHand)
        }
    }
}
