package com.baiviet.game.maubinh.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.SettlementLine
import kotlinx.serialization.Serializable

/**
 * Chi tiết kết quả so sánh một chi giữa 2 người chơi.
 */
@Serializable
data class MauBinhChiComparison(
    val chiIndex: Int, // 1, 2, hoặc 3
    val p1Hand: MauBinhChiHand,
    val p2Hand: MauBinhChiHand,
    val winner: PlayerId?, // null nếu hòa
    val chiScore: Int, // Số chi người thắng nhận (> 0), hoặc 0 nếu hòa
    val isBonus: Boolean, // Thắng bằng hàng đặc biệt
    val description: String,
)

/**
 * Kết quả so sánh đối đầu giữa 2 người chơi (p1 vs p2).
 */
@Serializable
data class MauBinhPairwiseComparison(
    val p1: PlayerId,
    val p2: PlayerId,
    val netChiForP1: Int, // Dương: p1 ăn p2; Âm: p2 ăn p1; 0: hòa
    val isP1InstantWin: Boolean = false,
    val isP2InstantWin: Boolean = false,
    val p1InstantWinType: MauBinhInstantWinType? = null,
    val p2InstantWinType: MauBinhInstantWinType? = null,
    val isP1Foul: Boolean = false,
    val isP2Foul: Boolean = false,
    val isSap3ChiForP1: Boolean = false,
    val isSap3ChiForP2: Boolean = false,
    val isSapLangForP1: Boolean = false,
    val isSapLangForP2: Boolean = false,
    val chi1Comparison: MauBinhChiComparison? = null,
    val chi2Comparison: MauBinhChiComparison? = null,
    val chi3Comparison: MauBinhChiComparison? = null,
    val summaryReason: String,
)

/**
 * Trạng thái bài của người chơi phục vụ tính điểm cuối ván.
 */
@Serializable
data class MauBinhPlayerHand(
    val playerId: PlayerId,
    val cards: List<Card>,
    val instantWinType: MauBinhInstantWinType? = null,
    val arrangement: MauBinhArrangement? = null,
)

/**
 * Bộ máy tính điểm và lập Settlement cho Mậu Binh.
 */
object MauBinhScoring {

