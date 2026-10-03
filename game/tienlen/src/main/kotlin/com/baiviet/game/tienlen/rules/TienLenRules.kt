package com.baiviet.game.tienlen.rules

import com.baiviet.core.engine.RuleConfig
import kotlinx.serialization.Serializable

/** Cách tính tiền cuối ván. */
@Serializable
enum class TlScoring {
    /** Đếm lá: ván dừng khi có người về Nhất, mỗi lá còn lại = 1B. */
    COUNT_CARDS,

    /** Xếp hạng: chơi tới khi còn 1 người, trả tiền theo thứ hạng. */
    RANKING,
}

/**
 * Luật Tiến lên miền Nam. Mọi con số của luật nằm ở đây — chỉnh trong màn Luật nhà.
 *
 * Đơn vị:
 * - Chế độ [TlScoring.COUNT_CARDS]: các trường `*Cards` tính bằng "lá" (1 lá = 1B).
 * - Chế độ [TlScoring.RANKING]: các trường `ranking*Halves` tính bằng nửa B (1 = ½B).
 */
@Serializable
data class TienLenRules(
    val scoring: TlScoring = TlScoring.COUNT_CARDS,

    // ── Đi đầu ──
    /** Ván đầu: người giữ 3♠ (hoặc lá nhỏ nhất) phải đánh lá đó ở nước đầu tiên. */
    val require3SpadesOnFirstMove: Boolean = true,

    // ── Chặt ──
    /** 4 đôi thông được chặt cả khi đã bỏ lượt trong vòng. */
    val fourPairsCutWithoutTurn: Boolean = true,
    /** Tứ quý chặt được đôi heo. */
    val quadCutsPairOfTwos: Boolean = true,

    // ── Tới trắng ──
    /** Tứ quý 3 tới trắng (chỉ ván đầu tiên). */
    val quadThreeInstantWinFirstGame: Boolean = true,
    /** Số lá đồng màu để tới trắng (12 hoặc 13). */
    val sameColorInstantWinCount: Int = 13,
    /** 5 đôi thông tới trắng. */
    val fiveConsecutivePairsInstantWin: Boolean = true,
    /** Tới trắng: người thua trả thêm tiền thối heo/hàng. */
    val instantWinCountsPenalties: Boolean = false,

    // ── Về ──
    /** Cấm về (đánh lá cuối) bằng bộ có heo. */
    val forbidFinishWithTwo: Boolean = false,

    // ── Đếm lá (đơn vị: lá) ──
    val blackTwoCards: Int = 3,
    val redTwoCards: Int = 6,
    val threePairsCards: Int = 9,
    val quadCards: Int = 12,
    val fourPairsCards: Int = 18,
    val congCards: Int = 26,
    val instantWinCards: Int = 26,

    // ── Xếp hạng (đơn vị: nửa B) ──
    val rankingBlackTwoHalves: Int = 1,
    val rankingRedTwoHalves: Int = 2,
    val rankingThreePairsHalves: Int = 3,
    val rankingQuadHalves: Int = 6,
    val rankingFourPairsHalves: Int = 6,
    val rankingCongHalves: Int = 4,
    val rankingInstantWinHalves: Int = 4,
    /** Tiền theo hạng (đơn vị B), Nhất → Bét. */
    val rankingPayout4: List<Int> = listOf(2, 1, -1, -2),
    val rankingPayout3: List<Int> = listOf(1, 0, -1),
    val rankingPayout2: List<Int> = listOf(1, -1),

    // ── Bàn ──
    val turnSeconds: Int = 20,
    /** Số xu tối thiểu vào bàn = minBalanceMultiplier × B. */
    val minBalanceMultiplier: Int = 30,
) : RuleConfig {

    /** Bảng tiền theo hạng cho [playerCount] người. */
    fun rankingPayout(playerCount: Int): List<Int> = when (playerCount) {
        2 -> rankingPayout2
        3 -> rankingPayout3
        else -> rankingPayout4
    }

    companion object {
        const val GAME_ID = "tienlen"
        val DEFAULT = TienLenRules()
    }
}
