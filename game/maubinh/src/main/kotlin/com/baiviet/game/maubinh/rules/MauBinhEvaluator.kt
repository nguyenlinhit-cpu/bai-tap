package com.baiviet.game.maubinh.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit

/**
 * Bộ chấm điểm và nhận diện chi bài Mậu Binh (5 lá hoặc 3 lá),
 * cùng bộ nhận diện Mậu Binh Tới Trắng.
 */
object MauBinhEvaluator {

    /**
     * Đánh giá chi 5 lá (Chi 1 hoặc Chi 2).
     */
    fun evaluateChi1OrChi2(cards: List<Card>, rules: MauBinhRules = MauBinhRules.DEFAULT): MauBinhChiHand {
        require(cards.size == 5) { "Chi 5 lá phải có đúng 5 lá (hiện có ${cards.size})" }
        val sorted = cards.sortedByDescending { it.mbRank }
        val ranks = sorted.map { it.mbRank }
        val isFlush = cards.map { it.suit }.distinct().size == 1

        val straightInfo = checkStraight(ranks, rules)
        val isStraight = straightInfo != null

        // 1. Thùng phá sảnh
        if (isFlush && isStraight) {
            val desc = "Thùng phá sảnh ${rankLabel(straightInfo!!.primaryRank)}"
            return MauBinhChiHand(
                type = MauBinhComboType.STRAIGHT_FLUSH,
                cards = sorted,
                tieBreakers = listOf(straightInfo.rankScore),
                description = desc,
            )
        }

        // Đếm số lượng theo rank
        val rankCounts = ranks.groupingBy { it }.eachCount().toList().sortedWith(
            compareByDescending<Pair<Int, Int>> { it.second }.thenByDescending { it.first },
        )

        // 2. Tứ quý
        if (rankCounts[0].second == 4) {
            val quadRank = rankCounts[0].first
            val kicker = rankCounts[1].first
            return MauBinhChiHand(
                type = MauBinhComboType.FOUR_OF_A_KIND,
                cards = sorted,
                tieBreakers = listOf(quadRank, kicker),
                description = "Tứ quý ${rankLabel(quadRank)}",
            )
        }

        // 3. Cù lũ
        if (rankCounts[0].second == 3 && rankCounts[1].second == 2) {
            val tripleRank = rankCounts[0].first
            val pairRank = rankCounts[1].first
            return MauBinhChiHand(
                type = MauBinhComboType.FULL_HOUSE,
                cards = sorted,
                tieBreakers = listOf(tripleRank, pairRank),
                description = "Cù lũ ${rankLabel(tripleRank)}",
            )
        }

        // 4. Thùng
        if (isFlush) {
            return MauBinhChiHand(
                type = MauBinhComboType.FLUSH,
                cards = sorted,
                tieBreakers = ranks,
                description = "Thùng ${rankLabel(ranks.first())}",
            )
        }

        // 5. Sảnh
        if (isStraight) {
            val desc = "Sảnh ${rankLabel(straightInfo!!.primaryRank)}"
            return MauBinhChiHand(
                type = MauBinhComboType.STRAIGHT,
                cards = sorted,
                tieBreakers = listOf(straightInfo.rankScore),
                description = desc,
            )
        }

        // 6. Sám cô
        if (rankCounts[0].second == 3) {
            val tripleRank = rankCounts[0].first
            val kickers = rankCounts.drop(1).map { it.first }
            return MauBinhChiHand(
                type = MauBinhComboType.THREE_OF_A_KIND,
                cards = sorted,
                tieBreakers = listOf(tripleRank) + kickers,
                description = "Sám ${rankLabel(tripleRank)}",
            )
        }

        // 7. Thú (2 đôi)
        if (rankCounts[0].second == 2 && rankCounts[1].second == 2) {
            val highPair = maxOf(rankCounts[0].first, rankCounts[1].first)
            val lowPair = minOf(rankCounts[0].first, rankCounts[1].first)
            val kicker = rankCounts[2].first
            return MauBinhChiHand(
                type = MauBinhComboType.TWO_PAIR,
                cards = sorted,
                tieBreakers = listOf(highPair, lowPair, kicker),
                description = "Thú (${rankLabel(highPair)}, ${rankLabel(lowPair)})",
            )
        }

        // 8. Một đôi
        if (rankCounts[0].second == 2) {
            val pairRank = rankCounts[0].first
            val kickers = rankCounts.drop(1).map { it.first }
            return MauBinhChiHand(
                type = MauBinhComboType.ONE_PAIR,
                cards = sorted,
                tieBreakers = listOf(pairRank) + kickers,
                description = "Đôi ${rankLabel(pairRank)}",
            )
        }

        // 9. Mậu thầu
        return MauBinhChiHand(
            type = MauBinhComboType.HIGH_CARD,
            cards = sorted,
            tieBreakers = ranks,
            description = "Mậu thầu ${rankLabel(ranks.first())}",
        )
    }

