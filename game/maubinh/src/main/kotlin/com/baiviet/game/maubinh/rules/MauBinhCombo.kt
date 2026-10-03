package com.baiviet.game.maubinh.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import kotlinx.serialization.Serializable

/**
 * Thứ bậc lá bài trong Mậu Binh: 2 (thấp nhất) < 3 < ... < K < A (cao nhất).
 * Không so chất trong bất kỳ tình huống so chi nào.
 */
object MauBinhOrder {
    fun rankIndex(rank: Rank): Int = when (rank) {
        Rank.TWO -> 0
        Rank.THREE -> 1
        Rank.FOUR -> 2
        Rank.FIVE -> 3
        Rank.SIX -> 4
        Rank.SEVEN -> 5
        Rank.EIGHT -> 6
        Rank.NINE -> 7
        Rank.TEN -> 8
        Rank.JACK -> 9
        Rank.QUEEN -> 10
        Rank.KING -> 11
        Rank.ACE -> 12
    }

    val cardComparator: Comparator<Card> = compareBy<Card> { rankIndex(it.rank) }
}

val Card.mbRank: Int get() = MauBinhOrder.rankIndex(rank)

/**
 * Loại tổ hợp bài theo thứ tự mạnh -> yếu.
 */
@Serializable
enum class MauBinhComboType(val strength: Int, val viName: String) {
    HIGH_CARD(1, "Mậu thầu"),
    ONE_PAIR(2, "Đôi"),
    TWO_PAIR(3, "Thú"),
    THREE_OF_A_KIND(4, "Sám cô"),
    STRAIGHT(5, "Sảnh"),
    FLUSH(6, "Thùng"),
    FULL_HOUSE(7, "Cù lũ"),
    FOUR_OF_A_KIND(8, "Tứ quý"),
    STRAIGHT_FLUSH(9, "Thùng phá sảnh"),
}

/**
 * Kết quả phân loại một chi bài (5 lá hoặc 3 lá).
 *
 * @param type Loại tổ hợp
 * @param cards Các lá bài thuộc chi
 * @param tieBreakers Danh sách số đo xếp hạng dùng để phá hòa khi cùng [type] (từ quan trọng nhất đến ít quan trọng nhất)
 */
@Serializable
data class MauBinhChiHand(
    val type: MauBinhComboType,
    val cards: List<Card>,
    val tieBreakers: List<Int>,
    val description: String,
) : Comparable<MauBinhChiHand> {

    override fun compareTo(other: MauBinhChiHand): Int {
        if (this.type.strength != other.type.strength) {
            return this.type.strength.compareTo(other.type.strength)
        }
        val minSize = minOf(this.tieBreakers.size, other.tieBreakers.size)
        for (i in 0 until minSize) {
            val cmp = this.tieBreakers[i].compareTo(other.tieBreakers[i])
            if (cmp != 0) return cmp
        }
        return 0
    }
}

/**
 * Loại tới trắng Mậu Binh (không cần so chi).
 */
@Serializable
enum class MauBinhInstantWinType(val priority: Int, val viName: String) {
    DRAGON_ROLL(6, "Rồng cuốn"),
    DRAGON_STRAIGHT(5, "Sảnh rồng"),
    FIVE_PAIRS_ONE_TRIPLE(4, "Năm đôi một sám"),
    SIX_PAIRS(3, "Lục phé bôn"),
    THREE_FLUSHES(2, "Ba thùng"),
    THREE_STRAIGHTS(1, "Ba sảnh"),
}

/**
 * Cấu trúc 3 chi của một người chơi:
 * - [chi1]: Chi đầu (5 lá)
 * - [chi2]: Chi giữa (5 lá)
 * - [chi3]: Chi cuối (3 lá)
 */
@Serializable
data class MauBinhArrangement(
    val chi1: List<Card>,
    val chi2: List<Card>,
    val chi3: List<Card>,
) {
    init {
        require(chi1.size == 5) { "Chi 1 phải đủ 5 lá (hiện có ${chi1.size})" }
        require(chi2.size == 5) { "Chi 2 phải đủ 5 lá (hiện có ${chi2.size})" }
        require(chi3.size == 3) { "Chi 3 phải đủ 3 lá (hiện có ${chi3.size})" }
        val all = chi1 + chi2 + chi3
        require(all.distinct().size == 13) { "3 chi phải chứa đủ 13 lá không trùng lặp" }
    }

    fun isFoul(rules: MauBinhRules = MauBinhRules.DEFAULT): Boolean {
        val h1 = MauBinhEvaluator.evaluateChi1OrChi2(chi1, rules)
        val h2 = MauBinhEvaluator.evaluateChi1OrChi2(chi2, rules)
        val h3 = MauBinhEvaluator.evaluateChi3(chi3)

        // Quy tắc: Chi 1 >= Chi 2 >= Chi 3
        if (h1 < h2) return true
        if (compareChi2WithChi3(h2, h3) < 0) return true
        return false
    }

    companion object {
        /**
         * So sánh Chi 2 (5 lá) với Chi 3 (3 lá) để xác định binh lủng.
         * Quy tắc:
         * - Chi 3 chỉ tối đa là Sám cô (THREE_OF_A_KIND), Đôi (ONE_PAIR), Mậu thầu (HIGH_CARD).
         * - Nếu Chi 2 có type > THREE_OF_A_KIND (Thùng phá sảnh, Tứ quý, Cù lũ, Thùng, Sảnh, Thú) -> Chi 2 > Chi 3.
         * - Nếu Chi 2 và Chi 3 cùng là Sám cô: so rank của bộ ba.
         * - Nếu Chi 2 là Sám cô và Chi 3 là Đôi/Mậu thầu -> Chi 2 > Chi 3.
         * - Nếu Chi 2 và Chi 3 cùng là Một đôi: so rank của đôi trước, nếu bằng nhau thì so các lá phụ (kicker).
         * - Nếu Chi 2 là Một đôi và Chi 3 là Mậu thầu -> Chi 2 > Chi 3.
         * - Nếu Chi 2 và Chi 3 cùng là Mậu thầu: so lần lượt từ lá cao nhất xuống.
         */
        fun compareChi2WithChi3(chi2Hand: MauBinhChiHand, chi3Hand: MauBinhChiHand): Int {
            if (chi2Hand.type.strength > chi3Hand.type.strength) return 1
            if (chi2Hand.type.strength < chi3Hand.type.strength) return -1

            // Cùng loại (chỉ có thể là Sám cô, Một đôi hoặc Mậu thầu)
            val minKickers = minOf(chi2Hand.tieBreakers.size, chi3Hand.tieBreakers.size)
            for (i in 0 until minKickers) {
                val cmp = chi2Hand.tieBreakers[i].compareTo(chi3Hand.tieBreakers[i])
                if (cmp != 0) return cmp
            }
            return 0
        }
    }
}
