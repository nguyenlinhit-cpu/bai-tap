package com.baiviet.game.bacay.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit

object BaCayEvaluator {

    fun evaluate(cards: List<Card>, sapBeatsBaTien: Boolean = false): BaCayHand {
        require(cards.size == 3) { "Ba Cây yêu cầu đúng 3 lá bài, nhận được ${cards.size}" }

        val isSap = cards[0].rank == cards[1].rank && cards[1].rank == cards[2].rank
        val isBaTien = cards.all { isFaceCard(it.rank) }

        val type = when {
            sapBeatsBaTien -> when {
                isSap -> BaCayHandType.SAP
                isBaTien -> BaCayHandType.BA_TIEN
                else -> BaCayHandType.POINTS
            }
            else -> when {
                isBaTien -> BaCayHandType.BA_TIEN
                isSap -> BaCayHandType.SAP
                else -> BaCayHandType.POINTS
            }
        }

        val points = calculatePoints(cards)
        val strongest = cards.maxByOrNull { cardWeight(it) } ?: cards.first()

        return BaCayHand(
            cards = cards,
            type = type,
            points = points,
            strongestCard = strongest,
        )
    }

    /**
     * So sánh hai tay bài Ba Cây.
     *
     * @return 1 nếu A > B, -1 nếu A < B, 0 nếu Hòa
     */
    fun compare(handA: BaCayHand, handB: BaCayHand, rules: BaCayRules): Int {
        if (handA.type != handB.type) {
            return if (rules.sapBeatsBaTien) {
                // Thứ tự: SAP > BA_TIEN > POINTS
                val weightA = when (handA.type) {
                    BaCayHandType.SAP -> 3
                    BaCayHandType.BA_TIEN -> 2
                    BaCayHandType.POINTS -> 1
                }
                val weightB = when (handB.type) {
                    BaCayHandType.SAP -> 3
                    BaCayHandType.BA_TIEN -> 2
                    BaCayHandType.POINTS -> 1
                }
                weightA.compareTo(weightB)
            } else {
                // Thứ tự chuẩn: BA_TIEN > SAP > POINTS
                val weightA = when (handA.type) {
                    BaCayHandType.BA_TIEN -> 3
                    BaCayHandType.SAP -> 2
                    BaCayHandType.POINTS -> 1
                }
                val weightB = when (handB.type) {
                    BaCayHandType.BA_TIEN -> 3
                    BaCayHandType.SAP -> 2
                    BaCayHandType.POINTS -> 1
                }
                weightA.compareTo(weightB)
            }
        }

        // Cùng loại bài:
        return when (handA.type) {
            BaCayHandType.SAP -> {
                val rankA = rankPriority(handA.cards.first().rank)
                val rankB = rankPriority(handB.cards.first().rank)
                rankA.compareTo(rankB)
            }
            BaCayHandType.BA_TIEN -> {
                compareCards(handA.strongestCard, handB.strongestCard)
            }
            BaCayHandType.POINTS -> {
                if (handA.points != handB.points) {
                    handA.points.compareTo(handB.points)
                } else {
                    if (rules.tieIsPush) 0 else compareCards(handA.strongestCard, handB.strongestCard)
                }
            }
        }
    }

    fun payoutMultiplier(winningHand: BaCayHand, rules: BaCayRules): Int =
        if (winningHand.type == BaCayHandType.BA_TIEN && rules.baTienPaysDouble) 2 else 1

    /**
     * So sánh 2 lá bài riêng lẻ theo chuẩn Ba Cây / Cào:
     * Chất trước: Rô > Cơ > Chuồn > Bích. Cùng chất so số: A > K > ... > 2.
     */
    fun compareCards(a: Card, b: Card): Int {
        val suitA = suitPriority(a.suit)
        val suitB = suitPriority(b.suit)
        if (suitA != suitB) return suitA.compareTo(suitB)
        return rankPriority(a.rank).compareTo(rankPriority(b.rank))
    }

    private fun isFaceCard(rank: Rank): Boolean =
        rank == Rank.JACK || rank == Rank.QUEEN || rank == Rank.KING

    private fun calculatePoints(cards: List<Card>): Int {
        val total = cards.sumOf { cardValue(it.rank) }
        return total % 10
    }

    private fun cardValue(rank: Rank): Int = when (rank) {
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

    /**
     * Độ ưu tiên chất trong Ba Cây:
     * ♦ Rô (3) > ♥ Cơ (2) > ♣ Chuồn (1) > ♠ Bích (0)
     */
    private fun suitPriority(suit: Suit): Int = when (suit) {
        Suit.DIAMOND -> 3
        Suit.HEART -> 2
        Suit.CLUB -> 1
        Suit.SPADE -> 0
    }

    /**
     * Độ ưu tiên số lá bài trong so bài Ba Cây:
     * A (14) > K (13) > Q (12) > J (11) > 10 (10) > ... > 2 (2)
     */
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

    private fun cardWeight(card: Card): Int =
        suitPriority(card.suit) * 100 + rankPriority(card.rank)
}
