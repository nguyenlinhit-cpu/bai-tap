package com.baiviet.game.poker.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Suit

object PokerEvaluator {

    /**
     * Đánh giá và trả về 5 lá tốt nhất từ danh sách bài (2–7 lá).
     */
    fun evaluate(cards: List<Card>): PokerHand {
        require(cards.isNotEmpty()) { "Bài không được rỗng" }

        if (cards.size < 5) {
            return evaluateShortHand(cards)
        }

        val all5Combos = combinations(cards, 5)
        return all5Combos.map { evaluate5Cards(it) }.maxOrNull()
            ?: evaluate5Cards(cards.take(5))
    }

    /**
     * Đánh giá chính xác bộ đúng 5 lá bài.
     */
    fun evaluate5Cards(cards: List<Card>): PokerHand {
        require(cards.size == 5) { "Phải có đúng 5 lá để đánh giá" }

        val sortedCards = cards.sortedByDescending { PokerHand.rankToValue(it.rank) }
        val ranks = sortedCards.map { PokerHand.rankToValue(it.rank) }
        val isFlush = sortedCards.all { it.suit == sortedCards[0].suit }

        // Kiểm tra sảnh
        val isWheel = ranks == listOf(14, 5, 4, 3, 2)
        val isNormalStraight = ranks.distinct().size == 5 && (ranks[0] - ranks[4] == 4)
        val isStraight = isNormalStraight || isWheel
        val straightHigh = if (isWheel) 5 else ranks[0]

        // 1. Thùng phá sảnh lớn (Royal Flush)
        if (isFlush && isStraight && straightHigh == 14 && !isWheel) {
            return PokerHand(
                type = PokerHandType.ROYAL_FLUSH,
                tieBreakers = listOf(14),
                best5Cards = sortedCards,
                description = "Thùng phá sảnh lớn",
            )
        }

        // 2. Thùng phá sảnh (Straight Flush)
        if (isFlush && isStraight) {
            return PokerHand(
                type = PokerHandType.STRAIGHT_FLUSH,
                tieBreakers = listOf(straightHigh),
                best5Cards = sortedCards,
                description = "Thùng phá sảnh ${PokerHand.valueToRankName(straightHigh)}",
            )
        }

        // Nhóm theo rank
        val groups = sortedCards.groupBy { PokerHand.rankToValue(it.rank) }
            .toList()
            .sortedWith(
                compareByDescending<Pair<Int, List<Card>>> { it.second.size }
                    .thenByDescending { it.first },
            )

        // 3. Tứ quý (Four of a Kind)
        if (groups[0].second.size == 4) {
            val quad = groups[0].first
            val kicker = groups[1].first
            return PokerHand(
                type = PokerHandType.FOUR_OF_A_KIND,
                tieBreakers = listOf(quad, kicker),
                best5Cards = sortedCards,
                description = "Tứ quý ${PokerHand.valueToRankName(quad)}",
            )
        }

        // 4. Cù lũ (Full House)
        if (groups[0].second.size == 3 && groups[1].second.size == 2) {
            val trips = groups[0].first
            val pair = groups[1].first
            return PokerHand(
                type = PokerHandType.FULL_HOUSE,
                tieBreakers = listOf(trips, pair),
                best5Cards = sortedCards,
                description = "Cù lũ ${PokerHand.valueToRankName(trips)} và ${PokerHand.valueToRankName(pair)}",
            )
        }

        // 5. Thùng (Flush)
        if (isFlush) {
            val suitName = vietnameseSuitName(sortedCards[0].suit)
            return PokerHand(
                type = PokerHandType.FLUSH,
                tieBreakers = ranks,
                best5Cards = sortedCards,
                description = "Thùng $suitName (${PokerHand.valueToRankName(ranks[0])} cao)",
            )
        }

        // 6. Sảnh (Straight)
        if (isStraight) {
            return PokerHand(
                type = PokerHandType.STRAIGHT,
                tieBreakers = listOf(straightHigh),
                best5Cards = sortedCards,
                description = "Sảnh tới ${PokerHand.valueToRankName(straightHigh)}",
            )
        }

        // 7. Sám cô (Three of a Kind)
        if (groups[0].second.size == 3) {
            val trips = groups[0].first
            val kickers = listOf(groups[1].first, groups[2].first)
            return PokerHand(
                type = PokerHandType.THREE_OF_A_KIND,
                tieBreakers = listOf(trips) + kickers,
                best5Cards = sortedCards,
                description = "Sám cô ${PokerHand.valueToRankName(trips)}",
            )
        }

        // 8. Thú - 2 đôi (Two Pair)
        if (groups[0].second.size == 2 && groups[1].second.size == 2) {
            val highPair = groups[0].first
            val lowPair = groups[1].first
            val kicker = groups[2].first
            return PokerHand(
                type = PokerHandType.TWO_PAIR,
                tieBreakers = listOf(highPair, lowPair, kicker),
                best5Cards = sortedCards,
                description = "Hai đôi ${PokerHand.valueToRankName(highPair)} và ${PokerHand.valueToRankName(lowPair)}",
            )
        }

        // 9. Đôi (One Pair)
        if (groups[0].second.size == 2) {
            val pair = groups[0].first
            val kickers = listOf(groups[1].first, groups[2].first, groups[3].first)
            return PokerHand(
                type = PokerHandType.ONE_PAIR,
                tieBreakers = listOf(pair) + kickers,
                best5Cards = sortedCards,
                description = "Đôi ${PokerHand.valueToRankName(pair)}",
            )
        }

        // 10. Mậu thầu (High Card)
        return PokerHand(
            type = PokerHandType.HIGH_CARD,
            tieBreakers = ranks,
            best5Cards = sortedCards,
            description = "Mậu thầu ${PokerHand.valueToRankName(ranks[0])}",
        )
    }

    private fun evaluateShortHand(cards: List<Card>): PokerHand {
        val sortedCards = cards.sortedByDescending { PokerHand.rankToValue(it.rank) }
        val ranks = sortedCards.map { PokerHand.rankToValue(it.rank) }
        val groups = sortedCards.groupBy { PokerHand.rankToValue(it.rank) }
            .toList()
            .sortedWith(
                compareByDescending<Pair<Int, List<Card>>> { it.second.size }
                    .thenByDescending { it.first },
            )

        if (groups.isNotEmpty() && groups[0].second.size >= 2) {
            val pair = groups[0].first
            val kickers = groups.drop(1).map { it.first }
            return PokerHand(
                type = PokerHandType.ONE_PAIR,
                tieBreakers = listOf(pair) + kickers,
                best5Cards = sortedCards,
                description = "Đôi ${PokerHand.valueToRankName(pair)}",
            )
        }

        return PokerHand(
            type = PokerHandType.HIGH_CARD,
            tieBreakers = ranks,
            best5Cards = sortedCards,
            description = "Mậu thầu ${PokerHand.valueToRankName(ranks[0])}",
        )
    }

    private fun vietnameseSuitName(suit: Suit): String = when (suit) {
        Suit.DIAMOND -> "rô"
        Suit.HEART -> "cơ"
        Suit.CLUB -> "chuồn"
        Suit.SPADE -> "bích"
    }

    private fun <T> combinations(list: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (list.isEmpty() || list.size < k) return emptyList()
        val head = list[0]
        val tail = list.subList(1, list.size)
        val withHead = combinations(tail, k - 1).map { listOf(head) + it }
        val withoutHead = combinations(tail, k)
        return withHead + withoutHead
    }
}
