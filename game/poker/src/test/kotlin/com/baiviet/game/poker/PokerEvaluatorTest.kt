package com.baiviet.game.poker

import com.baiviet.game.poker.TestCards.Ac
import com.baiviet.game.poker.TestCards.Ad
import com.baiviet.game.poker.TestCards.Ah
import com.baiviet.game.poker.TestCards.As
import com.baiviet.game.poker.TestCards.Jc
import com.baiviet.game.poker.TestCards.Jd
import com.baiviet.game.poker.TestCards.Jh
import com.baiviet.game.poker.TestCards.Js
import com.baiviet.game.poker.TestCards.Kc
import com.baiviet.game.poker.TestCards.Kd
import com.baiviet.game.poker.TestCards.Kh
import com.baiviet.game.poker.TestCards.Ks
import com.baiviet.game.poker.TestCards.N2c
import com.baiviet.game.poker.TestCards.N2d
import com.baiviet.game.poker.TestCards.N2h
import com.baiviet.game.poker.TestCards.N2s
import com.baiviet.game.poker.TestCards.N3c
import com.baiviet.game.poker.TestCards.N3d
import com.baiviet.game.poker.TestCards.N3h
import com.baiviet.game.poker.TestCards.N3s
import com.baiviet.game.poker.TestCards.N4c
import com.baiviet.game.poker.TestCards.N4d
import com.baiviet.game.poker.TestCards.N4h
import com.baiviet.game.poker.TestCards.N4s
import com.baiviet.game.poker.TestCards.N5c
import com.baiviet.game.poker.TestCards.N5d
import com.baiviet.game.poker.TestCards.N5h
import com.baiviet.game.poker.TestCards.N5s
import com.baiviet.game.poker.TestCards.N6c
import com.baiviet.game.poker.TestCards.N6d
import com.baiviet.game.poker.TestCards.N6h
import com.baiviet.game.poker.TestCards.N6s
import com.baiviet.game.poker.TestCards.N7c
import com.baiviet.game.poker.TestCards.N7d
import com.baiviet.game.poker.TestCards.N7h
import com.baiviet.game.poker.TestCards.N7s
import com.baiviet.game.poker.TestCards.N8c
import com.baiviet.game.poker.TestCards.N8d
import com.baiviet.game.poker.TestCards.N8h
import com.baiviet.game.poker.TestCards.N8s
import com.baiviet.game.poker.TestCards.N9c
import com.baiviet.game.poker.TestCards.N9d
import com.baiviet.game.poker.TestCards.N9h
import com.baiviet.game.poker.TestCards.N9s
import com.baiviet.game.poker.TestCards.Qc
import com.baiviet.game.poker.TestCards.Qd
import com.baiviet.game.poker.TestCards.Qh
import com.baiviet.game.poker.TestCards.Qs
import com.baiviet.game.poker.TestCards.Tc
import com.baiviet.game.poker.TestCards.Td
import com.baiviet.game.poker.TestCards.Th
import com.baiviet.game.poker.TestCards.Ts
import com.baiviet.game.poker.rules.PokerEvaluator
import com.baiviet.game.poker.rules.PokerHandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokerEvaluatorTest {

    @Test
    fun testRoyalFlush() {
        val cards = listOf(As, Ks, Qs, Js, Ts)
        val hand = PokerEvaluator.evaluate(cards)
        assertEquals(PokerHandType.ROYAL_FLUSH, hand.type)
        assertEquals(listOf(14), hand.tieBreakers)
    }

    @Test
    fun testStraightFlush() {
        val cards = listOf(N9s, N8s, N7s, N6s, N5s)
        val hand = PokerEvaluator.evaluate(cards)
        assertEquals(PokerHandType.STRAIGHT_FLUSH, hand.type)
        assertEquals(listOf(9), hand.tieBreakers)
    }

    @Test
    fun testFourOfAKind() {
        val cards = listOf(As, Ah, Ad, Ac, Ks)
        val hand = PokerEvaluator.evaluate(cards)
        assertEquals(PokerHandType.FOUR_OF_A_KIND, hand.type)
        assertEquals(listOf(14, 13), hand.tieBreakers)
    }

    @Test
    fun testFullHouse() {
        val cards = listOf(As, Ah, Ad, Ks, Kh)
        val hand = PokerEvaluator.evaluate(cards)
        assertEquals(PokerHandType.FULL_HOUSE, hand.type)
        assertEquals(listOf(14, 13), hand.tieBreakers)
    }

    @Test
    fun testFlush() {
        val cards = listOf(As, Js, N8s, N5s, N2s)
        val hand = PokerEvaluator.evaluate(cards)
        assertEquals(PokerHandType.FLUSH, hand.type)
        assertEquals(listOf(14, 11, 8, 5, 2), hand.tieBreakers)
    }

    @Test
    fun testBroadwayStraight() {
        val cards = listOf(As, Kh, Qd, Jc, Ts)
        val hand = PokerEvaluator.evaluate(cards)
        assertEquals(PokerHandType.STRAIGHT, hand.type)
        assertEquals(listOf(14), hand.tieBreakers)
    }

    @Test
    fun testWheelStraight() {
        // A-2-3-4-5
        val cards = listOf(As, N2h, N3d, N4c, N5s)
        val hand = PokerEvaluator.evaluate(cards)
        assertEquals(PokerHandType.STRAIGHT, hand.type)
        assertEquals(listOf(5), hand.tieBreakers)
    }

    @Test
    fun testWheelVsSixHighStraight() {
        val wheel = PokerEvaluator.evaluate(listOf(As, N2h, N3d, N4c, N5s))
        val sixHigh = PokerEvaluator.evaluate(listOf(N6c, N5h, N4d, N3s, N2c))
        assertTrue(sixHigh > wheel)
    }

    @Test
    fun testThreeOfAKind() {
        val cards = listOf(Qs, Qh, Qd, Ks, N3c)
        val hand = PokerEvaluator.evaluate(cards)
        assertEquals(PokerHandType.THREE_OF_A_KIND, hand.type)
        assertEquals(listOf(12, 13, 3), hand.tieBreakers)
    }

    @Test
    fun testTwoPair() {
        val cards = listOf(Ks, Kh, N8s, N8d, As)
        val hand = PokerEvaluator.evaluate(cards)
        assertEquals(PokerHandType.TWO_PAIR, hand.type)
        assertEquals(listOf(13, 8, 14), hand.tieBreakers)
    }

    @Test
    fun testTwoPairKickerTieBreaker() {
        val hand1 = PokerEvaluator.evaluate(listOf(Ks, Kh, N8s, N8d, As)) // KK88A
        val hand2 = PokerEvaluator.evaluate(listOf(Kc, Kd, N8h, N8c, Qs)) // KK88Q
        assertTrue(hand1 > hand2)
    }

    @Test
    fun testOnePair() {
        val cards = listOf(Js, Jh, As, Kd, N9c)
        val hand = PokerEvaluator.evaluate(cards)
        assertEquals(PokerHandType.ONE_PAIR, hand.type)
        assertEquals(listOf(11, 14, 13, 9), hand.tieBreakers)
    }

    @Test
    fun testHighCard() {
        val cards = listOf(As, Kd, N9s, N7c, N2h)
        val hand = PokerEvaluator.evaluate(cards)
        assertEquals(PokerHandType.HIGH_CARD, hand.type)
        assertEquals(listOf(14, 13, 9, 7, 2), hand.tieBreakers)
    }

    @Test
    fun testSelectBest5From7Cards() {
        // 2 hole cards: As, Ks. Board: Qs, Js, Ts, 2c, 3h -> Should make Royal Flush!
        val sevenCards = listOf(As, Ks, Qs, Js, Ts, N2c, N3h)
        val hand = PokerEvaluator.evaluate(sevenCards)
        assertEquals(PokerHandType.ROYAL_FLUSH, hand.type)
    }

    @Test
    fun testSplitHandWithIdenticalRanks() {
        // Board: A-K-Q-J-9 rainbow. Hole 1: 2-3, Hole 2: 4-5 -> Both play the board!
        val hand1 = PokerEvaluator.evaluate(listOf(N2s, N3h, As, Kd, Qh, Jc, N9d))
        val hand2 = PokerEvaluator.evaluate(listOf(N4s, N5h, As, Kd, Qh, Jc, N9d))
        assertEquals(0, hand1.compareTo(hand2))
    }
}