    /**
     * Tính toán toàn bộ kết quả đối đầu từng cặp (pairwise) và tạo Settlement zero-sum.
     */
    fun settle(
        playerHands: List<MauBinhPlayerHand>,
        rules: MauBinhRules,
        baseBet: Long,
    ): Settlement {
        val players = playerHands.map { it.playerId }
        val pairwiseResults = mutableListOf<MauBinhPairwiseComparison>()

        // 1. Phân loại từng người chơi: Tới trắng, Binh lủng, hay Hợp lệ
        val evaluatedHands = playerHands.associateWith { hand ->
            val isFoul = if (hand.instantWinType != null) {
                false
            } else if (hand.arrangement != null) {
                hand.arrangement.isFoul(rules)
            } else {
                true // Chưa xếp hoặc thiếu bài -> xem như binh lủng
            }
            isFoul
        }

        // 2. Tìm xem có ai đạt Sập làng không (chỉ áp dụng cho bàn >= 3 người)
        // Một người sập làng nếu họ sập 3 chi với TẤT CẢ các đối thủ khác (không tính tới trắng).
        val potentialSap3ChiCounts = mutableMapOf<PlayerId, Int>()

        // So sơ bộ để kiểm tra sập 3 chi giữa các cặp thông thường
        for (i in playerHands.indices) {
            val p1 = playerHands[i]
            if (p1.instantWinType != null || evaluatedHands[p1] == true) continue
            var sweepCount = 0
            for (j in playerHands.indices) {
                if (i == j) continue
                val p2 = playerHands[j]
                if (p2.instantWinType != null) continue
                if (evaluatedHands[p2] == true) {
                    // Đối thủ binh lủng -> tự động coi như bị sập
                    sweepCount++
                } else {
                    val p1Arr = p1.arrangement!!
                    val p2Arr = p2.arrangement!!
                    val h1_1 = MauBinhEvaluator.evaluateChi1OrChi2(p1Arr.chi1, rules)
                    val h1_2 = MauBinhEvaluator.evaluateChi1OrChi2(p2Arr.chi1, rules)
                    val h2_1 = MauBinhEvaluator.evaluateChi1OrChi2(p1Arr.chi2, rules)
                    val h2_2 = MauBinhEvaluator.evaluateChi1OrChi2(p2Arr.chi2, rules)
                    val h3_1 = MauBinhEvaluator.evaluateChi3(p1Arr.chi3)
                    val h3_2 = MauBinhEvaluator.evaluateChi3(p2Arr.chi3)

                    if (h1_1 > h1_2 && h2_1 > h2_2 && h3_1 > h3_2) {
                        sweepCount++
                    }
                }
            }
            potentialSap3ChiCounts[p1.playerId] = sweepCount
        }

        val totalOpponents = playerHands.size - 1
        val sapLangPlayers = potentialSap3ChiCounts.filter { (pId, count) ->
            playerHands.size >= 3 && count == totalOpponents && rules.sapLangMultiplier
        }.keys

        // 3. So bài từng cặp (i, j) với i < j
        val settlementLines = mutableListOf<SettlementLine>()

        for (i in 0 until playerHands.size) {
            for (j in (i + 1) until playerHands.size) {
                val hand1 = playerHands[i]
                val hand2 = playerHands[j]
                val isP1SapLang = sapLangPlayers.contains(hand1.playerId)
                val isP2SapLang = sapLangPlayers.contains(hand2.playerId)

                val pair = comparePair(
                    hand1 = hand1,
                    hand2 = hand2,
                    isP1Foul = evaluatedHands[hand1] == true,
                    isP2Foul = evaluatedHands[hand2] == true,
                    isP1SapLang = isP1SapLang,
                    isP2SapLang = isP2SapLang,
                    rules = rules,
                )
                pairwiseResults.add(pair)

                // Tạo dòng SettlementLine
                if (pair.netChiForP1 > 0) {
                    val amount = pair.netChiForP1 * baseBet
                    settlementLines.add(
                        SettlementLine(
                            from = hand2.playerId,
                            to = hand1.playerId,
                            amount = amount,
                            reason = "${hand1.playerId} thắng ${hand2.playerId} (+${pair.netChiForP1} chi): ${pair.summaryReason}",
                        ),
                    )
                } else if (pair.netChiForP1 < 0) {
                    val amount = (-pair.netChiForP1) * baseBet
                    settlementLines.add(
                        SettlementLine(
                            from = hand1.playerId,
                            to = hand2.playerId,
                            amount = amount,
                            reason = "${hand2.playerId} thắng ${hand1.playerId} (+${-pair.netChiForP1} chi): ${pair.summaryReason}",
                        ),
                    )
                }
            }
        }

        return Settlement.fromLines(players, settlementLines)
    }

