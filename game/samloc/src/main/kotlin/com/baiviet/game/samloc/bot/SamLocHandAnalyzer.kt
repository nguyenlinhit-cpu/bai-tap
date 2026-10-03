package com.baiviet.game.samloc.bot

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.game.samloc.rules.SamLocCombo
import com.baiviet.game.samloc.rules.SamLocComboFinder
import com.baiviet.game.samloc.rules.SamLocComboType
import com.baiviet.game.samloc.rules.SlOrder
import com.baiviet.game.samloc.rules.isTwo
import com.baiviet.game.samloc.rules.slRank
import com.baiviet.game.samloc.rules.sortedSl

/**
 * Phân tích cấu trúc tay bài trong Sâm Lốc để bot ra quyết định.
 */
object SamLocHandAnalyzer {
    /**
     * Tách tay bài thành tập các bộ rời nhau có số lượng bộ ít nhất.
     */
    fun decompose(hand: List<Card>): List<SamLocCombo> {
        if (hand.isEmpty()) return emptyList()
        val sorted = hand.sortedSl()
        var bestPartition: List<SamLocCombo>? = null

        fun search(
            remaining: List<Card>,
            currentCombos: List<SamLocCombo>,
        ) {
            if (remaining.isEmpty()) {
                if (bestPartition == null || currentCombos.size < bestPartition!!.size) {
                    bestPartition = currentCombos
                }
                return
            }
            if (bestPartition != null && currentCombos.size >= bestPartition!!.size) {
                return
            }

            // Thử sảnh dài nhất có thể trước
            val allCombos =
                SamLocComboFinder
                    .all(remaining)
                    .sortedWith(
                        compareByDescending<SamLocCombo> { it.size }
                            .thenByDescending { it.top.slRank },
                    )

            var triedAny = false
            for (combo in allCombos) {
                if (combo.type != SamLocComboType.SINGLE) {
                    val nextRemaining = remaining - combo.cards.toSet()
                    search(nextRemaining, currentCombos + combo)
                    triedAny = true
                    if (bestPartition != null && bestPartition!!.size <= currentCombos.size + 1) break
                }
            }

            if (!triedAny) {
                // Toàn bộ bài rác lẻ
                val singles = remaining.map { SamLocCombo(SamLocComboType.SINGLE, listOf(it)) }
                val full = currentCombos + singles
                if (bestPartition == null || full.size < bestPartition!!.size) {
                    bestPartition = full
                }
            }
        }

        search(sorted, emptyList())
        return bestPartition ?: sorted.map { SamLocCombo(SamLocComboType.SINGLE, listOf(it)) }
    }

    /**
     * Đánh giá xem bài có đủ mạnh để Báo Sâm hay không.
     * Bài đẹp: số lượng bộ ít (<= 2 bộ hoặc 3 bộ có tứ quý/heo/A), không có rác nhỏ khó thoát.
     */
    fun shouldCallSam(hand: List<Card>): Boolean {
        if (hand.size != 10) return false
        val groups = decompose(hand)
        if (groups.size <= 2) {
            // 1 hoặc 2 bộ (ví dụ: sảnh 8 lá + đôi, hoặc 2 sảnh 5 lá) -> cực kỳ mạnh!
            val singles = groups.filter { it.type == SamLocComboType.SINGLE }
            if (singles.isEmpty()) return true
            // Nếu có rác thì rác phải là heo hoặc Át
            if (singles.all { it.hasTwo || it.top.rank == Rank.ACE }) return true
        }

        if (groups.size == 3) {
            val hasQuad = groups.any { it.isQuad }
            val hasTwo = hand.any { it.isTwo }
            val singles = groups.filter { it.type == SamLocComboType.SINGLE }
            if (hasQuad && hasTwo && singles.size <= 1 && (singles.isEmpty() || singles.first().top.slRank >= 10)) {
                return true
            }
        }
        return false
    }

    /**
     * Tìm lá rác lớn nhất có thể dùng để giữ cửa khi đối thủ kế tiếp Báo 1.
     */
    fun findHighestSingle(hand: List<Card>): Card? = hand.maxWithOrNull(SlOrder.handCardComparator)
}
