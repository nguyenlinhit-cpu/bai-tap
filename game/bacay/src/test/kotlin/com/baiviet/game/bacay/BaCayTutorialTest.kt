package com.baiviet.game.bacay

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.game.bacay.rules.BaCayEvaluator
import com.baiviet.game.bacay.rules.BaCayHandType
import com.baiviet.game.bacay.rules.BaCayRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Các con số trong Ván tập Ba cây phải khớp bộ luật thật. */
class BaCayTutorialTest {
    private fun c(r: Rank, s: Suit) = Card(r, s)

    @Test
    fun scriptedHandsMatchRules() {
        val me = BaCayEvaluator.evaluate(listOf(c(Rank.FOUR, Suit.HEART), c(Rank.TWO, Suit.CLUB), c(Rank.THREE, Suit.DIAMOND)))
        val dealer = BaCayEvaluator.evaluate(listOf(c(Rank.SEVEN, Suit.SPADE), c(Rank.KING, Suit.HEART), c(Rank.TWO, Suit.SPADE)))
        assertEquals(9, me.points)
        assertEquals(9, dealer.points)
        assertTrue("Bằng nút: 3♦ (Rô) phải thắng K♥ (Cơ)", BaCayEvaluator.compare(me, dealer, BaCayRules.DEFAULT) > 0)
        val baTien = BaCayEvaluator.evaluate(listOf(c(Rank.JACK, Suit.SPADE), c(Rank.QUEEN, Suit.HEART), c(Rank.KING, Suit.CLUB)))
        val sap = BaCayEvaluator.evaluate(listOf(c(Rank.FIVE, Suit.SPADE), c(Rank.FIVE, Suit.HEART), c(Rank.FIVE, Suit.DIAMOND)))
        assertEquals(BaCayHandType.BA_TIEN, baTien.type)
        assertTrue(BaCayEvaluator.compare(baTien, me, BaCayRules.DEFAULT) > 0)
        assertTrue(BaCayEvaluator.compare(baTien, sap, BaCayRules.DEFAULT) > 0)
    }
}
