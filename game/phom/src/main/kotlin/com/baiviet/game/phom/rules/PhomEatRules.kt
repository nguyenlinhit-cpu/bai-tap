package com.baiviet.game.phom.rules

import com.baiviet.core.cards.Card

/**
 * Quy tắc ăn bài và kiểm tra các ràng buộc:
 * 1. Mỗi phỏm chứa tối đa 1 lá ăn (không ăn 2 lá vào 1 phỏm).
 * 2. Không được đánh lá đã ăn.
 * 3. Không được đánh lá làm vỡ phỏm chứa lá đã ăn (lá bị trói).
 * 4. Mọi lá đã ăn phải nằm trong các phỏm rời nhau khi hạ.
 */
object PhomEatRules {

    /**
     * Tìm tất cả các phỏm mà [incoming] có thể tạo cùng với bài trên [hand].
     * Mỗi phỏm tạo ra KHÔNG được chứa bất kỳ lá nào trong [existingEatenCards].
     */
    fun findPossibleEatMelds(
        hand: List<Card>,
        existingEatenCards: Set<Card>,
        incoming: Card,
    ): List<PhomMeld> {
        val nonEatenHand = hand.filter { it !in existingEatenCards }
        val melds = mutableListOf<PhomMeld>()

        // 1. Ghép phỏm ngang (2 hoặc 3 lá cùng rank trong tay + incoming)
        val sameRankCards = nonEatenHand.filter { it.rank == incoming.rank }
        if (sameRankCards.size >= 2) {
            // Lấy các tổ hợp 2 lá từ sameRankCards
            for (i in 0 until sameRankCards.size) {
                for (j in i + 1 until sameRankCards.size) {
                    val combo = listOf(sameRankCards[i], sameRankCards[j], incoming)
                    PhomMeld.classify(combo, incoming)?.let { melds.add(it) }
                }
            }
            if (sameRankCards.size >= 3) {
                // Lấy 3 lá từ sameRankCards
                for (i in 0 until sameRankCards.size) {
                    for (j in i + 1 until sameRankCards.size) {
                        for (k in j + 1 until sameRankCards.size) {
                            val combo = listOf(sameRankCards[i], sameRankCards[j], sameRankCards[k], incoming)
                            PhomMeld.classify(combo, incoming)?.let { melds.add(it) }
                        }
                    }
                }
            }
        }

        // 2. Ghép phỏm dọc (cùng suit, các lá liên tiếp)
        val sameSuitCards = nonEatenHand.filter { it.suit == incoming.suit }
        val incIdx = incoming.phomIndex

        // incoming là lá giữa: [incIdx - 1, incoming, incIdx + 1]
        val prev1 = sameSuitCards.firstOrNull { it.phomIndex == incIdx - 1 }
        val next1 = sameSuitCards.firstOrNull { it.phomIndex == incIdx + 1 }
        if (prev1 != null && next1 != null) {
            val combo = listOf(prev1, incoming, next1)
            PhomMeld.classify(combo, incoming)?.let { melds.add(it) }
        }

        // incoming là lá cuối: [incIdx - 2, incIdx - 1, incoming]
        val prev2 = sameSuitCards.firstOrNull { it.phomIndex == incIdx - 2 }
        if (prev2 != null && prev1 != null) {
            val combo = listOf(prev2, prev1, incoming)
            PhomMeld.classify(combo, incoming)?.let { melds.add(it) }
        }

        // incoming là lá đầu: [incoming, incIdx + 1, incIdx + 2]
        val next2 = sameSuitCards.firstOrNull { it.phomIndex == incIdx + 2 }
        if (next1 != null && next2 != null) {
            val combo = listOf(incoming, next1, next2)
            PhomMeld.classify(combo, incoming)?.let { melds.add(it) }
        }

        // Sảnh 4 lá trở lên có thể chứa incoming
        for (startIdx in maxOf(0, incIdx - 3)..minOf(10, incIdx)) {
            val neededIndices = (startIdx..startIdx + 3).toList()
            if (incIdx in neededIndices) {
                val otherIndices = neededIndices - incIdx
                val matchedCards = otherIndices.mapNotNull { idx -> sameSuitCards.firstOrNull { it.phomIndex == idx } }
                if (matchedCards.size == 3) {
                    val combo = (matchedCards + incoming).sortedBy { it.phomIndex }
                    PhomMeld.classify(combo, incoming)?.let { if (it !in melds) melds.add(it) }
                }
            }
        }

        // Lọc các phỏm sao cho sau khi ăn, tất cả lá đã ăn (cũ + mới) vẫn có thể xếp thành các phỏm rời nhau
        val newEatenCards = existingEatenCards + incoming
        val fullHand = hand + incoming
        return melds.filter { candidateMeld ->
            canSupportAllEaten(fullHand, newEatenCards, forcedMelds = listOf(candidateMeld))
        }
    }

