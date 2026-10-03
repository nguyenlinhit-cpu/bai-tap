package com.baiviet.game.tienlen

import com.baiviet.game.tienlen.rules.BeatRules
import com.baiviet.game.tienlen.rules.Combo
import com.baiviet.game.tienlen.rules.ComboFinder
import com.baiviet.game.tienlen.rules.ComboType
import com.baiviet.game.tienlen.rules.TienLenRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComboTest {

    private val rules = TienLenRules()

    private fun combo(codes: String): Combo = requireNotNull(Combo.classify(cards(codes))) { "Không hợp lệ: $codes" }

    @Test
    fun `straight with ace at the end is valid`() {
        assertEquals(ComboType.STRAIGHT, combo("Qs Kh Ad").type)
        assertEquals(ComboType.STRAIGHT, combo("3s 4h 5d 6c 7s 8s 9s 10s Js Qs Ks As").type)
    }

    @Test
    fun `straight containing two is invalid`() {
        assertNull(Combo.classify(cards("Ks As 2h")))
        assertNull(Combo.classify(cards("As 2h 3d")))
        assertNull(Combo.classify(cards("2s 3h 4d")))
    }

    @Test
    fun `non consecutive or short straight invalid`() {
        assertNull(Combo.classify(cards("3s 4h 6d")))
        assertNull(Combo.classify(cards("3s 4h")))
    }

    @Test
    fun `pairs triples quads classified`() {
        assertEquals(ComboType.PAIR, combo("9h 9s").type)
        assertEquals(ComboType.TRIPLE, combo("9h 9s 9c").type)
        assertEquals(ComboType.QUAD, combo("9h 9s 9c 9d").type)
        assertNull(Combo.classify(cards("9h 10s")))
    }

    @Test
    fun `pair sequence needs three consecutive pairs without two`() {
        assertEquals(ComboType.PAIR_SEQUENCE, combo("5s 5h 6s 6h 7c 7d").type)
        assertEquals(4, combo("5s 5h 6s 6h 7c 7d 8s 8h").pairCount)
        assertNull(Combo.classify(cards("5s 5h 6s 6h")))
        assertNull(Combo.classify(cards("Ks Kh As Ah 2s 2h")))
        assertNull(Combo.classify(cards("5s 5h 6s 6h 8c 8d")))
    }

    @Test
    fun `pair compares by highest suit`() {
        assertTrue(BeatRules.canBeat(combo("9h 9s"), combo("9d 9c"), rules))
        assertFalse(BeatRules.canBeat(combo("9d 9c"), combo("9h 9s"), rules))
    }

    @Test
    fun `straight compares by top card suit`() {
        assertTrue(BeatRules.canBeat(combo("5s 6s 7h"), combo("5h 6h 7d"), rules))
        assertFalse(BeatRules.canBeat(combo("5s 6s 7h 8s"), combo("5h 6h 7d"), rules))
    }

    @Test
    fun `three pairs cuts a single two but not a pair of twos`() {
        val threePairs = combo("5s 5h 6s 6h 7c 7d")
        assertTrue(BeatRules.isCut(threePairs, combo("2h"), rules))
        assertFalse(BeatRules.canBeat(threePairs, combo("2h 2s"), rules))
    }

    @Test
    fun `quad cuts pair of twos and three pairs`() {
        val quad = combo("4s 4h 4c 4d")
        assertTrue(BeatRules.isCut(quad, combo("2h 2s"), rules))
        assertTrue(BeatRules.isCut(quad, combo("Js Jh Qs Qh Kc Kd"), rules))
        assertFalse(BeatRules.isCut(quad, combo("2h 2s"), rules.copy(quadCutsPairOfTwos = false)))
    }

    @Test
    fun `four pairs cuts quad and pair of twos`() {
        val fourPairs = combo("5s 5h 6s 6h 7c 7d 8s 8h")
        assertTrue(BeatRules.isCut(fourPairs, combo("As Ah Ac Ad"), rules))
        assertTrue(BeatRules.isCut(fourPairs, combo("2h 2s"), rules))
        assertTrue(BeatRules.isOutOfTurnCut(fourPairs, combo("2h"), rules))
        assertFalse(BeatRules.isOutOfTurnCut(fourPairs, combo("2h"), rules.copy(fourPairsCutWithoutTurn = false)))
    }

    @Test
    fun `nothing cuts triple twos`() {
        val tripleTwo = combo("2s 2h 2d")
        assertFalse(BeatRules.canBeat(combo("5s 5h 6s 6h 7c 7d 8s 8h"), tripleTwo, rules))
        assertFalse(BeatRules.canBeat(combo("As Ah Ac Ad"), tripleTwo, rules))
    }

    @Test
    fun `bigger three pairs cuts smaller three pairs`() {
        assertTrue(BeatRules.isCut(combo("6s 6h 7s 7h 8c 8d"), combo("5s 5h 6c 6d 7c 7d"), rules))
    }

    @Test
    fun `finder lists all straights with suit choices`() {
        val hand = cards("3s 3h 4s 5d")
        val straights = ComboFinder.all(hand).filter { it.type == ComboType.STRAIGHT }
        assertEquals(2, straights.size)
    }

    @Test
    fun `finder beating finds cuts`() {
        val hand = cards("5s 5h 6s 6h 7c 7d 9s")
        val beats = ComboFinder.beating(hand, combo("2h"), rules)
        assertNotNull(beats.firstOrNull { it.type == ComboType.PAIR_SEQUENCE })
        assertTrue(beats.none { it.type == ComboType.SINGLE })
    }
}
