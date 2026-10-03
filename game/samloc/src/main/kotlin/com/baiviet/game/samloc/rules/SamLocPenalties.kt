package com.baiviet.game.samloc.rules

import com.baiviet.core.cards.Card
import kotlinx.serialization.Serializable

/** Loại heo/tứ quý bị thối hoặc bị chặt trong Sâm Lốc. */
@Serializable
enum class SamLocPenaltyKind(
    val viName: String,
) {
    TWO("heo"),
    BLACK_TWO("heo đen"),
    RED_TWO("heo đỏ"),
    QUAD("tứ quý"),
}

@Serializable
data class SamLocPenaltyItem(
    val kind: SamLocPenaltyKind,
    val cards: List<Card>,
)

data class SamLocHandPenalty(
    val items: List<SamLocPenaltyItem>,
    val normalCards: List<Card>,
)

object SamLocPenalties {
    fun kindOfTwo(
        card: Card,
        rules: SamLocRules,
    ): SamLocPenaltyKind =
        if (rules.differentiateTwoColors) {
            if (card.suit.isRed) SamLocPenaltyKind.RED_TWO else SamLocPenaltyKind.BLACK_TWO
        } else {
            SamLocPenaltyKind.TWO
        }

    fun cardsForPenalty(
        kind: SamLocPenaltyKind,
        rules: SamLocRules,
    ): Int =
        when (kind) {
            SamLocPenaltyKind.TWO -> rules.defaultTwoCards
            SamLocPenaltyKind.BLACK_TWO -> rules.blackTwoCards
            SamLocPenaltyKind.RED_TWO -> rules.redTwoCards
            SamLocPenaltyKind.QUAD -> rules.quadPenaltyCards
        }

    /** Phân tích tay bài còn lại khi kết thúc ván để tính thối heo và thối tứ quý. */
    fun analyze(
        hand: List<Card>,
        rules: SamLocRules,
    ): SamLocHandPenalty {
        val items = mutableListOf<SamLocPenaltyItem>()
        val rest = hand.sortedSl().toMutableList()

        // Tứ quý
        val quads = rest.groupBy { it.slRank }.filterValues { it.size == 4 }.values
        for (quad in quads) {
            items += SamLocPenaltyItem(SamLocPenaltyKind.QUAD, quad)
            rest.removeAll(quad)
        }

        // Heo lẻ (nếu còn)
        val twos = rest.filter { it.isTwo }
        for (two in twos) {
            items += SamLocPenaltyItem(kindOfTwo(two, rules), listOf(two))
            rest.remove(two)
        }

        return SamLocHandPenalty(items, rest)
    }
}