    /**
     * Kiểm tra xem [hand] có thể tạo các phỏm rời nhau chứa đầy đủ từng lá trong [eatenCards] hay không.
     * Mỗi lá ăn phải nằm trong đúng 1 phỏm riêng biệt (không ăn 2 lá vào cùng 1 phỏm).
     */
    fun canSupportAllEaten(
        hand: List<Card>,
        eatenCards: Set<Card>,
        forcedMelds: List<PhomMeld> = emptyList(),
    ): Boolean {
        if (eatenCards.isEmpty()) return true
        val allValidMelds = PhomMeldFinder.findAllRawMelds(hand)

        fun backtrack(remainingEaten: List<Card>, usedCards: Set<Card>): Boolean {
            if (remainingEaten.isEmpty()) return true
            val targetEaten = remainingEaten.first()
            val candidateMelds = allValidMelds.filter {
                targetEaten in it.cards && it.cards.none { c -> c in usedCards }
            }
            for (meld in candidateMelds) {
                if (backtrack(remainingEaten.drop(1), usedCards + meld.cards)) {
                    return true
                }
            }
            return false
        }

        val forcedUsedCards = forcedMelds.flatMap { it.cards }.toSet()
        val remainingEaten = (eatenCards - forcedMelds.mapNotNull { it.eatenCard }.toSet()).toList()
        return backtrack(remainingEaten, forcedUsedCards)
    }

    /**
     * Kiểm tra xem lá [cardToDiscard] có bị "trói" hoặc là lá đã ăn không được đánh hay không.
     */
    fun isCardLocked(
        hand: List<Card>,
        eatenCards: Set<Card>,
        cardToDiscard: Card,
    ): Boolean {
        // 1. Lá đã ăn tuyệt đối không được đánh
        if (cardToDiscard in eatenCards) return true
        if (eatenCards.isEmpty()) return false

        // 2. Nếu đánh lá này đi, số lá còn lại phải hỗ trợ được toàn bộ các lá đã ăn trong các phỏm rời
        val remainingHand = hand - cardToDiscard
        return !canSupportAllEaten(remainingHand, eatenCards)
    }

    /**
     * Danh sách các lá trong [hand] có thể đánh hợp lệ (không bị trói, không phải lá ăn).
     */
    fun legalDiscards(
        hand: List<Card>,
        eatenCards: Set<Card>,
        exposedMelds: List<PhomMeld> = emptyList(),
    ): List<Card> {
        val meldedCards = exposedMelds.flatMap { it.cards }.toSet()
        val unmeldedHand = hand.filter { it !in meldedCards }
        val candidates = if (unmeldedHand.isNotEmpty()) unmeldedHand else hand
        val nonEaten = candidates.filter { it !in eatenCards }
        val pool = if (nonEaten.isNotEmpty()) nonEaten else candidates
        val unlocked = pool.filter { !isCardLocked(hand, eatenCards, it) }
        return if (unlocked.isNotEmpty()) unlocked else pool
    }
}