    /**
     * Đánh giá chi 3 lá (Chi 3 / Chi cuối).
     * Chỉ có: Sám cô > Đôi > Mậu thầu (không có sảnh/thùng 3 lá).
     */
    fun evaluateChi3(cards: List<Card>): MauBinhChiHand {
        require(cards.size == 3) { "Chi 3 lá phải có đúng 3 lá (hiện có ${cards.size})" }
        val sorted = cards.sortedByDescending { it.mbRank }
        val ranks = sorted.map { it.mbRank }

        val rankCounts = ranks.groupingBy { it }.eachCount().toList().sortedWith(
            compareByDescending<Pair<Int, Int>> { it.second }.thenByDescending { it.first },
        )

        // 1. Sám cô
        if (rankCounts[0].second == 3) {
            val tripleRank = rankCounts[0].first
            return MauBinhChiHand(
                type = MauBinhComboType.THREE_OF_A_KIND,
                cards = sorted,
                tieBreakers = listOf(tripleRank),
                description = "Sám ${rankLabel(tripleRank)}",
            )
        }

        // 2. Một đôi
        if (rankCounts[0].second == 2) {
            val pairRank = rankCounts[0].first
            val kicker = rankCounts[1].first
            return MauBinhChiHand(
                type = MauBinhComboType.ONE_PAIR,
                cards = sorted,
                tieBreakers = listOf(pairRank, kicker),
                description = "Đôi ${rankLabel(pairRank)}",
            )
        }

        // 3. Mậu thầu
        return MauBinhChiHand(
            type = MauBinhComboType.HIGH_CARD,
            cards = sorted,
            tieBreakers = ranks,
            description = "Mậu thầu ${rankLabel(ranks.first())}",
        )
    }

    private data class StraightCheckResult(val rankScore: Int, val primaryRank: Int)

    /**
     * Kiểm tra dãy 5 lá có tạo thành sảnh hay không:
     * - Sảnh thường: 5 rank liên tiếp: top - bottom == 4 && 5 distinct ranks.
     * - Sảnh A-2-3-4-5: ranks chứa [12 (A), 3 (5), 2 (4), 1 (3), 0 (2)].
     *   - Mặc định: lớn thứ nhì (rankScore = 95, cao hơn 9-10-J-Q-K (score 90), dưới 10-J-Q-K-A (score 100)).
     *   - Nếu [rules.aceTwoThreeFourFiveIsLowest]: nhỏ nhất (rankScore = 5, dưới 2-3-4-5-6 (score 10)).
     */
    private fun checkStraight(ranks: List<Int>, rules: MauBinhRules): StraightCheckResult? {
        val distinct = ranks.distinct().sortedDescending()
        if (distinct.size != 5) return null

        // Kiểm tra sảnh A-2-3-4-5: [12, 3, 2, 1, 0]
        if (distinct == listOf(12, 3, 2, 1, 0)) {
            val score = if (rules.aceTwoThreeFourFiveIsLowest) 5 else 95
            return StraightCheckResult(rankScore = score, primaryRank = 3) // lá 5
        }

        // Sảnh thông thường
        if (distinct[0] - distinct[4] == 4) {
            // Sảnh 10-J-Q-K-A: [12, 11, 10, 9, 8] -> score 100
            val top = distinct[0]
            val score = if (top == 12) 100 else (top - 3) * 10
            return StraightCheckResult(rankScore = score, primaryRank = top)
        }

        return null
    }

