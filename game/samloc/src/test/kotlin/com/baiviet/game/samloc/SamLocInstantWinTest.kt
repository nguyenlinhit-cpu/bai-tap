package com.baiviet.game.samloc

import com.baiviet.game.samloc.rules.SamLocInstantWin
import com.baiviet.game.samloc.rules.SamLocInstantWinType
import com.baiviet.game.samloc.rules.SamLocRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SamLocInstantWinTest {
    private val rules = SamLocRules.DEFAULT

    @Test
    fun `detects four twos instant win`() {
        val hand = cards("2s 2c 2d 2h 3s 4c 5d 6h 7s 8c")
        assertEquals(SamLocInstantWinType.FOUR_TWOS, SamLocInstantWin.detect(hand, rules))
    }

    @Test
    fun `detects dragon 10 card straight instant win`() {
        // 3..Q
        val dragon1 = cards("3s 4c 5d 6h 7s 8c 9d 10h Js Qc")
        assertEquals(SamLocInstantWinType.DRAGON, SamLocInstantWin.detect(dragon1, rules))

        // A-2-3..10
        val dragon2 = cards("As 2c 3d 4h 5s 6c 7d 8h 9s 10c")
        assertEquals(SamLocInstantWinType.DRAGON, SamLocInstantWin.detect(dragon2, rules))

        // 5..A
        val dragon3 = cards("5s 6c 7d 8h 9s 10c Jd Qh Ks Ac")
        assertEquals(SamLocInstantWinType.DRAGON, SamLocInstantWin.detect(dragon3, rules))
    }

    @Test
    fun `detects five pairs instant win`() {
        val hand = cards("3s 3c 4s 4c 7s 7d Js Jh 2s 2c")
        assertEquals(SamLocInstantWinType.FIVE_PAIRS, SamLocInstantWin.detect(hand, rules))
    }

    @Test
    fun `detects same color 10 cards instant win`() {
        // 10 lá toàn đỏ (cơ, rô), không phải sảnh rồng hay 5 đôi
        val redHand = cards("3d 3h 4d 5h 7d 8h 9d 10h Jd Kd")
        assertEquals(SamLocInstantWinType.SAME_COLOR, SamLocInstantWin.detect(redHand, rules))

        // 10 lá toàn đen (bích, chuồn), không phải sảnh rồng hay 5 đôi
        val blackHand = cards("3s 3c 4s 5c 7s 8c 9s 10c Js Ks")
        assertEquals(SamLocInstantWinType.SAME_COLOR, SamLocInstantWin.detect(blackHand, rules))
    }

    @Test
    fun `three triples instant win only when house rule enabled`() {
        val hand = cards("3s 3c 3d 5s 5c 5d 9s 9c 9d 2h")

        // Mặc định: tắt
        assertNull("Mặc định 3 sám không tới trắng", SamLocInstantWin.detect(hand, rules))

        // Bật luật nhà
        val customRules = rules.copy(threeTriplesInstantWin = true)
        assertEquals(SamLocInstantWinType.THREE_TRIPLES, SamLocInstantWin.detect(hand, customRules))
    }

    @Test
    fun `resolves priority between instant win types`() {
        // Tứ quý heo > 5 đôi
        val fourTwos = cards("2s 2c 2d 2h 3s 4c 5d 6h 7s 8c")
        val fivePairs = cards("3s 3c 4s 4c 7s 7d Js Jh Ks Kc")

        val candidates =
            mapOf(
                0 to SamLocInstantWinType.FIVE_PAIRS,
                1 to SamLocInstantWinType.FOUR_TWOS,
            )
        val winner = SamLocInstantWin.resolve(candidates, listOf(fivePairs, fourTwos))
        assertEquals(1, winner)
    }
}