    /**
     * So sánh chi tiết một cặp đấu.
     */
    fun comparePair(
        hand1: MauBinhPlayerHand,
        hand2: MauBinhPlayerHand,
        isP1Foul: Boolean,
        isP2Foul: Boolean,
        isP1SapLang: Boolean,
        isP2SapLang: Boolean,
        rules: MauBinhRules,
    ): MauBinhPairwiseComparison {
        val p1 = hand1.playerId
        val p2 = hand2.playerId

        // TH1: Tới trắng
        if (hand1.instantWinType != null || hand2.instantWinType != null) {
            return resolveInstantWinPair(hand1, hand2, rules)
        }

        // TH2: Binh lủng
        if (isP1Foul && isP2Foul) {
            return MauBinhPairwiseComparison(
                p1 = p1,
                p2 = p2,
                netChiForP1 = 0,
                isP1Foul = true,
                isP2Foul = true,
                summaryReason = "Cả hai cùng binh lủng (hòa)",
            )
        }
        if (isP1Foul) {
            val penalty = rules.foulPenaltyChi
            return MauBinhPairwiseComparison(
                p1 = p1,
                p2 = p2,
                netChiForP1 = -penalty,
                isP1Foul = true,
                isP2Foul = false,
                summaryReason = "$p1 binh lủng (−$penalty chi)",
            )
        }
        if (isP2Foul) {
            val penalty = rules.foulPenaltyChi
            return MauBinhPairwiseComparison(
                p1 = p1,
                p2 = p2,
                netChiForP1 = penalty,
                isP1Foul = false,
                isP2Foul = true,
                summaryReason = "$p2 binh lủng (−$penalty chi)",
            )
        }

        // TH3: Cả hai đều hợp lệ -> So từng chi
        val arr1 = hand1.arrangement!!
        val arr2 = hand2.arrangement!!

        val h1_1 = MauBinhEvaluator.evaluateChi1OrChi2(arr1.chi1, rules)
        val h1_2 = MauBinhEvaluator.evaluateChi1OrChi2(arr2.chi1, rules)
        val chi1Comp = compareChi1(p1, p2, h1_1, h1_2, rules)

        val h2_1 = MauBinhEvaluator.evaluateChi1OrChi2(arr1.chi2, rules)
        val h2_2 = MauBinhEvaluator.evaluateChi1OrChi2(arr2.chi2, rules)
        val chi2Comp = compareChi2(p1, p2, h2_1, h2_2, rules)

        val h3_1 = MauBinhEvaluator.evaluateChi3(arr1.chi3)
        val h3_2 = MauBinhEvaluator.evaluateChi3(arr2.chi3)
        val chi3Comp = compareChi3(p1, p2, h3_1, h3_2, rules)

        var chi1Points = scoreChiForP1(chi1Comp, p1)
        var chi2Points = scoreChiForP1(chi2Comp, p1)
        var chi3Points = scoreChiForP1(chi3Comp, p1)

        val p1WinsAll = chi1Points > 0 && chi2Points > 0 && chi3Points > 0
        val p2WinsAll = chi1Points < 0 && chi2Points < 0 && chi3Points < 0

        var isSap3ChiForP1 = false
        var isSap3ChiForP2 = false

        var totalChi = chi1Points + chi2Points + chi3Points

        if (p1WinsAll && rules.sapThreeChiMultiplier) {
            isSap3ChiForP1 = true
            totalChi *= 2
        } else if (p2WinsAll && rules.sapThreeChiMultiplier) {
            isSap3ChiForP2 = true
            totalChi *= 2
        }

        if (isP1SapLang && isSap3ChiForP1) {
            totalChi *= 2
        } else if (isP2SapLang && isSap3ChiForP2) {
            totalChi *= 2
        }

        val reason = buildString {
            if (isP1SapLang) append("Sập làng! ")
            else if (isP2SapLang) append("Bị sập làng! ")
            else if (isSap3ChiForP1) append("Sập 3 chi (×2)! ")
            else if (isSap3ChiForP2) append("Bị sập 3 chi (×2)! ")

            append("Chi 1: ${chi1Comp.description}, ")
            append("Chi 2: ${chi2Comp.description}, ")
            append("Chi 3: ${chi3Comp.description}")
        }

        return MauBinhPairwiseComparison(
            p1 = p1,
            p2 = p2,
            netChiForP1 = totalChi,
            isP1Foul = false,
            isP2Foul = false,
            isSap3ChiForP1 = isSap3ChiForP1,
            isSap3ChiForP2 = isSap3ChiForP2,
            isSapLangForP1 = isP1SapLang && isSap3ChiForP1,
            isSapLangForP2 = isP2SapLang && isSap3ChiForP2,
            chi1Comparison = chi1Comp,
            chi2Comparison = chi2Comp,
            chi3Comparison = chi3Comp,
            summaryReason = reason,
        )
    }

    private fun scoreChiForP1(comp: MauBinhChiComparison, p1: PlayerId): Int {
        return when (comp.winner) {
            p1 -> comp.chiScore
            null -> 0
            else -> -comp.chiScore
        }
    }

