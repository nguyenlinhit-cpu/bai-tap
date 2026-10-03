package com.baiviet.game.phom

import com.baiviet.game.phom.rules.PhomMeld
import com.baiviet.game.phom.rules.PhomMeldFinder
import com.baiviet.game.phom.rules.PhomMeldType
import com.baiviet.game.phom.rules.phomPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhomMeldTest {

    @Test
    fun `cardPoints assigns correct points to ranks`() {
        assertEquals(1, c("As").phomPoint)
        assertEquals(2, c("2h").phomPoint)
        assertEquals(10, c("10d").phomPoint)
        assertEquals(11, c("Jc").phomPoint)
        assertEquals(12, c("Qd").phomPoint)
        assertEquals(13, c("Kh").phomPoint)
    }

    @Test
    fun `group meld requires 3 or 4 cards of same rank`() {
        val threeKings = PhomMeld.classify(cards("Ks Kd Kh"))
        assertNotNull(threeKings)
        assertEquals(PhomMeldType.GROUP, threeKings?.type)

        val fourAces = PhomMeld.classify(cards("As Ac Ad Ah"))
        assertNotNull(fourAces)
        assertEquals(PhomMeldType.GROUP, fourAces?.type)

        val twoTens = PhomMeld.classify(cards("10s 10h"))
        assertNull(twoTens)

        val differentRanks = PhomMeld.classify(cards("Ks Kd 10h"))
        assertNull(differentRanks)
    }

    @Test
    fun `run meld requires 3 or more consecutive cards of same suit`() {
        val a23Spade = PhomMeld.classify(cards("As 2s 3s"))
        assertNotNull(a23Spade)
        assertEquals(PhomMeldType.RUN, a23Spade?.type)

        val run78910Heart = PhomMeld.classify(cards("7h 8h 9h 10h"))
        assertNotNull(run78910Heart)
        assertEquals(PhomMeldType.RUN, run78910Heart?.type)

        val run10JQK = PhomMeld.classify(cards("10c Jc Qc Kc"))
        assertNotNull(run10JQK)
        assertEquals(PhomMeldType.RUN, run10JQK?.type)
    }

    @Test
    fun `Q-K-A and K-A-2 are NOT valid run melds in Phom`() {
        // Q-K-A is invalid because A is only index 1 (lowest)
        assertNull(PhomMeld.classify(cards("Qh Kh Ah")))
        assertNull(PhomMeld.classify(cards("Kh Ah 2h")))
        assertNull(PhomMeld.classify(cards("Jh Qh Kh Ah")))
    }

    @Test
    fun `run meld cannot have mixed suits`() {
        assertNull(PhomMeld.classify(cards("4s 5h 6s")))
    }

    @Test
    fun `canLayOff and withLayOff work for group meld`() {
        val meld = PhomMeld.classify(cards("7s 7c 7d"))!!
        assertTrue(meld.canLayOff(c("7h")))
        assertFalse(meld.canLayOff(c("8s")))

        val updated = meld.withLayOff(c("7h"))
        assertEquals(4, updated.cards.size)
        // A 4-card group cannot accept any more cards
        assertFalse(updated.canLayOff(c("7s")))
    }

    @Test
    fun `canLayOff and withLayOff work for run meld`() {
        val meld = PhomMeld.classify(cards("5d 6d 7d"))!!
        assertTrue(meld.canLayOff(c("4d")))
        assertTrue(meld.canLayOff(c("8d")))
        assertFalse(meld.canLayOff(c("3d")))
        assertFalse(meld.canLayOff(c("8s")))

        val extendedFront = meld.withLayOff(c("4d"))
        assertEquals(4, extendedFront.cards.size)
        assertEquals(c("4d"), extendedFront.cards.first())

        val extendedBack = meld.withLayOff(c("8d"))
        assertEquals(4, extendedBack.cards.size)
        assertEquals(c("8d"), extendedBack.cards.last())
    }

    @Test
    fun `findCas detects pairs, consecutive suits and gap-1 suits`() {
        val hand = cards("5s 5h 8c 9c Jd Kd")
        val cas = PhomMeldFinder.findAllCa(hand)

        // Pair 5s-5h
        assertTrue(cas.any { (it.cards.first == c("5s") && it.cards.second == c("5h")) || (it.cards.first == c("5h") && it.cards.second == c("5s")) })
        // Consecutive 8c-9c
        assertTrue(cas.any { (it.cards.first == c("8c") && it.cards.second == c("9c")) || (it.cards.first == c("9c") && it.cards.second == c("8c")) })
        // Gap-1 Jd-Kd (Qd missing)
        assertTrue(cas.any { (it.cards.first == c("Jd") && it.cards.second == c("Kd")) || (it.cards.first == c("Kd") && it.cards.second == c("Jd")) })
    }

    @Test
    fun `K-A is NOT a ca doc in Phom`() {
        val hand = cards("Ks As 3h 6d 8c 10s")
        val cas = PhomMeldFinder.findAllCa(hand)
        assertFalse(cas.any { (it.cards.first == c("Ks") && it.cards.second == c("As")) || (it.cards.first == c("As") && it.cards.second == c("Ks")) })
    }

    @Test
    fun `isUKhan correctly identifies hands with no cas or melds`() {
        // Hand completely scattered: no pairs, no consecutive or gap-1 cards in same suit
        val uKhanHand = cards("As 5s 9s Kh 4h 8h 2d 6d 10c")
        assertTrue(PhomMeldFinder.isUKhan(uKhanHand))

        // Hand with a pair is not U Khan
        val handWithPair = cards("As Ah 4h 7d 10c Kc 3s 6h 9d")
        assertFalse(PhomMeldFinder.isUKhan(handWithPair))

        // Hand with a consecutive pair in same suit is not U Khan
        val handWithCa = cards("As 2s 5h 7d 10c Kc 3s 6h 9d")
        assertFalse(PhomMeldFinder.isUKhan(handWithCa))
    }

    @Test
    fun `findBestPartition minimizes trash points`() {
        // Hand: 3x 8s, run 4c-5c-6c, and trash cards: 2h, As, Kd
        val hand = cards("8s 8c 8d 4c 5c 6c 2h As Kd")
        val partition = PhomMeldFinder.findBestPartition(hand, emptySet())

        assertEquals(2, partition.melds.size)
        assertEquals(3, partition.trash.size)
        // Trash points: 2 + 1 + 13 = 16
        assertEquals(16, partition.trashPoints)
        assertFalse(PhomMeldFinder.checkU(hand, emptySet()))
    }

    @Test
    fun `checkU detects U`() {
        // Hand of 10 cards: 3 melds + 1 trash card (after draw or deal)
        val hand10 = cards("8s 8c 8d 4c 5c 6c Jd Qd Kd 2h")
        assertTrue(PhomMeldFinder.checkU(hand10, emptySet()))

        // Hand of 9 cards: 3 melds + 0 trash
        val hand9 = cards("8s 8c 8d 4c 5c 6c Jd Qd Kd")
        assertTrue(PhomMeldFinder.checkU(hand9, emptySet()))
    }
}
