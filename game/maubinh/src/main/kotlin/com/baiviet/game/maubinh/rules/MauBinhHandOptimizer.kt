package com.baiviet.game.maubinh.rules

import com.baiviet.core.cards.Card

/**
 * Thuật toán tối ưu hóa sắp xếp bài Mậu Binh tốc độ cao (Zero-allocation inner loop).
 * Dùng cho Bot AI và tính năng "Xếp gợi ý" của người chơi.
 */
object MauBinhHandOptimizer {

    data class OptimizationResult(
        val arrangement: MauBinhArrangement,
        val instantWinType: MauBinhInstantWinType?,
        val score: Double,
        val chi1Hand: MauBinhChiHand,
        val chi2Hand: MauBinhChiHand,
        val chi3Hand: MauBinhChiHand,
    )

    /**
     * Tìm cách xếp bài 13 lá tối ưu nhất (hợp lệ, không binh lủng, tối đa hóa kỳ vọng ăn chi).
     */
    fun findBestArrangement(
        cards: List<Card>,
        rules: MauBinhRules = MauBinhRules.DEFAULT,
    ): OptimizationResult {
        require(cards.size == 13) { "Cần đúng 13 lá để xếp bài Mậu Binh" }

        val instantWin = MauBinhEvaluator.detectInstantWin(cards)

        // Sắp xếp bài giảm dần theo rank để tổ hợp nhất quán
        val sortedCards = cards.sortedWith(MauBinhOrder.cardComparator.reversed())
        val ranks = IntArray(13) { sortedCards[it].mbRank }
        val suits = IntArray(13) { sortedCards[it].suit.ordinal }

        var bestScore = Long.MIN_VALUE
        var bestChi1Indices: IntArray? = null
        var bestChi2Indices: IntArray? = null
        var bestChi3Indices: IntArray? = null

        // Mảng tái sử dụng lưu 8 lá còn lại
        val rem8 = IntArray(8)
        val rem3 = IntArray(3)

        // Duyệt C(13, 5) = 1,287 cách chọn Chi 1
        for (i0 in 0..8) {
            for (i1 in (i0 + 1)..9) {
                for (i2 in (i1 + 1)..10) {
                    for (i3 in (i2 + 1)..11) {
                        for (i4 in (i3 + 1)..12) {
                            val h1Score = fastEval5(
                                ranks[i0], ranks[i1], ranks[i2], ranks[i3], ranks[i4],
                                suits[i0] == suits[i1] && suits[i1] == suits[i2] && suits[i2] == suits[i3] && suits[i3] == suits[i4],
                                rules,
                            )

                            // 8 chỉ số còn lại
                            var rIdx = 0
                            for (idx in 0..12) {
                                if (idx != i0 && idx != i1 && idx != i2 && idx != i3 && idx != i4) {
                                    rem8[rIdx++] = idx
                                }
                            }

                            // Duyệt C(8, 5) = 56 cách chọn Chi 2
                            for (j0 in 0..3) {
                                val idx0 = rem8[j0]
                                for (j1 in (j0 + 1)..4) {
                                    val idx1 = rem8[j1]
                                    for (j2 in (j1 + 1)..5) {
                                        val idx2 = rem8[j2]
                                        for (j3 in (j2 + 1)..6) {
                                            val idx3 = rem8[j3]
                                            for (j4 in (j3 + 1)..7) {
                                                val idx4 = rem8[j4]

                                                val h2Score = fastEval5(
                                                    ranks[idx0], ranks[idx1], ranks[idx2], ranks[idx3], ranks[idx4],
                                                    suits[idx0] == suits[idx1] && suits[idx1] == suits[idx2] && suits[idx2] == suits[idx3] && suits[idx3] == suits[idx4],
                                                    rules,
                                                )

                                                // CẮT TỈA: Nếu Chi 1 < Chi 2 -> Binh lủng, bỏ qua ngay!
                                                if (h1Score < h2Score) continue

                                                // 3 chỉ số còn lại cho Chi 3
                                                var c3Idx = 0
                                                for (k in 0..7) {
                                                    if (k != j0 && k != j1 && k != j2 && k != j3 && k != j4) {
                                                        rem3[c3Idx++] = rem8[k]
                                                    }
                                                }

                                                val k0 = rem3[0]
                                                val k1 = rem3[1]
                                                val k2 = rem3[2]

                                                val h3Score = fastEval3(ranks[k0], ranks[k1], ranks[k2])

                                                // CẮT TỈA: So Chi 2 với Chi 3
                                                if (!isChi2AtLeastChi3(h2Score, h3Score)) continue

                                                // Tính điểm kỳ vọng
                                                val score = calculateHandScore(h1Score, h2Score, h3Score, rules)
                                                if (score > bestScore) {
                                                    bestScore = score
                                                    bestChi1Indices = intArrayOf(i0, i1, i2, i3, i4)
                                                    bestChi2Indices = intArrayOf(idx0, idx1, idx2, idx3, idx4)
                                                    bestChi3Indices = intArrayOf(k0, k1, k2)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val arrangement: MauBinhArrangement
        if (bestChi1Indices != null && bestChi2Indices != null && bestChi3Indices != null) {
            arrangement = MauBinhArrangement(
                chi1 = bestChi1Indices.map { sortedCards[it] },
                chi2 = bestChi2Indices.map { sortedCards[it] },
                chi3 = bestChi3Indices.map { sortedCards[it] },
            )
        } else {
            arrangement = fallbackArrangement(sortedCards)
        }

        val h1 = MauBinhEvaluator.evaluateChi1OrChi2(arrangement.chi1, rules)
        val h2 = MauBinhEvaluator.evaluateChi1OrChi2(arrangement.chi2, rules)
        val h3 = MauBinhEvaluator.evaluateChi3(arrangement.chi3)

        return OptimizationResult(
            arrangement = arrangement,
            instantWinType = instantWin,
            score = bestScore.toDouble(),
            chi1Hand = h1,
            chi2Hand = h2,
            chi3Hand = h3,
        )
    }

    /**
     * Đánh giá chi 5 lá và mã hóa thành một số nguyên 64-bit duy nhất (từ cao xuống thấp).
     * Bit 40..36: Type (1..9)
     * Bit 35..0: Tie breakers
     */
    private fun fastEval5(r0: Int, r1: Int, r2: Int, r3: Int, r4: Int, isFlush: Boolean, rules: MauBinhRules): Long {
        // Kiểm tra sảnh
        val isA2345 = r0 == 12 && r1 == 3 && r2 == 2 && r3 == 1 && r4 == 0
        val isStraightNorm = r0 - r4 == 4 && r0 != r1 && r1 != r2 && r2 != r3 && r3 != r4
        val isStraight = isA2345 || isStraightNorm
        val straightScore = if (isA2345) {
            if (rules.aceTwoThreeFourFiveIsLowest) 5 else 95
        } else if (isStraightNorm) {
            if (r0 == 12) 100 else (r0 - 3) * 10
        } else 0

        // 1. Thùng phá sảnh
        if (isFlush && isStraight) {
            return encode(9, straightScore, 0, 0, 0, 0)
        }

        // 2. Tứ quý
        if (r0 == r3) return encode(8, r0, r4, 0, 0, 0)
        if (r1 == r4) return encode(8, r1, r0, 0, 0, 0)

        // 3. Cù lũ
        if (r0 == r2 && r3 == r4) return encode(7, r0, r3, 0, 0, 0)
        if (r0 == r1 && r2 == r4) return encode(7, r2, r0, 0, 0, 0)

        // 4. Thùng
        if (isFlush) return encode(6, r0, r1, r2, r3, r4)

        // 5. Sảnh
        if (isStraight) return encode(5, straightScore, 0, 0, 0, 0)

        // 6. Sám cô
        if (r0 == r2) return encode(4, r0, r3, r4, 0, 0)
        if (r1 == r3) return encode(4, r1, r0, r4, 0, 0)
        if (r2 == r4) return encode(4, r2, r0, r1, 0, 0)

        // 7. Thú (2 đôi)
        if (r0 == r1 && r2 == r3) return encode(3, r0, r2, r4, 0, 0)
        if (r0 == r1 && r3 == r4) return encode(3, r0, r3, r2, 0, 0)
        if (r1 == r2 && r3 == r4) return encode(3, r1, r3, r0, 0, 0)

        // 8. Một đôi
        if (r0 == r1) return encode(2, r0, r2, r3, r4, 0)
        if (r1 == r2) return encode(2, r1, r0, r3, r4, 0)
        if (r2 == r3) return encode(2, r2, r0, r1, r4, 0)
        if (r3 == r4) return encode(2, r3, r0, r1, r2, 0)

        // 9. Mậu thầu
        return encode(1, r0, r1, r2, r3, r4)
    }

    /**
     * Đánh giá chi 3 lá (r0 >= r1 >= r2).
     */
    private fun fastEval3(r0: Int, r1: Int, r2: Int): Long {
        if (r0 == r2) return encode(4, r0, 0, 0, 0, 0) // Sám cô
        if (r0 == r1) return encode(2, r0, r2, 0, 0, 0) // Đôi
        if (r1 == r2) return encode(2, r1, r0, 0, 0, 0) // Đôi
        return encode(1, r0, r1, r2, 0, 0) // Mậu thầu
    }

    private fun encode(type: Int, b0: Int, b1: Int, b2: Int, b3: Int, b4: Int): Long {
        return (type.toLong() shl 35) or
            (b0.toLong() shl 28) or
            (b1.toLong() shl 21) or
            (b2.toLong() shl 14) or
            (b3.toLong() shl 7) or
            b4.toLong()
    }

    private fun isChi2AtLeastChi3(h2Code: Long, h3Code: Long): Boolean {
        val t2 = (h2Code shr 35).toInt()
        val t3 = (h3Code shr 35).toInt()
        if (t2 > t3) return true
        if (t2 < t3) return false

        // t2 == t3 (chỉ có thể là Sám (4), Đôi (2), hoặc Mậu thầu (1))
        val b2_0 = ((h2Code shr 28) and 0x7F).toInt()
        val b3_0 = ((h3Code shr 28) and 0x7F).toInt()
        if (b2_0 != b3_0) return b2_0 > b3_0

        val b2_1 = ((h2Code shr 21) and 0x7F).toInt()
        val b3_1 = ((h3Code shr 21) and 0x7F).toInt()
        if (b2_1 != b3_1) return b2_1 > b3_1

        val b2_2 = ((h2Code shr 14) and 0x7F).toInt()
        val b3_2 = ((h3Code shr 14) and 0x7F).toInt()
        return b2_2 >= b3_2
    }

    private fun calculateHandScore(h1: Long, h2: Long, h3: Long, rules: MauBinhRules): Long {
        val t1 = (h1 shr 35).toInt()
        val t2 = (h2 shr 35).toInt()
        val t3 = (h3 shr 35).toInt()

        var score = (t1 * 1000L) + (t2 * 1000L) + (t3 * 1000L)

        // Chi 1 bonuses
        if (t1 == 9) score += rules.bonusChiThungPhaSanhChi1 * 1500L
        else if (t1 == 8) score += rules.bonusChiTuQuyChi1 * 1500L

        // Chi 2 bonuses
        if (t2 == 9) score += rules.bonusChiThungPhaSanhChi2 * 1500L
        else if (t2 == 8) score += rules.bonusChiTuQuyChi2 * 1500L
        else if (t2 == 7) score += rules.bonusChiCuLuChi2 * 1500L

        // Chi 3 bonuses
        if (t3 == 4) score += rules.bonusChiSamChi3 * 1500L

        // Cân bằng các chi
        val minT = minOf(t1, t2, t3)
        score += minT * 500L

        return score
    }

    private fun fallbackArrangement(sortedCards: List<Card>): MauBinhArrangement {
        val chi1 = sortedCards.take(5)
        val chi2 = sortedCards.drop(5).take(5)
        val chi3 = sortedCards.takeLast(3)
        return MauBinhArrangement(chi1, chi2, chi3)
    }
}
