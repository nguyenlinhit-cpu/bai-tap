package com.baiviet.game.phom.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import kotlinx.serialization.Serializable

/**
 * Thứ tự và điểm bài trong Phỏm:
 * A (1 điểm, nhỏ nhất) < 2 (2) < 3 (3) < ... < 10 (10) < J (11) < Q (12) < K (13 điểm, lớn nhất).
 * A là lá nhỏ nhất, không nối vòng qua K (Q-K-A không hợp lệ).
 */
object PhomOrder {
    fun rankPoint(rank: Rank): Int = when (rank) {
        Rank.ACE -> 1
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
    }

    fun rankIndex(rank: Rank): Int = rankPoint(rank) - 1 // 0..12

    fun suitIndex(suit: Suit): Int = when (suit) {
        Suit.SPADE -> 0
        Suit.CLUB -> 1
        Suit.DIAMOND -> 2
        Suit.HEART -> 3
    }

    /** Xếp bài trên tay: gom theo suit rồi rank, hoặc rank rồi suit. */
    val handComparator: Comparator<Card> =
        compareBy<Card> { rankIndex(it.rank) }.thenBy { suitIndex(it.suit) }

    val suitThenRankComparator: Comparator<Card> =
        compareBy<Card> { suitIndex(it.suit) }.thenBy { rankIndex(it.rank) }
}

val Card.phomPoint: Int get() = PhomOrder.rankPoint(rank)
val Card.phomIndex: Int get() = PhomOrder.rankIndex(rank)

fun List<Card>.sortedPhom(): List<Card> = sortedWith(PhomOrder.handComparator)
fun List<Card>.sortedBySuit(): List<Card> = sortedWith(PhomOrder.suitThenRankComparator)

@Serializable
enum class PhomMeldType(val viName: String) {
    GROUP("Phỏm ngang"), // 3 hoặc 4 lá cùng số
    RUN("Phỏm dọc"),     // ≥ 3 lá liên tiếp cùng chất
}

/**
 * Một phỏm hợp lệ.
 *
 * @param type loại phỏm (ngang hoặc dọc)
 * @param cards các lá tạo phỏm
 * @param eatenCard lá bài đã ăn tạo nên phỏm này (nếu có, tối đa 1 lá)
 */
@Serializable
data class PhomMeld(
    val type: PhomMeldType,
    val cards: List<Card>,
    val eatenCard: Card? = null,
) {
    init {
        require(cards.size >= 3) { "Phỏm phải có ít nhất 3 lá bài" }
        if (eatenCard != null) {
            require(eatenCard in cards) { "Lá ăn phải thuộc phỏm" }
        }
    }

    val size: Int get() = cards.size

    val description: String
        get() = when (type) {
            PhomMeldType.GROUP -> "Phỏm ${cards.first().rank.label}"
            PhomMeldType.RUN -> "Phỏm ${cards.first().rank.label}→${cards.last().rank.label} ${cards.first().suit.symbol}"
        }

    /** Kiểm tra xem lá [candidate] có thể gửi vào phỏm này không. */
    fun canLayOff(candidate: Card): Boolean {
        if (candidate in cards) return false
        return when (type) {
            PhomMeldType.GROUP -> {
                // Phỏm ngang chỉ chứa tối đa 4 lá, cùng rank, khác chất
                cards.size == 3 && candidate.rank == cards.first().rank
            }
            PhomMeldType.RUN -> {
                // Phỏm dọc cùng chất, nối vào đầu hoặc cuối
                if (candidate.suit != cards.first().suit) return false
                val firstIdx = cards.first().phomIndex
                val lastIdx = cards.last().phomIndex
                val candIdx = candidate.phomIndex
                candIdx == firstIdx - 1 || candIdx == lastIdx + 1
            }
        }
    }

    /** Tạo phỏm mới sau khi gửi lá [extraCard] vào. */
    fun withLayOff(extraCard: Card): PhomMeld {
        require(canLayOff(extraCard)) { "Không thể gửi $extraCard vào phỏm này" }
        val newCards = (cards + extraCard).sortedWith(
            if (type == PhomMeldType.RUN) PhomOrder.suitThenRankComparator else PhomOrder.handComparator,
        )
        return copy(cards = newCards)
    }

    companion object {
        /**
         * Kiểm tra và phân loại danh sách các lá bài thành một Phỏm hợp lệ.
         * Trả về [PhomMeld] nếu hợp lệ, ngược lại null.
         */
        fun classify(cards: Collection<Card>, eatenCard: Card? = null): PhomMeld? {
            if (cards.size < 3) return null
            if (cards.toSet().size != cards.size) return null
            if (eatenCard != null && eatenCard !in cards) return null

            // 1. Kiểm tra phỏm ngang (cùng rank, khác chất, 3 hoặc 4 lá)
            val firstRank = cards.first().rank
            if (cards.all { it.rank == firstRank } && cards.size in 3..4) {
                return PhomMeld(PhomMeldType.GROUP, cards.toList().sortedPhom(), eatenCard)
            }

            // 2. Kiểm tra phỏm dọc (cùng chất, các rank liên tiếp)
            val firstSuit = cards.first().suit
            if (cards.all { it.suit == firstSuit }) {
                val sorted = cards.toList().sortedBy { it.phomIndex }
                val indices = sorted.map { it.phomIndex }
                val minIdx = indices.first()
                val isConsecutive = indices.indices.all { indices[it] == minIdx + it }
                if (isConsecutive) {
                    return PhomMeld(PhomMeldType.RUN, sorted, eatenCard)
                }
            }

            return null
        }
    }
}

/** Loại cạ trong Phỏm (2 lá bài chờ ghép thành phỏm). */
enum class PhomCaType {
    SAME_RANK,    // 2 lá cùng rank (ví dụ: 7♠ 7♥)
    CONSECUTIVE,  // 2 lá liên tiếp cùng chất (ví dụ: 8♦ 9♦)
    GAP_ONE,      // 2 lá cùng chất cách 1 số (ví dụ: 7♠ 9♠)
}

/**
 * Một cạ trên tay bài.
 */
data class PhomCa(
    val type: PhomCaType,
    val cards: Pair<Card, Card>,
    val neededRanks: List<Rank>,
) {
    fun canCompleteWith(card: Card): Boolean {
        return when (type) {
            PhomCaType.SAME_RANK -> card.rank == cards.first.rank && card.suit != cards.first.suit && card.suit != cards.second.suit
            PhomCaType.CONSECUTIVE, PhomCaType.GAP_ONE -> card.suit == cards.first.suit && card.rank in neededRanks
        }
    }
}
