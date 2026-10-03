package com.baiviet.game.phom

import com.baiviet.game.phom.rules.PhomEatRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhomEatRulesTest {

    @Test
    fun `findPossibleEatMelds returns melds when discard forms a valid meld with hand cards`() {
        val hand = cards("8s 8c 2h 4d Jd Qd 9s")
        val discard = c("8h")
        assertTrue(PhomEatRules.findPossibleEatMelds(hand, emptySet(), discard).isNotEmpty())

        val discardRun = c("10d")
        assertTrue(PhomEatRules.findPossibleEatMelds(hand, emptySet(), discardRun).isNotEmpty())
    }

    @Test
    fun `findPossibleEatMelds returns empty when discard cannot form any meld`() {
        val hand = cards("8s 8c 2h 4d Jd Qd 9s")
        val discard = c("5c")
        assertTrue(PhomEatRules.findPossibleEatMelds(hand, emptySet(), discard).isEmpty())
    }

    @Test
    fun `findPossibleEatMelds prevents putting multiple eaten cards into a single meld`() {
        // Player already ate 8s earlier.
        // Hand contains 8s (eaten) and 8c.
        // Another player discards 8d.
        // If the player eats 8d, both 8s and 8d would be in the same group meld (8s-8c-8d),
        // which violates "max 1 eaten card per meld".
        val eaten8s = c("8s")
        val hand = cards("8s 8c 2h 4d 7c 9c")
        val discard = c("8d")

        assertTrue(PhomEatRules.findPossibleEatMelds(hand, setOf(eaten8s), discard).isEmpty())
    }

    @Test
    fun `eaten card cannot be discarded`() {
        val eatenCard = c("8s")
        val hand = cards("8s 8c 8d 2h 4d 7c 9c")
        val legals = PhomEatRules.legalDiscards(
            hand = hand,
            eatenCards = setOf(eatenCard),
        )

        assertFalse(legals.contains(eatenCard))
        assertTrue(legals.contains(c("2h")))
    }

    @Test
    fun `cards locked into meld with eaten card cannot be discarded`() {
        // Player ate 8s using 8c and 8d.
        // Hand has 8s (eaten), 8c, 8d, 2h, 4d.
        // If player discards 8c, the meld with 8s is broken, leaving eaten card 8s unmelded!
        // Therefore, 8c and 8d are locked and cannot be discarded.
        val eatenCard = c("8s")
        val hand = cards("8s 8c 8d 2h 4d")
        val legals = PhomEatRules.legalDiscards(
            hand = hand,
            eatenCards = setOf(eatenCard),
        )

        assertFalse(legals.contains(eatenCard))
        assertFalse(legals.contains(c("8c")))
        assertFalse(legals.contains(c("8d")))
        assertTrue(legals.contains(c("2h")))
        assertTrue(legals.contains(c("4d")))
    }
}
