package com.baiviet.game.poker

import com.baiviet.core.cards.Deck
import com.baiviet.game.poker.rules.FastEvaluator
import com.baiviet.game.poker.rules.PokerEvaluator
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/** Bộ đánh giá nhanh phải cho cùng thứ tự với bộ đánh giá đầy đủ. */
class FastEvaluatorTest {
    @Test
    fun `fast evaluator agrees with full evaluator on random 7 card hands`() {
        val rnd = Random(3)
        repeat(20_000) {
            val deck = Deck.FULL_DECK.shuffled(rnd)
            val a = deck.subList(0, 7)
            val b = deck.subList(2, 9)
            val slow = Integer.signum(PokerEvaluator.evaluate(a).compareTo(PokerEvaluator.evaluate(b)))
            val fast = Integer.signum(FastEvaluator.score(a).compareTo(FastEvaluator.score(b)))
            assertEquals("$a vs $b", slow, fast)
        }
    }
}
