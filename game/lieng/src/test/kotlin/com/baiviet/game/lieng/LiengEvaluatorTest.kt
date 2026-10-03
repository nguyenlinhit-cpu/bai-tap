package com.baiviet.game.lieng

import com.baiviet.game.lieng.rules.LiengEvaluator
import com.baiviet.game.lieng.rules.LiengHandType
import com.baiviet.game.lieng.rules.LiengRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiengEvaluatorTest {

    private val defaultRules = LiengRules()

    @Test
    fun testSap() {
        val handAces = LiengEvaluator.evaluate(listOf(CAS, CAH, CAD))
        assertEquals(LiengHandType.SAP, handAces.type)

        val handTwos = LiengEvaluator.evaluate(listOf(C2S, C2S.copy(suit = com.baiviet.core.cards.Suit.HEART), C2S.copy(suit = com.baiviet.core.cards.Suit.DIAMOND)))
        assertEquals(LiengHandType.SAP, handTwos.type)

        // Sáp A lớn hơn Sáp 2
        assertTrue(LiengEvaluator.compare(handAces, handTwos, defaultRules) > 0)
    }

    @Test
    fun testLieng() {
        // Q-K-A (Liêng lớn nhất)
        val qka = LiengEvaluator.evaluate(listOf(CQD, CKC, CAS))
        assertEquals(LiengHandType.LIENG, qka.type)

        // A-2-3 (Liêng nhỏ nhất)
        val a23 = LiengEvaluator.evaluate(listOf(CAS, C2S, C3H))
        assertEquals(LiengHandType.LIENG, a23.type)

        assertTrue(LiengEvaluator.compare(qka, a23, defaultRules) > 0)

        // Sáp lớn hơn Liêng
        val sap = LiengEvaluator.evaluate(listOf(CAS, CAH, CAD))
        assertTrue(LiengEvaluator.compare(sap, qka, defaultRules) > 0)
    }

    @Test
    fun testAnhAndDiem() {
        // 3 lá Tây (J, Q, K nhưng không phải Liêng hay Sáp)
        val anh = LiengEvaluator.evaluate(listOf(CJH, CQD, CKC.copy(rank = com.baiviet.core.cards.Rank.JACK))) // J-J-Q
        assertEquals(LiengHandType.ANH, anh.type)

        // 9 điểm
        val diem9 = LiengEvaluator.evaluate(listOf(C2S, C3H, C4D)) // 2 + 3 + 4 = 9 điểm (nhưng liên tiếp là Liêng!)
        assertEquals(LiengHandType.LIENG, diem9.type)

        val diem8 = LiengEvaluator.evaluate(listOf(C2S, C6S, C10S)) // 2 + 6 + 0 = 8 điểm
        assertEquals(LiengHandType.DIEM, diem8.type)
        assertEquals(8, diem8.points)

        // Ảnh lớn hơn Điểm
        assertTrue(LiengEvaluator.compare(anh, diem8, defaultRules) > 0)
    }

    @Test
    fun testTieBreakBySuit() {
        // Cùng 8 điểm: Hand A có ♦8, Hand B có ♥8
        val handA = LiengEvaluator.evaluate(listOf(C8D, C10S, C10S.copy(suit = com.baiviet.core.cards.Suit.CLUB))) // 8 điểm, có ♦8
        val handB = LiengEvaluator.evaluate(listOf(C8D.copy(suit = com.baiviet.core.cards.Suit.HEART), C10S, C10S.copy(suit = com.baiviet.core.cards.Suit.SPADE))) // 8 điểm, có ♥8

        // ♦ Rô > ♥ Cơ theo luật mặc định
        assertEquals(1, LiengEvaluator.compare(handA, handB, defaultRules))

        // Nếu bật Cơ trên Rô:
        val coOverRo = defaultRules.copy(suitOrderCoOverRo = true)
        assertEquals(-1, LiengEvaluator.compare(handA, handB, coOverRo))
    }
}
