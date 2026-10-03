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

class BaCayEvaluatorTest {

    private val defaultRules = BaCayRules()

    @Test
    fun testPointCalculation() {
        // 3 + 6 + 10 = 19 -> 9 nút
        val hand9 = BaCayEvaluator.evaluate(listOf(C3H, C6S, C10S))
        assertEquals(BaCayHandType.POINTS, hand9.type)
        assertEquals(9, hand9.points)

        // 2 + 8 + K = 10 -> 0 nút (bù)
        val hand0 = BaCayEvaluator.evaluate(listOf(C2S, C8D, CKC))
        assertEquals(BaCayHandType.POINTS, hand0.type)
        assertEquals(0, hand0.points)
    }

    @Test
    fun testBaTien() {
        val hand = BaCayEvaluator.evaluate(listOf(CJH, CQD, CKC))
        assertEquals(BaCayHandType.BA_TIEN, hand.type)

        // Ba Tiên lớn hơn 9 nút
        val hand9 = BaCayEvaluator.evaluate(listOf(C3H, C6S, C10S))
        assertTrue(BaCayEvaluator.compare(hand, hand9, defaultRules) > 0)
    }

    @Test
    fun testSapAndRulesOption() {
        val sapNines = BaCayEvaluator.evaluate(listOf(C9C, C9C.copy(suit = Suit.HEART), C9C.copy(suit = Suit.DIAMOND))) // Sáp 9
        val baTien = BaCayEvaluator.evaluate(listOf(CJH, CQD, CKS)) // Ba Tiên (J, Q, K)

        // Mặc định: Ba Tiên > Sáp
        assertTrue(BaCayEvaluator.compare(baTien, sapNines, defaultRules) > 0)

        // Luật nhà: Sáp > Ba Tiên
        val sapBeatsRules = defaultRules.copy(sapBeatsBaTien = true)
        val sapEvaluated = BaCayEvaluator.evaluate(listOf(C9C, C9C.copy(suit = Suit.HEART), C9C.copy(suit = Suit.DIAMOND)), sapBeatsBaTien = true)
        val baTienEvaluated = BaCayEvaluator.evaluate(listOf(CJH, CQD, CKS), sapBeatsBaTien = true)
        assertTrue(BaCayEvaluator.compare(sapEvaluated, baTienEvaluated, sapBeatsRules) > 0)
    }

    @Test
    fun testTieBreakByStrongestCard() {
        // Cả hai đều 9 nút:
        // Hand A có CAD (Át Rô)
        val handA = BaCayEvaluator.evaluate(listOf(CAD, C3H, C5C)) // 1 + 3 + 5 = 9 nút, có ♦A
        // Hand B có CAH (Át Cơ)
        val handB = BaCayEvaluator.evaluate(listOf(CAH, C4D, C4D.copy(suit = Suit.SPADE))) // 1 + 4 + 4 = 9 nút, có ♥A

        // ♦ Rô > ♥ Cơ nên Hand A thắng!
        assertEquals(1, BaCayEvaluator.compare(handA, handB, defaultRules))

        // Nếu bật tieIsPush -> hòa
        val pushRules = defaultRules.copy(tieIsPush = true)
        assertEquals(0, BaCayEvaluator.compare(handA, handB, pushRules))
    }
}
