package com.baiviet.game.samloc.rules

import com.baiviet.core.cards.Card
import kotlinx.serialization.Serializable
import kotlin.comparisons.nullsFirst

/**
 * Các loại ăn trắng trong Sâm Lốc theo thứ tự ưu tiên (khai báo trước = mạnh hơn).
 */
@Serializable
enum class SamLocInstantWinType(
    val viName: String,
) {
    FOUR_TWOS("Tứ quý heo"),
    DRAGON("Sảnh rồng"),
    FIVE_PAIRS("5 đôi"),
    SAME_COLOR("Đồng màu"),
    THREE_TRIPLES("3 sám"),
}

/**
 * Kiểm tra ăn trắng ngay sau khi chia bài trong Sâm Lốc (mỗi người 10 lá).
 */
object SamLocInstantWin {
    fun detect(
        hand: List<Card>,
        rules: SamLocRules,
    ): SamLocInstantWinType? {
        if (hand.size != 10) return null
        val counts = IntArray(13)
        hand.forEach { counts[it.slRank]++ }

        return when {
            // 1. Tứ quý heo
            counts[SlOrder.TWO_INDEX] == 4 -> SamLocInstantWinType.FOUR_TWOS

            // 2. Sảnh rồng (10 lá liên tiếp)
            SamLocCombo.classify(hand)?.type == SamLocComboType.STRAIGHT -> SamLocInstantWinType.DRAGON

            // 3. 5 đôi
            counts.sumOf { it / 2 } == 5 -> SamLocInstantWinType.FIVE_PAIRS

            // 4. 10 lá đồng màu
            isSameColor(hand) -> SamLocInstantWinType.SAME_COLOR

            // 5. 3 sám (Luật nhà)
            rules.threeTriplesInstantWin && counts.count { it >= 3 } >= 3 -> SamLocInstantWinType.THREE_TRIPLES

            else -> null
        }
    }

    private fun isSameColor(hand: List<Card>): Boolean {
        val red = hand.count { it.suit.isRed }
        return red == 10 || red == 0
    }

    /**
     * Giải quyết người ăn trắng khi có nhiều người cùng thỏa điều kiện.
     * Ưu tiên loại trước; cùng loại so lá lớn nhất trên tay (theo rank Sâm, rồi chất).
     */
    fun resolve(
        candidates: Map<Int, SamLocInstantWinType>,
        hands: List<List<Card>>,
    ): Int? {
        if (candidates.isEmpty()) return null
        val cardComp: Comparator<Card?> = nullsFirst(SlOrder.handCardComparator)
        return candidates.entries
            .minWithOrNull(
                compareBy<Map.Entry<Int, SamLocInstantWinType>> { it.value.ordinal }
                    .thenByDescending(cardComp) { e ->
                        hands[e.key].maxWithOrNull(SlOrder.handCardComparator)
                    },
            )?.key
    }
}
