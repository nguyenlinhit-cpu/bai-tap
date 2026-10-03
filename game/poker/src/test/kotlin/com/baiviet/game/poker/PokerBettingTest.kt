package com.baiviet.game.poker

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.poker.engine.PokerAction
import com.baiviet.game.poker.engine.PokerEngine
import com.baiviet.game.poker.rules.PokerEvaluator
import com.baiviet.game.poker.rules.PokerRules
import com.baiviet.game.poker.rules.PokerScoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokerBettingTest {

    private val engine = PokerEngine()
    private val bb = 100L
    private fun table(n: Int) = TableConfig(n, PokerRules(), betUnit = bb)
    private fun p(i: Int) = PlayerId(i)
    private fun c(r: Rank, s: Suit) = Card(r, s)

    @Test
    fun `dealer button rotates blinds`() {
        val s = engine.startHand(table(4), 1L, List(4) { 10_000L }, dealerIndex = 2)
        assertEquals(50L, s.playerStates.getValue(p(3)).currentStreetBet) // SB
        assertEquals(100L, s.playerStates.getValue(p(0)).currentStreetBet) // BB
        assertEquals(p(1), s.currentActor) // UTG
    }

    @Test
    fun `heads up dealer is small blind and acts first preflop`() {
        val s = engine.startHand(table(2), 1L, listOf(10_000L, 10_000L), dealerIndex = 1)
        assertEquals(50L, s.playerStates.getValue(p(1)).currentStreetBet)
        assertEquals(p(1), s.currentActor)
    }

    @Test(expected = IllegalActionException::class)
    fun `raise below minimum is rejected`() {
        val s = engine.startHand(table(3), 1L, List(3) { 10_000L }, dealerIndex = 0)
        engine.apply(s, s.currentActor!!, PokerAction.Raise(150L))
    }

    @Test
    fun `incomplete all-in raise does not reopen raising`() {
        // Dealer 0, SB 1, BB 2; P0 (UTG) gọi 100, P1 SB gọi, P2 BB tố lên 400; P0 theo; P1 all-in 450 (thiếu mức raise 300)
        var s = engine.startHand(table(3), 5L, listOf(10_000L, 450L, 10_000L), dealerIndex = 0)
        s = engine.apply(s, p(0), PokerAction.Call).state
        s = engine.apply(s, p(1), PokerAction.Call).state
        s = engine.apply(s, p(2), PokerAction.Raise(400L)).state
        s = engine.apply(s, p(0), PokerAction.Call).state
        s = engine.apply(s, p(1), PokerAction.AllIn).state
        assertEquals(450L, s.currentStreetHighBet)
        // P2 và P0 đã hành động → chỉ được theo hoặc úp
        val legal = engine.legalActions(s, s.currentActor!!)
        assertTrue(legal.none { it is PokerAction.Raise })
        assertTrue(PokerAction.Call in legal)
    }

    @Test
    fun `three tier side pots`() {
        val board = listOf(c(Rank.TWO, Suit.CLUB), c(Rank.SEVEN, Suit.DIAMOND), c(Rank.NINE, Suit.HEART), c(Rank.JACK, Suit.SPADE), c(Rank.FOUR, Suit.CLUB))
        val hands = mapOf(
            p(0) to PokerEvaluator.evaluate(listOf(c(Rank.ACE, Suit.SPADE), c(Rank.ACE, Suit.HEART)) + board), // đôi A, góp 100
            p(1) to PokerEvaluator.evaluate(listOf(c(Rank.KING, Suit.SPADE), c(Rank.KING, Suit.HEART)) + board), // đôi K, góp 300
            p(2) to PokerEvaluator.evaluate(listOf(c(Rank.QUEEN, Suit.SPADE), c(Rank.QUEEN, Suit.HEART)) + board), // đôi Q, góp 600
            p(3) to PokerEvaluator.evaluate(listOf(c(Rank.THREE, Suit.SPADE), c(Rank.FIVE, Suit.HEART)) + board), // góp 600
        )
        val (st, _) = PokerScoring.computeSettlement(
            players = (0..3).map { p(it) },
            invested = mapOf(p(0) to 100L, p(1) to 300L, p(2) to 600L, p(3) to 600L),
            folded = emptySet(),
            hands = hands,
        )
        assertEquals(300L, st.deltas[p(0)]) // pot chính 400
        assertEquals(300L, st.deltas[p(1)]) // tầng 2: 600
        assertEquals(0L, st.deltas[p(2)]) // tầng 3: 600 → P2 lấy lại vốn
        assertEquals(-600L, st.deltas[p(3)])
    }

    @Test
    fun `stacks carry into next hand`() {
        val s = engine.startHand(table(2), 9L, listOf(500L, 20_000L), dealerIndex = 0)
        assertEquals(450L, s.playerStates.getValue(p(0)).stack)
    }
}
