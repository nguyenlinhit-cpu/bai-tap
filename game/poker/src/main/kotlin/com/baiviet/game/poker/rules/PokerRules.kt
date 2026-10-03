package com.baiviet.game.poker.rules

import com.baiviet.core.engine.RuleConfig
import kotlinx.serialization.Serializable

/**
 * Cấu hình luật chơi Poker Texas Hold'em (No-Limit).
 *
 * @param startingChipsBB Số BB mặc định khi buy-in (50, 100, 200 BB, mặc định: 100 BB).
 * @param turnSeconds Thời gian mỗi lượt hành động (mặc định: 15 giây).
 * @param showWinRateInPractice Hiển thị % cơ hội thắng (chế độ luyện tập, mặc định: false).
 */
@Serializable
data class PokerRules(
    val startingChipsBB: Int = 100,
    val turnSeconds: Int = 15,
    val showWinRateInPractice: Boolean = false,
) : RuleConfig {
    val minBalanceMultiplier: Long = startingChipsBB.toLong()

    companion object {
        const val GAME_ID = "poker"
        val DEFAULT = PokerRules()
    }
}
