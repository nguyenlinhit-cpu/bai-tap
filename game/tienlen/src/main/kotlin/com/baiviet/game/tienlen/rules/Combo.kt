package com.baiviet.game.tienlen.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import kotlinx.serialization.Serializable

/**
 * Thứ tự lá trong Tiến lên: 3 < 4 < … < K < A < 2; chất ♠ < ♣ < ♦ < ♥.
 * So hai lá: so số trước, bằng số thì so chất.
 */
object TlOrder {
    /** Chỉ số số bài: 3 = 0 … A = 11, 2 = 12. */
    fun rankIndex(rank: Rank): Int = when (rank) {
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

    fun suitIndex(suit: Suit): Int = when (suit) {
        Suit.SPADE -> 0
        Suit.CLUB -> 1
        Suit.DIAMOND -> 2
        Suit.HEART -> 3
    }

    /** Giá trị tuyệt đối 0..51 để so sánh. */
    fun value(card: Card): Int = rankIndex(card.rank) * 4 + suitIndex(card.suit)

    /** Rank theo chỉ số 0..12. */
    val RANKS: List<Rank> = Rank.entries.sortedBy { rankIndex(it) }

    val cardComparator: Comparator<Card> = compareBy { value(it) }

    const val TWO_INDEX = 12
}

val Card.tlValue: Int get() = TlOrder.value(this)
val Card.tlRank: Int get() = TlOrder.rankIndex(rank)
val Card.isTwo: Boolean get() = rank == Rank.TWO

fun List<Card>.sortedTl(): List<Card> = sortedWith(TlOrder.cardComparator)

/** Loại bộ bài trong Tiến lên. */
@Serializable
enum class ComboType(val viName: String) {
    SINGLE("Rác"),
    PAIR("Đôi"),
    TRIPLE("Sám"),
    STRAIGHT("Sảnh"),
    PAIR_SEQUENCE("Đôi thông"),
    QUAD("Tứ quý"),
}

/**
 * Một bộ bài hợp lệ.
 *
 * @param cards các lá, sắp tăng dần theo [TlOrder]
 */
@Serializable
data class Combo(val type: ComboType, val cards: List<Card>) {

    /** Lá lớn nhất — dùng để so hai bộ cùng loại. */
    val top: Card get() = cards.last()

    val size: Int get() = cards.size

    /** Số đôi của đôi thông. */
    val pairCount: Int get() = if (type == ComboType.PAIR_SEQUENCE) cards.size / 2 else 0

    /** Bộ có chứa heo (lá 2). */
    val hasTwo: Boolean get() = cards.any { it.isTwo }

    /** "Hàng": 3+ đôi thông hoặc tứ quý. */
    val isBomb: Boolean get() = type == ComboType.QUAD || type == ComboType.PAIR_SEQUENCE

    /** Heo lẻ hoặc đôi heo hoặc sám heo. */
    val isTwos: Boolean get() = cards.all { it.isTwo }

    /** Mô tả tiếng Việt, ví dụ "Đôi 9", "Sảnh 5→9", "3 đôi thông". */
    val description: String
        get() = when (type) {
            ComboType.SINGLE -> if (top.isTwo) "Heo ${top.shortName}" else "Rác ${top.shortName}"
            ComboType.PAIR -> if (top.isTwo) "Đôi heo" else "Đôi ${top.rank.label}"
            ComboType.TRIPLE -> if (top.isTwo) "Sám heo" else "Sám ${top.rank.label}"
            ComboType.STRAIGHT -> "Sảnh ${cards.first().rank.label}→${top.rank.label}"
            ComboType.PAIR_SEQUENCE -> "$pairCount đôi thông"
            ComboType.QUAD -> "Tứ quý ${top.rank.label}"
        }

    companion object {
        /**
         * Nhận diện bộ bài. Trả về null nếu không phải bộ hợp lệ.
         *
         * - Sảnh: ≥ 3 lá liên tiếp, không chứa 2, A chỉ đứng cuối.
         * - Đôi thông: ≥ 3 đôi liên tiếp, không chứa 2.
         */
        fun classify(cards: Collection<Card>): Combo? {
            if (cards.isEmpty()) return null
            val sorted = cards.toList().sortedTl()
            if (sorted.toSet().size != sorted.size) return null
            val n = sorted.size
            val ranks = sorted.map { it.tlRank }
            if (n == 1) return Combo(ComboType.SINGLE, sorted)
            if (ranks.all { it == ranks[0] }) {
                return when (n) {
                    2 -> Combo(ComboType.PAIR, sorted)
                    3 -> Combo(ComboType.TRIPLE, sorted)
                    4 -> Combo(ComboType.QUAD, sorted)
                    else -> null
                }
            }
            if (ranks.any { it == TlOrder.TWO_INDEX }) return null
            val distinct = ranks.distinct()
            if (n >= 3 && distinct.size == n && isConsecutive(distinct)) {
                return Combo(ComboType.STRAIGHT, sorted)
            }
            if (n >= 6 && n % 2 == 0 && distinct.size == n / 2 && isConsecutive(distinct)) {
                val allPairs = distinct.all { r -> ranks.count { it == r } == 2 }
                if (allPairs) return Combo(ComboType.PAIR_SEQUENCE, sorted)
            }
            return null
        }

        private fun isConsecutive(sortedDistinct: List<Int>): Boolean =
            sortedDistinct.zipWithNext().all { (a, b) -> b == a + 1 }
    }
}
