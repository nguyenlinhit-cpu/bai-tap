package com.baiviet.game.xidach

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.game.xidach.rules.XiDachEvaluator
import com.baiviet.game.xidach.rules.XiDachHandStatus
import com.baiviet.game.xidach.rules.XiDachHandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XiDachEvaluatorTest {

    @Test
    fun testXiBang() {
        val hand = XiDachEvaluator.evaluate(listOf(CAS, CAH), isDealer = false)
        assertEquals(XiDachHandType.XI_BANG, hand.type)
        assertEquals(21, hand.score)
        assertEquals(XiDachHandStatus.DU_TUOI, hand.status)
    }

    @Test
    fun testXiDach() {
        val hand1 = XiDachEvaluator.evaluate(listOf(CAS, CKC), isDealer = false)
        assertEquals(XiDachHandType.XI_DACH, hand1.type)

        val hand2 = XiDachEvaluator.evaluate(listOf(CAH, C10S), isDealer = false)
        assertEquals(XiDachHandType.XI_DACH, hand2.type)
    }

    @Test
    fun testAceFlexibilityInTwoCards() {
        val hand = XiDachEvaluator.evaluate(listOf(CAS, C9C), isDealer = false)
        assertEquals(XiDachHandType.NORMAL, hand.type)
        assertEquals(20, hand.score)
        assertEquals(XiDachHandStatus.DU_TUOI, hand.status)
    }

    @Test
    fun testAceFlexibilityInThreeCards() {
        // A + 7 + 8: if A = 10 -> 25 (bust), so A = 1 -> 16 (valid)
        val hand1 = XiDachEvaluator.evaluate(listOf(CAS, C7H, C8D), isDealer = false)
        assertEquals(16, hand1.score)
        assertEquals(XiDachHandStatus.DU_TUOI, hand1.status)

        // A + 3 + 6: if A = 10 -> 19 (valid), if A = 1 -> 10, best is 19
        val hand2 = XiDachEvaluator.evaluate(listOf(CAS, C3H, C6S), isDealer = false)
        assertEquals(19, hand2.score)
        assertEquals(XiDachHandStatus.DU_TUOI, hand2.status)
    }

    @Test
    fun testNguLinh() {
        val cards = listOf(C2S, C3H, C4D, C5C, CAS)
        val hand = XiDachEvaluator.evaluate(cards, isDealer = false)
        assertEquals(XiDachHandType.NGU_LINH, hand.type)
        assertEquals(15, hand.score) // 2+3+4+5+1 = 15
        assertEquals(XiDachHandStatus.DU_TUOI, hand.status)
    }

    @Test
    fun testNguLinhVsNguLinhLowerScoreWins() {
        val hand15 = XiDachEvaluator.evaluate(listOf(C2S, C3H, C4D, C5C, CAS), isDealer = false) // 15
        val hand18 = XiDachEvaluator.evaluate(listOf(C2S, C3H, C4D, C5C, C4D.copy(suit = Suit.HEART)), isDealer = true) // 18

        val result = XiDachEvaluator.compare(player = hand15, dealer = hand18)
        assertEquals(1, result) // 15 wins against 18
    }

    @Test
    fun testBustAndTieRules() {
        val playerBust = XiDachEvaluator.evaluate(listOf(C10S, CJH, C3H), isDealer = false) // 23
        val dealerBust = XiDachEvaluator.evaluate(listOf(CQD, CKC, C4D), isDealer = true) // 24

        assertTrue(playerBust.isBust)
        assertTrue(dealerBust.isBust)

        // Mặc định cả hai quắc thì hòa
        assertEquals(0, XiDachEvaluator.compare(playerBust, dealerBust, bothBustPlayerLoses = false))
        // Luật nhà con quắc luôn thua
        assertEquals(-1, XiDachEvaluator.compare(playerBust, dealerBust, bothBustPlayerLoses = true))
    }
}