    private fun compareChi1(
        p1: PlayerId,
        p2: PlayerId,
        h1: MauBinhChiHand,
        h2: MauBinhChiHand,
        rules: MauBinhRules,
    ): MauBinhChiComparison {
        val cmp = h1.compareTo(h2)
        return when {
            cmp > 0 -> {
                val isBonus = h1.type == MauBinhComboType.FOUR_OF_A_KIND || h1.type == MauBinhComboType.STRAIGHT_FLUSH
                val score = when (h1.type) {
                    MauBinhComboType.STRAIGHT_FLUSH -> rules.bonusChiThungPhaSanhChi1
                    MauBinhComboType.FOUR_OF_A_KIND -> rules.bonusChiTuQuyChi1
                    else -> 1
                }
                val desc = if (isBonus) "${h1.description} (+${score} chi)" else "${h1.description} thắng ${h2.description}"
                MauBinhChiComparison(1, h1, h2, p1, score, isBonus, desc)
            }
            cmp < 0 -> {
                val isBonus = h2.type == MauBinhComboType.FOUR_OF_A_KIND || h2.type == MauBinhComboType.STRAIGHT_FLUSH
                val score = when (h2.type) {
                    MauBinhComboType.STRAIGHT_FLUSH -> rules.bonusChiThungPhaSanhChi1
                    MauBinhComboType.FOUR_OF_A_KIND -> rules.bonusChiTuQuyChi1
                    else -> 1
                }
                val desc = if (isBonus) "${h2.description} (+${score} chi)" else "${h2.description} thắng ${h1.description}"
                MauBinhChiComparison(1, h1, h2, p2, score, isBonus, desc)
            }
            else -> MauBinhChiComparison(1, h1, h2, null, 0, false, "Hòa (${h1.description})")
        }
    }

    private fun compareChi2(
        p1: PlayerId,
        p2: PlayerId,
        h1: MauBinhChiHand,
        h2: MauBinhChiHand,
        rules: MauBinhRules,
    ): MauBinhChiComparison {
        val cmp = h1.compareTo(h2)
        return when {
            cmp > 0 -> {
                val isBonus = h1.type == MauBinhComboType.FULL_HOUSE ||
                    h1.type == MauBinhComboType.FOUR_OF_A_KIND ||
                    h1.type == MauBinhComboType.STRAIGHT_FLUSH
                val score = when (h1.type) {
                    MauBinhComboType.STRAIGHT_FLUSH -> rules.bonusChiThungPhaSanhChi2
                    MauBinhComboType.FOUR_OF_A_KIND -> rules.bonusChiTuQuyChi2
                    MauBinhComboType.FULL_HOUSE -> rules.bonusChiCuLuChi2
                    else -> 1
                }
                val desc = if (isBonus) "${h1.description} (+${score} chi)" else "${h1.description} thắng ${h2.description}"
                MauBinhChiComparison(2, h1, h2, p1, score, isBonus, desc)
            }
            cmp < 0 -> {
                val isBonus = h2.type == MauBinhComboType.FULL_HOUSE ||
                    h2.type == MauBinhComboType.FOUR_OF_A_KIND ||
                    h2.type == MauBinhComboType.STRAIGHT_FLUSH
                val score = when (h2.type) {
                    MauBinhComboType.STRAIGHT_FLUSH -> rules.bonusChiThungPhaSanhChi2
                    MauBinhComboType.FOUR_OF_A_KIND -> rules.bonusChiTuQuyChi2
                    MauBinhComboType.FULL_HOUSE -> rules.bonusChiCuLuChi2
                    else -> 1
                }
                val desc = if (isBonus) "${h2.description} (+${score} chi)" else "${h2.description} thắng ${h1.description}"
                MauBinhChiComparison(2, h1, h2, p2, score, isBonus, desc)
            }
            else -> MauBinhChiComparison(2, h1, h2, null, 0, false, "Hòa (${h1.description})")
        }
    }

