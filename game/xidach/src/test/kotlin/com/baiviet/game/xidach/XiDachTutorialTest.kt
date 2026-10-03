package com.baiviet.game.xidach

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.game.xidach.rules.XiDachEvaluator
import com.baiviet.game.xidach.rules.XiDachHandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Các con số trong Ván tập Xì dách phải khớp bộ luật thật. */
class XiDachTutorialTest {
    private fun c(r: Rank, s: Suit) = Card(r, s)
    private fun ev(vararg cards: Card, dealer: Boolean = false) = XiDachEvaluator.evaluate(cards.toList(), dealer)

    @Test
    fun scriptedHandsMatchRules() {
        val ten = c(Rank.TEN, Suit.CLUB)
        val four = c(Rank.FOUR, Suit.DIAMOND)
        val ace = c(Rank.ACE, Suit.HEART)
        assertEquals(14, ev(ten, four).score)
        assertEquals(15, ev(ten, four, ace).score) // A tính 1 vì 24 sẽ quắc
        assertEquals(20, ev(ten, four, ace, c(Rank.FIVE, Suit.SPADE)).score)
        val nguLinh = ev(c(Rank.TWO, Suit.CLUB), c(Rank.THREE, Suit.HEART), c(Rank.FOUR, Suit.SPADE), c(Rank.FIVE, Suit.DIAMOND), c(Rank.ACE, Suit.CLUB))
        assertEquals(XiDachHandType.NGU_LINH, nguLinh.type)
        val dealer = ev(c(Rank.KING, Suit.SPADE), c(Rank.FIVE, Suit.HEART), c(Rank.THREE, Suit.CLUB), dealer = true)
        assertEquals(18, dealer.score)
        val me = ev(ten, four, ace, c(Rank.FIVE, Suit.SPADE))
        assertTrue(XiDachEvaluator.compare(me, dealer) > 0)
        assertEquals(15, ev(c(Rank.NINE, Suit.SPADE), c(Rank.SIX, Suit.HEART), dealer = true).score)
        assertEquals(23, ev(c(Rank.SIX, Suit.DIAMOND), c(Rank.NINE, Suit.CLUB), c(Rank.EIGHT, Suit.HEART)).score)
        assertEquals(XiDachHandType.XI_BANG, ev(c(Rank.ACE, Suit.SPADE), c(Rank.ACE, Suit.DIAMOND)).type)
        assertEquals(XiDachHandType.XI_DACH, ev(c(Rank.ACE, Suit.HEART), c(Rank.KING, Suit.CLUB)).type)
    }
}
