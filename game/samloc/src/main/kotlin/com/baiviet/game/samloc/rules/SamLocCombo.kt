package com.baiviet.game.samloc.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import kotlinx.serialization.Serializable

/**
 * Thứ tự lá trong Sâm Lốc: 3 < 4 < 5 < 6 < 7 < 8 < 9 < 10 < J < Q < K < A < 2.
 * ĐẶC BIỆT: Sâm Lốc KHÔNG so chất. Hai bộ bằng nhau không thể chặn nhau.
 */
object SlOrder {
    /** Chỉ số số bài theo luật Sâm: 3 = 0 … A = 11, 2 = 12. */
    fun rankIndex(rank: Rank): Int =
        when (rank) {
            Rank.THREE -> 0
            Rank.FOUR -> 1
            Rank.FIVE -> 2
            Rank.SIX -> 3
            Rank.SEVEN -> 4
            Rank.EIGHT -> 5
            Rank.NINE -> 6
            Rank.TEN -> 7
            Rank.JACK -> 8
            Rank.QUEEN -> 9
            Rank.KING -> 10
            Rank.ACE -> 11
            Rank.TWO -> 12
        }

    fun suitIndex(suit: Suit): Int =
        when (suit) {
            Suit.SPADE -> 0
            Suit.CLUB -> 1
            Suit.DIAMOND -> 2
            Suit.HEART -> 3
        }

    /** Dùng để xếp bài hiển thị trên tay (so rank trước, so chất phụ). */
    val handCardComparator: Comparator<Card> =
        compareBy<Card> { rankIndex(it.rank) }.thenBy { suitIndex(it.suit) }

    const val TWO_INDEX = 12
    const val ACE_INDEX = 11
}

val Card.slRank: Int get() = SlOrder.rankIndex(rank)
val Card.isTwo: Boolean get() = rank == Rank.TWO

fun List<Card>.sortedSl(): List<Card> = sortedWith(SlOrder.handCardComparator)

/** Loại bộ bài trong Sâm Lốc (không có đôi thông). */
@Serializable
enum class SamLocComboType(
    val viName: String,
) {
    SINGLE("Rác"),
    PAIR("Đôi"),
    TRIPLE("Sám"),
    STRAIGHT("Sảnh"),
    QUAD("Tứ quý"),
}

/**
 * Một bộ bài hợp lệ trong Sâm Lốc.
 */
@Serializable
data class SamLocCombo(
    val type: SamLocComboType,
    val cards: List<Card>,
) {
    /** Lá lớn nhất (trong sảnh: lá cuối quyết định độ lớn sảnh). */
    val top: Card get() = cards.last()

    val size: Int get() = cards.size

    val hasTwo: Boolean get() = cards.any { it.isTwo }

    /** Tứ quý là hàng chặt duy nhất trong Sâm Lốc. */
    val isQuad: Boolean get() = type == SamLocComboType.QUAD

    /** Heo lẻ hoặc đôi heo hoặc sám heo. */
    val isTwos: Boolean get() = cards.all { it.isTwo }

    val description: String
        get() =
            when (type) {
                SamLocComboType.SINGLE -> if (top.isTwo) "Heo" else "Rác ${top.rank.label}"
                SamLocComboType.PAIR -> if (top.isTwo) "Đôi heo" else "Đôi ${top.rank.label}"
                SamLocComboType.TRIPLE -> if (top.isTwo) "Sám heo" else "Sám ${top.rank.label}"
                SamLocComboType.STRAIGHT -> "Sảnh ${cards.first().rank.label}→${top.rank.label}"
                SamLocComboType.QUAD -> "Tứ quý ${top.rank.label}"
            }

    companion object {
        /**
         * Nhận diện bộ bài trong Sâm Lốc.
         *
         * - Rác: 1 lá
         * - Đôi: 2 lá cùng rank
         * - Sám: 3 lá cùng rank
         * - Tứ quý: 4 lá cùng rank
         * - Sảnh: ≥ 3 lá liên tiếp, không cần cùng chất.
         *   Cho phép: A-2-3 (A đứng đầu), 2-3-4 (2 đứng đầu), Q-K-A (A đứng cuối).
         *   Không cho phép vòng qua (K-A-2 sai).
         */
        fun classify(cards: Collection<Card>): SamLocCombo? {
            if (cards.isEmpty()) return null
            val n = cards.size
            if (cards.toSet().size != n) return null

            val sorted = cards.toList().sortedSl()
            val ranks = sorted.map { it.slRank }

            if (n == 1) return SamLocCombo(SamLocComboType.SINGLE, sorted)

            // Cùng rank: đôi, sám, tứ quý
            if (ranks.all { it == ranks[0] }) {
                return when (n) {
                    2 -> SamLocCombo(SamLocComboType.PAIR, sorted)
                    3 -> SamLocCombo(SamLocComboType.TRIPLE, sorted)
                    4 -> SamLocCombo(SamLocComboType.QUAD, sorted)
                    else -> null
                }
            }

            // Sảnh: n >= 3, các rank phải phân biệt
            if (n >= 3 && ranks.distinct().size == n) {
                val straightCards = checkStraight(sorted)
                if (straightCards != null) {
                    return SamLocCombo(SamLocComboType.STRAIGHT, straightCards)
                }
            }

            return null
        }

        /**
         * Kiểm tra các lá có tạo thành sảnh hợp lệ trong Sâm Lốc hay không.
         * Trả về danh sách lá bài xếp theo thứ tự sảnh (lá cuối cùng là lá quyết định độ lớn sảnh),
         * hoặc null nếu không phải sảnh hợp lệ.
         */
        private fun checkStraight(sortedCards: List<Card>): List<Card>? {
            val n = sortedCards.size
            val hasTwo = sortedCards.any { it.isTwo }
            val hasAce = sortedCards.any { it.rank == Rank.ACE }

            if (hasTwo) {
                val twoCard = sortedCards.first { it.isTwo }
                val nonTwos = sortedCards.filter { !it.isTwo }

                if (hasAce) {
                    // Phải là sảnh bắt đầu bằng A-2-3-4...
                    // Thứ tự: A (lá 0), 2 (lá 1), 3 (lá 2), 4, ...
                    val aceCard = sortedCards.first { it.rank == Rank.ACE }
                    val rest = sortedCards.filter { it != aceCard && it != twoCard }
                    // non-Ace, non-Two cards must be 3, 4, ..., n
                    val expectedRanks = (0 until (n - 2)).toList() // 3 = index 0, 4 = 1, ...
                    val actualRanks = rest.map { it.slRank }.sorted()
                    if (actualRanks == expectedRanks) {
                        return listOf(aceCard, twoCard) + rest.sortedBy { it.slRank }
                    }
                    return null
                } else {
                    // Bắt đầu bằng 2-3-4-5...
                    // non-Two cards must be 3, 4, ..., n+1
                    val expectedRanks = (0 until (n - 1)).toList() // 3 = index 0, 4 = 1, ...
                    val actualRanks = nonTwos.map { it.slRank }.sorted()
                    if (actualRanks == expectedRanks) {
                        return listOf(twoCard) + nonTwos.sortedBy { it.slRank }
                    }
                    return null
                }
            } else {
                // Không chứa 2: các rank từ 3 (0) đến A (11) phải liên tiếp
                val ranks = sortedCards.map { it.slRank }.sorted()
                val minR = ranks.first()
                if (ranks.indices.all { ranks[it] == minR + it }) {
                    return sortedCards.sortedBy { it.slRank }
                }
                return null
            }
        }
    }
}
