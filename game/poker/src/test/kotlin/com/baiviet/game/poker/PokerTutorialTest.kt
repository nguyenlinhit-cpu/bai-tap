package com.baiviet.game.poker

import com.baiviet.game.poker.rules.PokerEvaluator
import com.baiviet.game.poker.rules.PokerHandType
import com.baiviet.game.poker.ui.PokerTutorial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kết quả lật bài trong Ván tập Poker phải khớp bộ đánh giá tay bài thật. */
class PokerTutorialTest {
    @Test
    fun showdownMatchesEvaluator() {
        val board = PokerTutorial.flop + PokerTutorial.turn + PokerTutorial.river
        val me = PokerEvaluator.evaluate(PokerTutorial.me + board)
        val minh = PokerEvaluator.evaluate(PokerTutorial.minh + board)
        val hung = PokerEvaluator.evaluate(PokerTutorial.hung + board)
        assertEquals(PokerHandType.FLUSH, me.type)
        assertEquals(PokerHandType.FLUSH, minh.type)
        assertEquals(PokerHandType.THREE_OF_A_KIND, hung.type)
        assertTrue(me > minh)
        assertTrue(minh > hung)
        // Flop: đôi K
        assertEquals(PokerHandType.ONE_PAIR, PokerEvaluator.evaluate(PokerTutorial.me + PokerTutorial.flop).type)
    }
}
