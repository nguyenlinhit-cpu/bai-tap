package com.baiviet.game.lieng.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit

object LiengEvaluator {

    fun evaluate(cards: List<Card>, rules: LiengRules = LiengRules.DEFAULT): LiengHand {
        require(cards.size == 3) { "Liêng yêu cầu đúng 3 lá bài, nhận được ${cards.size}" }

        val strongest = cards.maxWith { a, b -> compareCards(a, b, rules) }

        // 1. Kiểm tra Sáp
        if (cards[0].rank == cards[1].rank && cards[1].rank == cards[2].rank) {
            return LiengHand(
                cards = cards,
                type = LiengHandType.SAP,
                points = 0,
                highestRank = cards[0].rank,
                strongestCard = strongest,
            )
        }

        // 2. Kiểm tra Liêng (3 lá liên tiếp)
        val liengRankOrder = getLiengRankOrder(cards.map { it.rank })
        if (liengRankOrder != null) {
            return LiengHand(
                cards = cards,
                type = LiengHandType.LIENG,
                points = 0,
                highestRank = liengRankOrder.first,
                strongestCard = strongest,
            )
        }

        // 3. Kiểm tra Ảnh (3 lá đều là J, Q, K)
        if (cards.all { isFaceCard(it.rank) }) {
            return LiengHand(
                cards = cards,
                type = LiengHandType.ANH,
                points = 0,
                highestRank = Rank.KING,
                strongestCard = strongest,
            )
        }

        // 4. Tính điểm
        val points = calculatePoints(cards)
        return LiengHand(
            cards = cards,
            type = LiengHandType.DIEM,
            points = points,
            highestRank = strongest.rank,
            strongestCard = strongest,
        )
    }

    fun compare(handA: LiengHand, handB: LiengHand, rules: LiengRules): Int {
        if (handA.type != handB.type) {
            return handA.type.ordinal.compareTo(handB.type.ordinal)
        }

        return when (handA.type) {
            LiengHandType.SAP -> {
                val rankA = rankPriority(handA.highestRank)
                val rankB = rankPriority(handB.highestRank)
                rankA.compareTo(rankB)
            }
            LiengHandType.LIENG -> {
                val orderA = getLiengSequenceOrder(handA.cards.map { it.rank }) ?: 0
                val orderB = getLiengSequenceOrder(handB.cards.map { it.rank }) ?: 0
                if (orderA != orderB) {
                    orderA.compareTo(orderB)
                } else {
                    if (rules.liengTieIsPush) 0 else compareCards(handA.strongestCard, handB.strongestCard, rules)
                }
            }
            LiengHandType.ANH -> {
                compareCards(handA.strongestCard, handB.strongestCard, rules)
            }
            LiengHandType.DIEM -> {
                if (handA.points != handB.points) {
                    handA.points.compareTo(handB.points)
                } else {
                    compareCards(handA.strongestCard, handB.strongestCard, rules)
                }
            }
        }
    }

    /**
     * So sánh 2 lá bài riêng lẻ trong Liêng theo quy định của rules:
     * - Mặc định: So chất trước (Rô > Cơ > Chuồn > Bích), cùng chất so số (A > K > ... > 2).
     * - Luật nhà có thể đổi chất (Cơ > Rô) hoặc so số trước chất.
     */
    fun compareCards(a: Card, b: Card, rules: LiengRules): Int {
        val suitA = suitPriority(a.suit, rules.suitOrderCoOverRo)
        val suitB = suitPriority(b.suit, rules.suitOrderCoOverRo)
        val rankA = rankPriority(a.rank)
        val rankB = rankPriority(b.rank)

        return if (rules.rankBeforeSuit) {
            if (rankA != rankB) rankA.compareTo(rankB) else suitA.compareTo(suitB)
        } else {
            if (suitA != suitB) suitA.compareTo(suitB) else rankA.compareTo(rankB)
        }
    }

    private fun isFaceCard(rank: Rank): Boolean =
        rank == Rank.JACK || rank == Rank.QUEEN || rank == Rank.KING

    private fun cardPoint(rank: Rank): Int = when (rank) {
        Rank.ACE -> 1
        Rank.TWO -> 2
        Rank.THREE -> 3
        Rank.FOUR -> 4
        Rank.FIVE -> 5
        Rank.SIX -> 6
        Rank.SEVEN -> 7
        Rank.EIGHT -> 8
        Rank.NINE -> 9
        Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING -> 0
    }

    private fun calculatePoints(cards: List<Card>): Int {
        val sum = cards.sumOf { cardPoint(it.rank) }
        return sum % 10
    }

    /**
     * Thứ tự chuỗi Liêng (1 đến 12):
     * A-2-3 (1), 2-3-4 (2), ..., 10-J-Q (10), J-Q-K (11), Q-K-A (12).
     * K-A-2 không hợp lệ.
     */
    private fun getLiengSequenceOrder(ranks: List<Rank>): Int? {
        val sorted = ranks.map { rankPriority(it) }.sorted()
        // Kiểm tra A-2-3 đặc biệt: A (14), 2 (2), 3 (3)
        if (sorted == listOf(2, 3, 14)) return 1

        // Kiểm tra liên tiếp thông thường
        if (sorted[1] == sorted[0] + 1 && sorted[2] == sorted[1] + 1) {
            // sorted[2] từ 4 (2-3-4 -> 2) đến 14 (Q-K-A -> 12)
            return sorted[2] - 2
        }
        return null
    }

    private fun getLiengRankOrder(ranks: List<Rank>): Pair<Rank, Int>? {
        val order = getLiengSequenceOrder(ranks) ?: return null
        val sortedByRank = ranks.sortedBy { rankPriority(it) }
        return sortedByRank.last() to order
    }

    private fun suitPriority(suit: Suit, coOverRo: Boolean): Int = when (suit) {
        Suit.HEART -> if (coOverRo) 3 else 2
        Suit.DIAMOND -> if (coOverRo) 2 else 3
        Suit.CLUB -> 1
        Suit.SPADE -> 0
    }

    private fun rankPriority(rank: Rank): Int = when (rank) {
        Rank.ACE -> 14
        Rank.KING -> 13
        Rank.QUEEN -> 12
        Rank.JACK -> 11
        Rank.TEN -> 10
        Rank.NINE -> 9
        Rank.EIGHT -> 8
        Rank.SEVEN -> 7
        Rank.SIX -> 6
        Rank.FIVE -> 5
        Rank.FOUR -> 4
        Rank.THREE -> 3
        Rank.TWO -> 2
    }
}
