package com.baiviet.game.lieng

import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.lieng.engine.LiengAction
import com.baiviet.game.lieng.engine.LiengEngine
import com.baiviet.game.lieng.engine.LiengPhase
import com.baiviet.game.lieng.rules.LiengRules
import com.baiviet.game.lieng.rules.LiengScoring
import com.baiviet.game.lieng.rules.LiengEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiengBettingTest {

    private val engine = LiengEngine()
    private val b = 100L
    private fun table(n: Int, rules: LiengRules = LiengRules()) = TableConfig(n, rules, betUnit = b)
    private val p0 = PlayerId(0)
    private val p1 = PlayerId(1)
    private val p2 = PlayerId(2)

    @Test
    fun `raise is at least B and at most the pot`() {
        val s = engine.startHand(table(3), 1L, listOf(10_000, 10_000, 10_000), 0)
        val raises = engine.legalActions(s, p0).filterIsInstance<LiengAction.Raise>().map { it.amount }
        assertEquals(b, raises.min())
        assertEquals(s.pot, raises.max()) // pot = 300
        try {
            engine.apply(s, p0, LiengAction.Raise(s.pot + 1))
            error("Phải ném lỗi")
        } catch (_: IllegalActionException) {
        }
    }

    @Test
    fun `raise count per round is capped`() {
        val rules = LiengRules(maxRaisesPerRound = 2)
        var s = engine.startHand(table(2, rules), 2L, listOf(100_000, 100_000), 0)
        s = engine.apply(s, p0, LiengAction.Raise(b)).state
        s = engine.apply(s, p1, LiengAction.Raise(b)).state
        val legal = engine.legalActions(s, p0)
        assertTrue(legal.none { it is LiengAction.Raise })
        assertTrue(LiengAction.AllIn !in legal)
        assertTrue(LiengAction.Call in legal)
    }

    @Test
    fun `short stack all-in creates side pot`() {
        // P0 chỉ có 300 xu (đã trả sàn còn 200), P1 và P2 nhiều xu
        var s = engine.startHand(table(3), 3L, listOf(300, 10_000, 10_000), 0)
        s = engine.apply(s, p0, LiengAction.AllIn).state
        assertEquals(300L, s.playerStates.getValue(p0).invested)
        s = engine.apply(s, p1, LiengAction.Raise(b)).state // theo 200 + tố 100
        s = engine.apply(s, p2, LiengAction.Call).state
        // Hai người còn lại xem hết các vòng
        while (!engine.isFinished(s)) {
            val actor = s.currentActor!!
            s = engine.apply(s, actor, if (LiengAction.Check in engine.legalActions(s, actor)) LiengAction.Check else LiengAction.Call).state
        }
        val st = engine.settle(s)
        assertEquals(0L, st.deltas.values.sum())
        // P0 không thể thắng quá 3 × 300 − 300 = 600
        assertTrue(st.deltas.getValue(p0) <= 600L)
        assertTrue(st.deltas.getValue(p0) >= -300L)
    }

    @Test
    fun `side pot goes to best eligible hand`() {
        val hands = mapOf(
            p0 to LiengEvaluator.evaluate(cards("As Ac Ad")), // sáp A, chỉ góp 100
            p1 to LiengEvaluator.evaluate(cards("Qh Kh Ah")), // liêng
            p2 to LiengEvaluator.evaluate(cards("2s 3c 9d")),
        )
        val (st, _) = LiengScoring.computeSettlement(
            players = listOf(p0, p1, p2),
            invested = mapOf(p0 to 100L, p1 to 500L, p2 to 500L),
            folded = emptySet(),
            hands = hands,
            rules = LiengRules(),
        )
        assertEquals(200L, st.deltas[p0]) // pot chính 300
        assertEquals(300L, st.deltas[p1]) // side pot 800 − 500 đã góp
        assertEquals(-500L, st.deltas[p2])
    }

    @Test
    fun `folded player stays out and game finishes after max rounds`() {
        var s = engine.startHand(table(3), 4L, listOf(10_000, 10_000, 10_000), 0)
        s = engine.apply(s, p0, LiengAction.Fold).state
        repeat(3) {
            assertFalse(engine.isFinished(s))
            s = engine.apply(s, s.currentActor!!, LiengAction.Check).state
            s = engine.apply(s, s.currentActor!!, LiengAction.Check).state
        }
        assertEquals(LiengPhase.FINISHED, s.phase)
        assertEquals(-100L, engine.settle(s).deltas[p0])
    }
}