    private fun compareChi3(
        p1: PlayerId,
        p2: PlayerId,
        h1: MauBinhChiHand,
        h2: MauBinhChiHand,
        rules: MauBinhRules,
    ): MauBinhChiComparison {
        val cmp = h1.compareTo(h2)
        return when {
            cmp > 0 -> {
                val isBonus = h1.type == MauBinhComboType.THREE_OF_A_KIND
                val score = if (isBonus) rules.bonusChiSamChi3 else 1
                val desc = if (isBonus) "${h1.description} (+${score} chi)" else "${h1.description} thắng ${h2.description}"
                MauBinhChiComparison(3, h1, h2, p1, score, isBonus, desc)
            }
            cmp < 0 -> {
                val isBonus = h2.type == MauBinhComboType.THREE_OF_A_KIND
                val score = if (isBonus) rules.bonusChiSamChi3 else 1
                val desc = if (isBonus) "${h2.description} (+${score} chi)" else "${h2.description} thắng ${h1.description}"
                MauBinhChiComparison(3, h1, h2, p2, score, isBonus, desc)
            }
            else -> MauBinhChiComparison(3, h1, h2, null, 0, false, "Hòa (${h1.description})")
        }
    }

    private fun resolveInstantWinPair(
        hand1: MauBinhPlayerHand,
        hand2: MauBinhPlayerHand,
        rules: MauBinhRules,
    ): MauBinhPairwiseComparison {
        val p1 = hand1.playerId
        val p2 = hand2.playerId
        val iw1 = hand1.instantWinType
        val iw2 = hand2.instantWinType

        if (iw1 != null && iw2 != null) {
            // Cả hai cùng tới trắng: so độ ưu tiên
            return when {
                iw1.priority > iw2.priority -> {
                    val reward = getInstantWinChi(iw1, rules)
                    MauBinhPairwiseComparison(
                        p1 = p1,
                        p2 = p2,
                        netChiForP1 = reward,
                        isP1InstantWin = true,
                        isP2InstantWin = true,
                        p1InstantWinType = iw1,
                        p2InstantWinType = iw2,
                        summaryReason = "Tới trắng: ${iw1.viName} cao hơn ${iw2.viName} (+${reward} chi)",
                    )
                }
                iw1.priority < iw2.priority -> {
                    val reward = getInstantWinChi(iw2, rules)
                    MauBinhPairwiseComparison(
                        p1 = p1,
                        p2 = p2,
                        netChiForP1 = -reward,
                        isP1InstantWin = true,
                        isP2InstantWin = true,
                        p1InstantWinType = iw1,
                        p2InstantWinType = iw2,
                        summaryReason = "Tới trắng: ${iw2.viName} cao hơn ${iw1.viName} (−${reward} chi)",
                    )
                }
                else -> {
                    // Cùng loại tới trắng -> hòa
                    MauBinhPairwiseComparison(
                        p1 = p1,
                        p2 = p2,
                        netChiForP1 = 0,
                        isP1InstantWin = true,
                        isP2InstantWin = true,
                        p1InstantWinType = iw1,
                        p2InstantWinType = iw2,
                        summaryReason = "Cùng tới trắng ${iw1.viName} (hòa)",
                    )
                }
            }
        }

        if (iw1 != null) {
            val reward = getInstantWinChi(iw1, rules)
            return MauBinhPairwiseComparison(
                p1 = p1,
                p2 = p2,
                netChiForP1 = reward,
                isP1InstantWin = true,
                p1InstantWinType = iw1,
                summaryReason = "Tới trắng: ${iw1.viName} (+${reward} chi)",
            )
        }

        val reward = getInstantWinChi(iw2!!, rules)
        return MauBinhPairwiseComparison(
            p1 = p1,
            p2 = p2,
            netChiForP1 = -reward,
            isP2InstantWin = true,
            p2InstantWinType = iw2,
            summaryReason = "Tới trắng: ${iw2.viName} (−${reward} chi)",
        )
    }

    fun getInstantWinChi(type: MauBinhInstantWinType, rules: MauBinhRules): Int = when (type) {
        MauBinhInstantWinType.DRAGON_ROLL -> rules.instantWinDragonRollChi
        MauBinhInstantWinType.DRAGON_STRAIGHT -> rules.instantWinDragonStraightChi
        MauBinhInstantWinType.FIVE_PAIRS_ONE_TRIPLE -> rules.instantWinFivePairsTripleChi
        MauBinhInstantWinType.SIX_PAIRS -> rules.instantWinSixPairsChi
        MauBinhInstantWinType.THREE_FLUSHES -> rules.instantWinThreeFlushesChi
        MauBinhInstantWinType.THREE_STRAIGHTS -> rules.instantWinThreeStraightsChi
    }
}
