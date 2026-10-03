package com.baiviet.game.bacay.rules

import com.baiviet.core.engine.RuleConfig
import kotlinx.serialization.Serializable

@Serializable
enum class BaCayMode {
    /** Chơi có nhà cái — các con so điểm trực tiếp với cái */
    WITH_DEALER,
    /** Ăn tất (nhất ăn tất) — mọi người cược vào pot, bài cao nhất ăn cả */
    WINNER_TAKES_ALL,
}

/**
 * Cấu hình luật chơi Ba Cây (Cào).
 *
 * @param gameMode Chế độ chơi: có cái hoặc nhất ăn tất.
 * @param sapBeatsBaTien Nếu true: Sáp (3 lá cùng số) đứng trên Ba Tiên. Mặc định false (Ba Tiên cao nhất).
 * @param baTienPaysDouble Nếu true: Thắng bằng Ba Tiên nhận gấp đôi tiền cược (×2). Mặc định false.
 * @param tieIsPush Nếu true: Bằng điểm thì hòa tiền. Mặc định false (so lá bài lớn nhất: Rô > Cơ > Chuồn > Bích).
 * @param turnSeconds Thời gian mỗi lượt nặn bài (mặc định: 15 giây).
 */
@Serializable
data class BaCayRules(
    val gameMode: BaCayMode = BaCayMode.WITH_DEALER,
    val sapBeatsBaTien: Boolean = false,
    val baTienPaysDouble: Boolean = false,
    val tieIsPush: Boolean = false,
    val turnSeconds: Int = 15,
) : RuleConfig {
    val minBalanceMultiplier: Long = 20L

    companion object {
        const val GAME_ID = "bacay"
        val DEFAULT = BaCayRules()
    }
}