    /**
     * Nhận diện Mậu binh tới trắng từ 13 lá bài chia ban đầu.
     * Trả về loại tới trắng cao nhất nếu có.
     */
    fun detectInstantWin(hand13: List<Card>): MauBinhInstantWinType? {
        require(hand13.size == 13) { "Bài phải đủ 13 lá" }

        // 1. Rồng cuốn: 13 lá 2 -> A cùng chất
        val bySuit = hand13.groupBy { it.suit }
        if (bySuit.size == 1 && hand13.map { it.mbRank }.distinct().size == 13) {
            return MauBinhInstantWinType.DRAGON_ROLL
        }

        // 2. Sảnh rồng: 13 lá 2 -> A khác chất
        if (hand13.map { it.mbRank }.distinct().size == 13) {
            return MauBinhInstantWinType.DRAGON_STRAIGHT
        }

        val counts = hand13.groupingBy { it.rank }.eachCount().values.sortedDescending()

        // 3. Năm đôi một sám: 1 bộ ba + 5 đôi
        if (counts.size == 6 && counts[0] == 3 && counts.drop(1).all { it == 2 }) {
            return MauBinhInstantWinType.FIVE_PAIRS_ONE_TRIPLE
        }

        // 4. Lục phé bôn: 6 đôi + 1 lá lẻ
        // counts có thể là [2, 2, 2, 2, 2, 2, 1] hoặc chứa 1 tứ quý [4, 2, 2, 2, 2, 1] (tứ quý tính là 2 đôi)
        val pairCount = counts.sumOf { it / 2 }
        if (pairCount >= 6) {
            return MauBinhInstantWinType.SIX_PAIRS
        }

        // 5. Ba thùng: 3 chi đều cùng chất (5-5-3)
        if (canFormThreeFlushes(hand13)) {
            return MauBinhInstantWinType.THREE_FLUSHES
        }

        // 6. Ba sảnh: 3 chi đều là sảnh (5 liên tiếp, 5 liên tiếp, 3 liên tiếp)
        if (canFormThreeStraights(hand13)) {
            return MauBinhInstantWinType.THREE_STRAIGHTS
        }

        return null
    }

    private fun canFormThreeFlushes(hand13: List<Card>): Boolean {
        // Ba thùng nghĩa là 13 lá có thể chia thành (5 lá cùng chất), (5 lá cùng chất), (3 lá cùng chất)
        val suitCounts = hand13.groupingBy { it.suit }.eachCount()
        val vals = suitCounts.values.sortedDescending()
        // Các phân phối có thể:
        // [13] -> 5 + 5 + 3
        // [10, 3] -> 5 + 5 + 3
        // [8, 5] -> 5 + 3 + 5
        // [5, 5, 3] -> 5 + 5 + 3
        return when {
            vals.firstOrNull() ?: 0 == 13 -> true
            vals.size >= 2 && vals[0] >= 10 && vals[1] >= 3 -> true
            vals.size >= 2 && vals[0] >= 8 && vals[1] >= 5 -> true
            vals.size >= 3 && vals[0] >= 5 && vals[1] >= 5 && vals[2] >= 3 -> true
            else -> false
        }
    }

    private fun canFormThreeStraights(hand13: List<Card>): Boolean {
        // Kiểm tra xem có thể phân hoạch 13 lá thành 2 sảnh 5 lá và 1 sảnh 3 lá hay không
        // Sảnh 3 lá là 3 lá có rank liên tiếp: x, x+1, x+2 (hoặc A-2-3 / Q-K-A)
        val ranks = hand13.map { it.mbRank }.sorted()

        // Thử tìm sảnh 5 thứ nhất
        val all5Straights = findAll5Straights(ranks)
        for (s1 in all5Straights) {
            val remain1 = removeCards(ranks, s1)
            val remain5Straights = findAll5Straights(remain1)
            for (s2 in remain5Straights) {
                val remain2 = removeCards(remain1, s2)
                if (is3Straight(remain2)) {
                    return true
                }
            }
        }
        return false
    }

    private fun findAll5Straights(ranks: List<Int>): List<List<Int>> {
        val distinct = ranks.distinct()
        val list = mutableListOf<List<Int>>()
        // A-2-3-4-5: [0, 1, 2, 3, 12]
        if (distinct.containsAll(listOf(0, 1, 2, 3, 12))) {
            list.add(listOf(0, 1, 2, 3, 12))
        }
        for (i in 0..8) {
            val target = (i..i + 4).toList()
            if (distinct.containsAll(target)) {
                list.add(target)
            }
        }
        return list
    }

    private fun is3Straight(ranks3: List<Int>): Boolean {
        if (ranks3.size != 3) return false
        val s = ranks3.distinct().sorted()
        if (s.size != 3) return false
        // A-2-3: [0, 1, 12]
        if (s == listOf(0, 1, 12)) return true
        // Q-K-A: [10, 11, 12]
        if (s[2] - s[0] == 2) return true
        return false
    }

    private fun removeCards(source: List<Int>, toRemove: List<Int>): List<Int> {
        val m = source.toMutableList()
        for (item in toRemove) {
            m.remove(item)
        }
        return m
    }

    private fun rankLabel(mbRankIndex: Int): String = when (mbRankIndex) {
        0 -> "2"
        1 -> "3"
        2 -> "4"
        3 -> "5"
        4 -> "6"
        5 -> "7"
        6 -> "8"
        7 -> "9"
        8 -> "10"
        9 -> "J"
        10 -> "Q"
        11 -> "K"
        12 -> "A"
        else -> ""
    }
}
