package com.baiviet.game.tienlen.rules

import com.baiviet.core.cards.Card
import kotlinx.serialization.Serializable

/** Loại heo/hàng bị thối hoặc bị chặt. */
@Serializable
enum class PenaltyKind(val viName: String) {
    BLACK_TWO("heo đen"),
    RED_TWO("heo đỏ"),
    THREE_PAIRS("3 đôi thông"),
    QUAD("tứ quý"),
    FOUR_PAIRS("4 đôi thông"),
}

@Serializable
data class PenaltyItem(val kind: PenaltyKind, val cards: List<Card>)

/**
 * Phân tích tay bài còn lại khi kết thúc ván.
 *
 * @param items heo/hàng bị thối
 * @param normalCards các lá còn lại tính như lá thường
 */
data class HandPenalty(val items: List<PenaltyItem>, val normalCards: List<Card>)

/**
 * Giá trị thối/chặt.
 *
 * Quyết định luật (xem docs/RULE_DECISIONS.md):
 * - Hàng thối tính THAY cho các lá tạo thành nó (3 đôi thông = 9 lá thay vì 6 lá).
 * - Tách hàng theo thứ tự: tứ quý trước, sau đó đôi thông chọn cách tách có giá trị lớn nhất.
 */
object Penalties {

    fun kindOfTwo(card: Card): PenaltyKind =
        if (card.suit.isRed) PenaltyKind.RED_TWO else PenaltyKind.BLACK_TWO

    /** Giá trị ở chế độ Đếm lá (đơn vị lá). */
    fun cards(kind: PenaltyKind, rules: TienLenRules): Int = when (kind) {
        PenaltyKind.BLACK_TWO -> rules.blackTwoCards
        PenaltyKind.RED_TWO -> rules.redTwoCards
        PenaltyKind.THREE_PAIRS -> rules.threePairsCards
        PenaltyKind.QUAD -> rules.quadCards
        PenaltyKind.FOUR_PAIRS -> rules.fourPairsCards
    }

    /** Giá trị ở chế độ Xếp hạng (đơn vị nửa B). */
    fun halves(kind: PenaltyKind, rules: TienLenRules): Int = when (kind) {
        PenaltyKind.BLACK_TWO -> rules.rankingBlackTwoHalves
        PenaltyKind.RED_TWO -> rules.rankingRedTwoHalves
        PenaltyKind.THREE_PAIRS -> rules.rankingThreePairsHalves
        PenaltyKind.QUAD -> rules.rankingQuadHalves
        PenaltyKind.FOUR_PAIRS -> rules.rankingFourPairsHalves
    }

    /** Giá trị quy ra xu theo chế độ tính tiền đang áp dụng. */
    fun xu(kind: PenaltyKind, rules: TienLenRules, betUnit: Long): Long = when (rules.scoring) {
        TlScoring.COUNT_CARDS -> cards(kind, rules) * betUnit
        TlScoring.RANKING -> halves(kind, rules) * betUnit / 2
    }

    /** Các khoản của một bộ bị chặt (heo lẻ, đôi heo = 2 heo, hàng). */
    fun kindsOfCombo(combo: Combo): List<PenaltyKind> = when {
        combo.isTwos -> combo.cards.map { kindOfTwo(it) }
        combo.type == ComboType.QUAD -> listOf(PenaltyKind.QUAD)
        combo.type == ComboType.PAIR_SEQUENCE && combo.pairCount == 3 -> listOf(PenaltyKind.THREE_PAIRS)
        combo.type == ComboType.PAIR_SEQUENCE -> listOf(PenaltyKind.FOUR_PAIRS)
        else -> emptyList()
    }

    /** Giá trị (xu) của bộ bị chặt. */
    fun comboXu(combo: Combo, rules: TienLenRules, betUnit: Long): Long =
        kindsOfCombo(combo).sumOf { xu(it, rules, betUnit) }

    /** Tách tay bài còn lại thành heo/hàng thối và lá thường. */
    fun analyze(hand: List<Card>, rules: TienLenRules): HandPenalty {
        val items = mutableListOf<PenaltyItem>()
        val rest = hand.sortedTl().toMutableList()

        // Heo lẻ
        rest.filter { it.isTwo }.forEach { two ->
            items += PenaltyItem(kindOfTwo(two), listOf(two))
            rest.remove(two)
        }

        // Tứ quý
        rest.groupBy { it.tlRank }.filterValues { it.size == 4 }.values.forEach { quad ->
            items += PenaltyItem(PenaltyKind.QUAD, quad)
            rest.removeAll(quad)
        }

        // Đôi thông: tìm các dãy rank liên tiếp có ≥ 2 lá
        val byRank = rest.groupBy { it.tlRank }
        val pairRanks = byRank.filterValues { it.size >= 2 }.keys.sorted()
        val runs = mutableListOf<List<Int>>()
        for (r in pairRanks) {
            if (runs.isNotEmpty() && runs.last().last() == r - 1) {
                runs[runs.size - 1] = runs.last() + r
            } else {
                runs += listOf(r)
            }
        }
        for (run in runs.filter { it.size >= 3 }) {
            for (segment in bestSplit(run.size, rules)) {
                val ranks = run.subList(segment.first, segment.last + 1)
                val cards = ranks.flatMap { byRank.getValue(it).take(2) }
                val kind = if (ranks.size == 3) PenaltyKind.THREE_PAIRS else PenaltyKind.FOUR_PAIRS
                items += PenaltyItem(kind, cards)
                rest.removeAll(cards)
            }
        }
        return HandPenalty(items, rest)
    }

    /**
     * Chia một dãy [length] đôi liên tiếp thành các đoạn đôi thông (≥ 3 đôi) để tổng giá trị lớn nhất.
     * Đôi không thuộc đoạn nào tính như 2 lá thường (Đếm lá) hoặc 0 (Xếp hạng).
     */
    private fun bestSplit(length: Int, rules: TienLenRules): List<IntRange> {
        val counting = rules.scoring == TlScoring.COUNT_CARDS
        val pairValue = if (counting) 2 else 0
        val threeValue = if (counting) rules.threePairsCards else rules.rankingThreePairsHalves
        val fourValue = if (counting) rules.fourPairsCards else rules.rankingFourPairsHalves
        val best = IntArray(length + 1)
        val choice = IntArray(length + 1) // độ dài đoạn kết thúc tại i (1 = đôi lẻ)
        for (i in 1..length) {
            best[i] = best[i - 1] + pairValue
            choice[i] = 1
            for (seg in 3..i) {
                val v = best[i - seg] + if (seg == 3) threeValue else fourValue
                if (v > best[i]) {
                    best[i] = v
                    choice[i] = seg
                }
            }
        }
        val segments = mutableListOf<IntRange>()
        var i = length
        while (i > 0) {
            val seg = choice[i]
            if (seg >= 3) segments += (i - seg) until i
            i -= seg
        }
        return segments.reversed()
    }
}
