package com.baiviet.game.samloc.rules

import com.baiviet.core.engine.RuleConfig
import kotlinx.serialization.Serializable

/**
 * Cấu hình luật Sâm Lốc. Mọi con số của luật nằm ở đây — chỉnh trong màn Luật nhà.
 *
 * Đơn vị: "lá" (1 lá = 1B).
 */
@Serializable
data class SamLocRules(
    // ── Chặt ──
    /** Tứ quý chặt được đôi heo (mặc định tắt). */
    val quadCutsPairOfTwos: Boolean = false,
    // ── Ăn trắng ──
    /** 3 sám ăn trắng (mặc định tắt). */
    val threeTriplesInstantWin: Boolean = false,
    // ── Phạt heo ──
    /** Phân biệt heo đen 10 lá / heo đỏ 15 lá (mặc định tắt: mọi heo đều 15 lá). */
    val differentiateTwoColors: Boolean = false,
    val blackTwoCards: Int = 10,
    val redTwoCards: Int = 15,
    val defaultTwoCards: Int = 15,
    // ── Các khoản phạt khác (đơn vị: lá, 1 lá = 1B) ──
    val quadPenaltyCards: Int = 15,
    val congCards: Int = 15,
    val finishWithTwoPenaltyCards: Int = 15,
    val cutCards: Int = 15,
    val instantWinCards: Int = 20,
    val samWinCards: Int = 20,
    val samFailPenaltyCards: Int = 20,
    // ── Bàn & thời gian ──
    val samPhaseSeconds: Int = 8,
    val turnSeconds: Int = 20,
    val minBalanceMultiplier: Int = 30,
) : RuleConfig {
    companion object {
        const val GAME_ID = "samloc"
        val DEFAULT = SamLocRules()
    }
}
