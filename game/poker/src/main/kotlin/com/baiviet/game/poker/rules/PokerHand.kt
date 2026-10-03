package com.baiviet.game.poker.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import kotlinx.serialization.Serializable

@Serializable
enum class PokerHandType(val strength: Int, val vietnameseName: String) {
    HIGH_CARD(1, "Mậu thầu"),
    ONE_PAIR(2, "Một đôi"),
    TWO_PAIR(3, "Thú (2 đôi)"),
    THREE_OF_A_KIND(4, "Sám cô"),
    STRAIGHT(5, "Sảnh"),
    FLUSH(6, "Thùng"),
    FULL_HOUSE(7, "Cù lũ"),
    FOUR_OF_A_KIND(8, "Tứ quý"),
    STRAIGHT_FLUSH(9, "Thùng phá sảnh"),
    ROYAL_FLUSH(10, "Thùng phá sảnh lớn"),
}

@Serializable
data class PokerHand(
    val type: PokerHandType,
    val tieBreakers: List<Int>,
    val best5Cards: List<Card> = emptyList(),
    val description: String = "",
) : Comparable<PokerHand> {

    override fun compareTo(other: PokerHand): Int {
        if (this.type.strength != other.type.strength) {
            return this.type.strength.compareTo(other.type.strength)
        }
        val minSize = minOf(this.tieBreakers.size, other.tieBreakers.size)
        for (i in 0 until minSize) {
            val cmp = this.tieBreakers[i].compareTo(other.tieBreakers[i])
            if (cmp != 0) return cmp
        }
        return this.tieBreakers.size.compareTo(other.tieBreakers.size)
    }

    companion object {
        fun rankToValue(rank: Rank): Int = when (rank) {
            Rank.TWO -> 2
            Rank.THREE -> 3
            Rank.FOUR -> 4
            Rank.FIVE -> 5
            Rank.SIX -> 6
            Rank.SEVEN -> 7
            Rank.EIGHT -> 8
            Rank.NINE -> 9
            Rank.TEN -> 10
            Rank.JACK -> 11
            Rank.QUEEN -> 12
            Rank.KING -> 13
            Rank.ACE -> 14
        }

        fun valueToRankName(value: Int): String = when (value) {
            14 -> "A"
            13 -> "K"
            12 -> "Q"
            11 -> "J"
            10 -> "10"
            else -> value.toString()
        }
    }
}
