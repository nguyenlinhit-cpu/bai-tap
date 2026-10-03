package com.baiviet.game.phom.bot

import com.baiviet.core.cards.Card
import com.baiviet.game.phom.rules.PhomMeldFinder
import com.baiviet.game.phom.rules.phomPoint

/**
 * Phân tích bài và đánh giá mức độ nguy hiểm của các lá rác trong Phỏm.
 */
object PhomHandAnalyzer {

    /**
     * Sắp xếp các lá rác từ khuyên đánh nhất đến ít khuyên đánh nhất.
     * Tiêu chí:
     * 1. Không nằm trong bất kỳ cạ nào.
     * 2. Điểm số cao hơn (K, Q, J) ưu tiên xả sớm để giảm điểm rác.
     * 3. An toàn: lá bài đã xuất hiện trên bàn (ở các đống rác) có xác suất bị ăn thấp hơn.
     */
    fun rankDiscardCandidates(
        hand: List<Card>,
        legalDiscards: List<Card>,
        allDiscardsOnTable: List<Card>,
    ): List<Card> {
        val caList = PhomMeldFinder.findAllCa(hand)
        val cardsInCa = caList.flatMap { listOf(it.cards.first, it.cards.second) }.toSet()

        return legalDiscards.sortedWith(
            compareBy<Card> { card ->
                // Ưu tiên 1: lá KHÔNG nằm trong cạ
                if (card in cardsInCa) 1 else 0
            }.thenByDescending { card ->
                // Ưu tiên 2: lá an toàn (đã có lá cùng rank hoặc cùng suit xuất hiện trên bàn)
                val sameRankCount = allDiscardsOnTable.count { it.rank == card.rank }
                sameRankCount
            }.thenByDescending { card ->
                // Ưu tiên 3: điểm số cao hơn (xả điểm)
                card.phomPoint
            },
        )
    }
}
