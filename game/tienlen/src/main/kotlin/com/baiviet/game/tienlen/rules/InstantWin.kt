package com.baiviet.game.tienlen.rules

import com.baiviet.core.cards.Card
import kotlinx.serialization.Serializable

/** Các loại tới trắng, theo thứ tự ưu tiên (khai báo trước = mạnh hơn). */
@Serializable
enum class InstantWinType(val viName: String) {
    DRAGON("Sảnh rồng"),
    FOUR_TWOS("Tứ quý heo"),
    FIVE_PAIR_SEQUENCE("5 đôi thông"),
    SIX_PAIRS("6 đôi"),
    SAME_COLOR("Đồng màu"),
    FOUR_THREES("Tứ quý 3"),
}

/**
 * Kiểm tra tới trắng ngay sau khi chia.
 *
 * Quyết định luật:
 * - "6 đôi": đếm số đôi rời nhau, tứ quý tính 2 đôi, sám tính 1 đôi, đôi heo cũng tính.
 * - Nhiều người cùng tới trắng: ưu tiên theo loại; cùng loại so lá lớn nhất trên tay.
 */
object InstantWin {

    fun detect(hand: List<Card>, rules: TienLenRules, isFirstGame: Boolean): InstantWinType? {
        val counts = IntArray(13)
        hand.forEach { counts[it.tlRank]++ }
        return when {
            (0..11).all { counts[it] >= 1 } -> InstantWinType.DRAGON
            counts[TlOrder.TWO_INDEX] == 4 -> InstantWinType.FOUR_TWOS
            rules.fiveConsecutivePairsInstantWin && hasPairRun(counts, 5) -> InstantWinType.FIVE_PAIR_SEQUENCE
            counts.sumOf { it / 2 } >= 6 -> InstantWinType.SIX_PAIRS
            sameColorCount(hand) >= rules.sameColorInstantWinCount -> InstantWinType.SAME_COLOR
            isFirstGame && rules.quadThreeInstantWinFirstGame && counts[0] == 4 -> InstantWinType.FOUR_THREES
            else -> null
        }
    }

    private fun hasPairRun(counts: IntArray, length: Int): Boolean {
        var run = 0
        for (r in 0..11) {
            run = if (counts[r] >= 2) run + 1 else 0
            if (run >= length) return true
        }
        return false
    }

    private fun sameColorCount(hand: List<Card>): Int {
        val red = hand.count { it.suit.isRed }
        return maxOf(red, hand.size - red)
    }

    /**
     * Chọn người tới trắng thắng cuộc trong số [candidates] (seat → loại).
     */
    fun resolve(candidates: Map<Int, InstantWinType>, hands: List<List<Card>>): Int? =
        candidates.entries
            .sortedWith(
                compareBy<Map.Entry<Int, InstantWinType>> { it.value.ordinal }
                    .thenByDescending { e -> hands[e.key].maxOf { it.tlValue } },
            )
            .firstOrNull()?.key
}
