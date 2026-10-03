package com.baiviet.core.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test bộ bài và xáo bài — mục 8 trong đặc tả:
 * "Xáo bài luôn ra đúng 52 lá khác nhau."
 */
class DeckTest {

    @Test
    fun `full deck has exactly 52 unique cards`() {
        val deck = Deck.FULL_DECK
        assertEquals(52, deck.size)
        assertEquals(52, deck.toSet().size)
    }

    @Test
    fun `full deck has 4 suits x 13 ranks`() {
        val deck = Deck.FULL_DECK
        for (suit in Suit.entries) {
            val cardsOfSuit = deck.filter { it.suit == suit }
            assertEquals("Mỗi chất phải có 13 lá", 13, cardsOfSuit.size)
        }
        for (rank in Rank.entries) {
            val cardsOfRank = deck.filter { it.rank == rank }
            assertEquals("Mỗi số phải có 4 lá", 4, cardsOfRank.size)
        }
    }

    @Test
    fun `shuffled deck has 52 unique cards`() {
        val (_, shuffled) = Deck.shuffled()
        assertEquals(52, shuffled.size)
        assertEquals(52, shuffled.toSet().size)
    }

    @Test
    fun `same seed produces same shuffle`() {
        val seed = 12345L
        val (_, deck1) = Deck.shuffled(seed)
        val (_, deck2) = Deck.shuffled(seed)
        assertEquals(deck1, deck2)
    }

    @Test
    fun `different seeds produce different shuffles`() {
        val (_, deck1) = Deck.shuffled(111L)
        val (_, deck2) = Deck.shuffled(222L)
        assertNotEquals(deck1, deck2)
    }

    @Test
    fun `shuffled deck contains same cards as original`() {
        val (_, shuffled) = Deck.shuffled()
        assertEquals(Deck.FULL_DECK.toSet(), shuffled.toSet())
    }

    @Test
    fun `shuffle is actually shuffling (not identity)`() {
        // 52! makes identical output effectively impossible
        var allSame = true
        repeat(10) {
            val (_, shuffled) = Deck.shuffled()
            if (shuffled != Deck.FULL_DECK) allSame = false
        }
        assertTrue("10 lần xáo đều giống bộ gốc — bất thường", !allSame)
    }

    @Test
    fun `deal distributes cards correctly for 4 players x 13 cards`() {
        val (_, deck) = Deck.shuffled(42L)
        val (hands, remainder) = Deck.deal(deck, playerCount = 4, cardsPerPlayer = 13)
        assertEquals(4, hands.size)
        hands.forEach { hand -> assertEquals(13, hand.size) }
        assertEquals(0, remainder.size)
        // Tổng lá = 52
        assertEquals(52, hands.sumOf { it.size } + remainder.size)
        // Không trùng lá
        assertEquals(52, hands.flatten().toSet().size)
    }

    @Test
    fun `deal with 2 players x 13 cards leaves 26 remainder`() {
        val (_, deck) = Deck.shuffled(42L)
        val (hands, remainder) = Deck.deal(deck, playerCount = 2, cardsPerPlayer = 13)
        assertEquals(2, hands.size)
        hands.forEach { hand -> assertEquals(13, hand.size) }
        assertEquals(26, remainder.size)
    }

    @Test
    fun `deal with extra first player card for Phom`() {
        val (_, deck) = Deck.shuffled(42L)
        val (hands, remainder) = Deck.deal(
            deck, playerCount = 4, cardsPerPlayer = 9, extraFirstPlayer = 1,
        )
        assertEquals(10, hands[0].size) // Người đi đầu 10 lá
        assertEquals(9, hands[1].size)
        assertEquals(9, hands[2].size)
        assertEquals(9, hands[3].size)
        assertEquals(52 - 37, remainder.size) // 52 - (10+9+9+9) = 15 nọc
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deal throws when not enough cards`() {
        val (_, deck) = Deck.shuffled(42L)
        Deck.deal(deck, playerCount = 5, cardsPerPlayer = 13)
    }
}
