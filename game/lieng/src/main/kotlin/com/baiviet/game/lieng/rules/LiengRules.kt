package com.baiviet.game.lieng.rules

import com.baiviet.core.engine.RuleConfig
import kotlinx.serialization.Serializable

/**
 * Cấu hình luật chơi Liêng.
 *
 * @param suitOrderCoOverRo Nếu true: ♥ Cơ > ♦ Rô > ♣ Chuồn > ♠ Bích. Mặc định false: ♦ Rô > ♥ Cơ > ♣ Chuồn > ♠ Bích.
 * @param rankBeforeSuit Nếu true: So số trước (A > K > ... > 2), cùng số mới so chất. Mặc định false: So chất trước.
 * @param liengTieIsPush Nếu true: Cùng dây Liêng thì chia đều pot. Mặc định false: So chất lá cao nhất.
 * @param maxBettingRounds Số vòng tố tối đa (mặc định: 3 vòng).
 * @param maxRaisesPerRound Số lần tố (kể cả tất tay vượt mức) tối đa trong một vòng — chống tố qua tố lại vô hạn.
 * @param turnSeconds Thời gian mỗi lượt tố (mặc định: 15 giây).
 * @param stackMultiplier Số xu mang vào ván tối đa = stackMultiplier × B (phần còn lại của ví không bị đem ra cược).
 */
@Serializable
data class LiengRules(
    val suitOrderCoOverRo: Boolean = false,
    val rankBeforeSuit: Boolean = false,
    val liengTieIsPush: Boolean = false,
    val maxBettingRounds: Int = 3,
    val maxRaisesPerRound: Int = 4,
    val turnSeconds: Int = 15,
    val stackMultiplier: Int = 100,
) : RuleConfig {
    val minBalanceMultiplier: Long = 30L

    companion object {
        const val GAME_ID = "lieng"
        val DEFAULT = LiengRules()
    }
}
