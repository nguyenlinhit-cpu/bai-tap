package com.baiviet.game.xidach.rules

import com.baiviet.core.engine.RuleConfig
import kotlinx.serialization.Serializable

/**
 * Cấu hình luật chơi Xì Dách (Việt Nam).
 *
 * @param bothBustPlayerLoses Nếu true: khi cả con và cái đều quắc, con luôn thua. Nếu false (chuẩn): hòa tiền.
 * @param winnerBecomesDealer Nếu true: người có Xì bàng hoặc Xì dách sẽ làm cái ván sau.
 * @param dealerMinScore Điểm tối thiểu cái được quyền xét bài (chuẩn: 15).
 * @param playerMinScore Điểm tối thiểu con được quyền dằn bài (chuẩn: 16).
 * @param turnSeconds Thời gian mỗi lượt (mặc định: 15 giây).
 * @param xiBangMultiplier Hệ số thắng Xì bàng (mặc định: 3x).
 * @param xiDachMultiplier Hệ số thắng Xì dách (mặc định: 2x).
 * @param nguLinhMultiplier Hệ số thắng Ngũ linh (mặc định: 2x).
 */
@Serializable
data class XiDachRules(
    val bothBustPlayerLoses: Boolean = false,
    val winnerBecomesDealer: Boolean = false,
    val dealerMinScore: Int = 15,
    val playerMinScore: Int = 16,
    val turnSeconds: Int = 15,
    val xiBangMultiplier: Int = 3,
    val xiDachMultiplier: Int = 2,
    val nguLinhMultiplier: Int = 2,
) : RuleConfig {
    val minBalanceMultiplier: Long = 30L

    companion object {
        const val GAME_ID = "xidach"
        val DEFAULT = XiDachRules()
    }
}
