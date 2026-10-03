package com.baiviet.game.maubinh

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.game.maubinh.rules.MauBinhArrangement
import com.baiviet.game.maubinh.rules.MauBinhComboType
import com.baiviet.game.maubinh.rules.MauBinhEvaluator
import com.baiviet.game.maubinh.rules.MauBinhInstantWinType
import com.baiviet.game.maubinh.rules.MauBinhPlayerHand
import com.baiviet.game.maubinh.rules.MauBinhRules
import com.baiviet.game.maubinh.rules.MauBinhScoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MauBinhEvaluatorTest {

    @Test
    fun `evaluate Chi 1 and 2 combo types correctly`() {
        // High card
        val highCard = MauBinhEvaluator.evaluateChi1OrChi2(cards("2s 5h 7d Jc Kc"))
        assertEquals(MauBinhComboType.HIGH_CARD, highCard.type)

        // One pair
        val pair = MauBinhEvaluator.evaluateChi1OrChi2(cards("2s 2h 7d Jc Kc"))
        assertEquals(MauBinhComboType.ONE_PAIR, pair.type)

        // Two pair (Thú)
        val twoPair = MauBinhEvaluator.evaluateChi1OrChi2(cards("2s 2h 7d 7c Kc"))
        assertEquals(MauBinhComboType.TWO_PAIR, twoPair.type)

        // Three of a kind (Sám)
        val triple = MauBinhEvaluator.evaluateChi1OrChi2(cards("2s 2h 2d Jc Kc"))
        assertEquals(MauBinhComboType.THREE_OF_A_KIND, triple.type)

        // Straight (Sảnh)
        val straight = MauBinhEvaluator.evaluateChi1OrChi2(cards("7s 8h 9d 10c Jc"))
        assertEquals(MauBinhComboType.STRAIGHT, straight.type)

        // Flush (Thùng)
        val flush = MauBinhEvaluator.evaluateChi1OrChi2(cards("2s 5s 7s 9s Ks"))
        assertEquals(MauBinhComboType.FLUSH, flush.type)

        // Full house (Cù lũ)
        val fullHouse = MauBinhEvaluator.evaluateChi1OrChi2(cards("5s 5h 5d 8c 8s"))
        assertEquals(MauBinhComboType.FULL_HOUSE, fullHouse.type)

        // Four of a kind (Tứ quý)
        val quad = MauBinhEvaluator.evaluateChi1OrChi2(cards("9s 9h 9d 9c 2s"))
        assertEquals(MauBinhComboType.FOUR_OF_A_KIND, quad.type)

        // Straight flush (Thùng phá sảnh)
        val straightFlush = MauBinhEvaluator.evaluateChi1OrChi2(cards("8h 9h 10h Jh Qh"))
        assertEquals(MauBinhComboType.STRAIGHT_FLUSH, straightFlush.type)
    }

    @Test
    fun `straight A-2-3-4-5 is 2nd highest by default and lowest when house rule set`() {
        val royalStraight = MauBinhEvaluator.evaluateChi1OrChi2(cards("10s Jh Qd Kc As")) // 10-J-Q-K-A
        val a2345 = MauBinhEvaluator.evaluateChi1OrChi2(cards("As 2h 3d 4c 5s")) // A-2-3-4-5
        val kingStraight = MauBinhEvaluator.evaluateChi1OrChi2(cards("9s 10h Jd Qc Ks")) // 9-10-J-Q-K
        val lowestNormal = MauBinhEvaluator.evaluateChi1OrChi2(cards("2s 3h 4d 5c 6s")) // 2-3-4-5-6

        // Mặc định: 10-J-Q-K-A > A-2-3-4-5 > 9-10-J-Q-K
        assertTrue(royalStraight > a2345)
        assertTrue(a2345 > kingStraight)
        assertTrue(a2345 > lowestNormal)

        // Luật nhà: aceTwoThreeFourFiveIsLowest = true
        val rulesLowest = MauBinhRules(aceTwoThreeFourFiveIsLowest = true)
        val a2345Lowest = MauBinhEvaluator.evaluateChi1OrChi2(cards("As 2h 3d 4c 5s"), rulesLowest)
        val lowestNormalCustom = MauBinhEvaluator.evaluateChi1OrChi2(cards("2s 3h 4d 5c 6s"), rulesLowest)

        assertTrue(lowestNormalCustom > a2345Lowest)
    }

    @Test
    fun `detect foul when Chi 1 is weaker than Chi 2`() {
        // Chi 1 là Thùng, Chi 2 là Cù lũ -> Lủng
        val foulArr = MauBinhArrangement(
            chi1 = cards("2s 5s 7s 9s Ks"), // Thùng
            chi2 = cards("Jh Jd Jc 8s 8h"), // Cù lũ (mạnh hơn Chi 1)
            chi3 = cards("3s 3h 4d"),        // Đôi 3
        )
        assertTrue(foulArr.isFoul())
    }

    @Test
    fun `detect foul when Chi 2 is weaker than Chi 3`() {
        // Chi 1 là Cù lũ, Chi 2 là Đôi 8, Chi 3 là Đôi K -> Lủng
        val foulArr = MauBinhArrangement(
            chi1 = cards("Jh Jd Jc 4s 4h"), // Cù lũ
            chi2 = cards("8s 8h 5d 6c 7s"), // Đôi 8
            chi3 = cards("Ks Kh 2d"),        // Đôi K (mạnh hơn Đôi 8 ở Chi 2)
        )
        assertTrue(foulArr.isFoul())

        // Chi 2 là Mậu thầu A, Chi 3 là Đôi 2 -> Lủng
        val foulArr2 = MauBinhArrangement(
            chi1 = cards("Jh Jd Jc 4s 4h"),
            chi2 = cards("As Kh Qd 9c 5c"), // Mậu thầu
            chi3 = cards("2s 2h 3d"),        // Đôi 2
        )
        assertTrue(foulArr2.isFoul())
    }

    @Test
    fun `valid arrangement when Chi 1 is greater than or equal to Chi 2 and Chi 3`() {
        val validArr = MauBinhArrangement(
            chi1 = cards("Jh Jd Jc 4s 4h"), // Cù lũ J
            chi2 = cards("2s 5s 7s 9s Ks"), // Thùng K
            chi3 = cards("8s 8h 2d"),        // Đôi 8
        )
        assertFalse(validArr.isFoul())
    }

    @Test
    fun `detect instant win types correctly`() {
        // 1. Rồng cuốn: 13 lá cùng chất từ 2..A
        val dragonRoll = cards("2h 3h 4h 5h 6h 7h 8h 9h 10h Jh Qh Kh Ah")
        assertEquals(MauBinhInstantWinType.DRAGON_ROLL, MauBinhEvaluator.detectInstantWin(dragonRoll))

        // 2. Sảnh rồng: 13 lá 2..A khác chất
        val dragonStraight = cards("2s 3h 4d 5c 6s 7h 8d 9c 10s Jh Qd Kc Ah")
        assertEquals(MauBinhInstantWinType.DRAGON_STRAIGHT, MauBinhEvaluator.detectInstantWin(dragonStraight))

        // 3. Năm đôi một sám
        val fivePairsTriple = cards("2s 2h 2d 3s 3h 4s 4h 5s 5h 6s 6h 7s 7h")
        assertEquals(MauBinhInstantWinType.FIVE_PAIRS_ONE_TRIPLE, MauBinhEvaluator.detectInstantWin(fivePairsTriple))

        // 4. Lục phé bôn (6 đôi + 1 lá lẻ)
        val sixPairs = cards("2s 2h 3s 3h 4s 4h 5s 5h 6s 6h 7s 7h 9c")
        assertEquals(MauBinhInstantWinType.SIX_PAIRS, MauBinhEvaluator.detectInstantWin(sixPairs))

        // 5. Ba thùng (5 lá cùng chất, 5 lá cùng chất, 3 lá cùng chất)
        val threeFlushes = cards("2h 4h 6h 8h 10h 3s 5s 7s 9s Js 2d 5d Kd")
        assertEquals(MauBinhInstantWinType.THREE_FLUSHES, MauBinhEvaluator.detectInstantWin(threeFlushes))

        // 6. Ba sảnh (5 lá liên tiếp, 5 lá liên tiếp, 3 lá liên tiếp)
        val threeStraights = cards("3h 4h 5s 6s 7d 3c 4c 5d 6d 7c 8s 9s 10s")
        assertEquals(MauBinhInstantWinType.THREE_STRAIGHTS, MauBinhEvaluator.detectInstantWin(threeStraights))
    }

    @Test
    fun `scoring applies bonus chi correctly`() {
        val rules = MauBinhRules.DEFAULT
        val p1 = MauBinhPlayerHand(
            playerId = PlayerId(0),
            cards = emptyList(),
            arrangement = MauBinhArrangement(
                chi1 = cards("9s 9h 9d 9c 2s"), // Tứ quý Chi 1 (bonus 4 chi)
                chi2 = cards("8s 8h 8d 5c 5s"), // Cù lũ Chi 2 (bonus 2 chi)
                chi3 = cards("7s 7h 7d"),        // Sám Chi 3 (bonus 3 chi)
            ),
        )
        val p2 = MauBinhPlayerHand(
            playerId = PlayerId(1),
            cards = emptyList(),
            arrangement = MauBinhArrangement(
                chi1 = cards("As Kh Qd Jc 9h"), // Mậu thầu
                chi2 = cards("2s 2h 4d 5c 7c"), // Một đôi
                chi3 = cards("3s 4h 6d"),        // Mậu thầu
            ),
        )

        // p1 thắng cả 3 chi bằng các chi thưởng:
        // Chi 1: 4 chi (Tứ quý)
        // Chi 2: 2 chi (Cù lũ)
        // Chi 3: 3 chi (Sám cô)
        // Tổng cơ sở = 9 chi.
        // Sập 3 chi (×2) -> 9 * 2 = 18 chi!
        val comparison = MauBinhScoring.comparePair(
            hand1 = p1,
            hand2 = p2,
            isP1Foul = false,
            isP2Foul = false,
            isP1SapLang = false,
            isP2SapLang = false,
            rules = rules,
        )

        assertTrue(comparison.isSap3ChiForP1)
        assertEquals(18, comparison.netChiForP1)
    }

    @Test
    fun `scoring sap lang doubles score again for 3 or more players`() {
        val rules = MauBinhRules.DEFAULT
        val p1 = MauBinhPlayerHand(
            playerId = PlayerId(0),
            cards = emptyList(),
            arrangement = MauBinhArrangement(
                chi1 = cards("As Kh Qd Jc 10s"), // Sảnh A
                chi2 = cards("8s 8h 8d 5c 4s"),  // Sám 8
                chi3 = cards("7s 7h 2d"),         // Đôi 7
            ),
        )
        val p2 = MauBinhPlayerHand(
            playerId = PlayerId(1),
            cards = emptyList(),
            arrangement = MauBinhArrangement(
                chi1 = cards("Ks Qh Jd 10c 9s"),  // Sảnh K (yếu hơn Sảnh A ở Chi 1)
                chi2 = cards("6s 6h 6d 3c 2s"),  // Sám 6 (yếu hơn Sám 8 ở Chi 2)
                chi3 = cards("5s 5h 4d"),         // Đôi 5 (yếu hơn Đôi 7 ở Chi 3)
            ),
        )
        val p3 = MauBinhPlayerHand(
            playerId = PlayerId(2),
            cards = emptyList(),
            arrangement = MauBinhArrangement(
                chi1 = cards("Qs Jh 10d 9c 8s"),  // Sảnh Q (yếu hơn Sảnh A ở Chi 1)
                chi2 = cards("4h 4d 4c 3s 2h"),  // Sám 4 (yếu hơn Sám 8 ở Chi 2)
                chi3 = cards("3h 3d 2c"),         // Đôi 3 (yếu hơn Đôi 7 ở Chi 3)
            ),
        )

        val settlement = MauBinhScoring.settle(listOf(p1, p2, p3), rules, baseBet = 100L)
        // P1 sập cả P2 và P3 -> Sập làng!
        // Chi 1 (+1), Chi 2 (+1), Chi 3 (+1) = 3 chi base.
        // Sập 3 chi (×2) = 6 chi.
        // Sập làng (×2 thêm) = 12 chi cho mỗi người!
        // P1 ăn 12 + 12 = 24 chi = 2400B.
        assertEquals(2400L, settlement.deltas[PlayerId(0)])
        assertEquals(0L, settlement.deltas.values.sum())
    }

    @Test
    fun `foul player loses 6 chi to each valid player and ties another foul player`() {
        val rules = MauBinhRules.DEFAULT
        val pValid = MauBinhPlayerHand(
            playerId = PlayerId(0),
            cards = emptyList(),
            arrangement = MauBinhArrangement(
                chi1 = cards("As Kh Qd Jc 10s"),
                chi2 = cards("8s 8h 8d 5c 4s"),
                chi3 = cards("7s 7h 2d"),
            ),
        )
        val pFoul1 = MauBinhPlayerHand(
            playerId = PlayerId(1),
            cards = emptyList(),
            arrangement = MauBinhArrangement(
                chi1 = cards("2s 3h 4d 5s 7c"), // Mậu thầu (5 lá riêng biệt)
                chi2 = cards("8c 8s 8d 6c 4c"), // Sám cô (mạnh hơn Chi 1 -> FOUL)
                chi3 = cards("7s 7h 2c"),
            ),
        )
        val pFoul2 = MauBinhPlayerHand(
            playerId = PlayerId(2),
            cards = emptyList(),
            arrangement = MauBinhArrangement(
                chi1 = cards("3c 4s 5h 6s 8h"), // Mậu thầu
                chi2 = cards("9s 9h 9d 6d 4h"), // Sám cô (mạnh hơn Chi 1 -> FOUL)
                chi3 = cards("7d 7c 2h"),
            ),
        )

        val settlement = MauBinhScoring.settle(listOf(pValid, pFoul1, pFoul2), rules, baseBet = 100L)
        // pValid thắng mỗi người lủng 6 chi -> 12 chi = +1200B
        // pFoul1 thua pValid 6 chi = -600B, hòa pFoul2 = 0
        // pFoul2 thua pValid 6 chi = -600B, hòa pFoul1 = 0
        assertEquals(1200L, settlement.deltas[PlayerId(0)])
        assertEquals(-600L, settlement.deltas[PlayerId(1)])
        assertEquals(-600L, settlement.deltas[PlayerId(2)])
        assertEquals(0L, settlement.deltas.values.sum())
    }
}
