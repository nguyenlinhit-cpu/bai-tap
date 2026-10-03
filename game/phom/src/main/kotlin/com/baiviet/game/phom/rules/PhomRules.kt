package com.baiviet.game.phom.rules

import com.baiviet.core.engine.RuleConfig
import kotlinx.serialization.Serializable

/**
 * Cấu hình luật Phỏm (Tá Lả). Mọi con số của luật được tập trung ở đây.
 *
 * Đơn vị: "lá" (1 lá = 1B xu cược bàn).
 */
@Serializable
data class PhomRules(
    // ── Biến thể Luật nhà ──
    /** Ù khan: bài chia ra không có phỏm và không có cạ nào → được báo Ù khan ngay (mặc định bật). */
    val uKhanEnabled: Boolean = true,

    /** Tiền ăn tăng dần: ăn lá thứ nhất 1B, lá thứ hai 2B, lá thứ ba 3B (mặc định tắt: mọi lá thường đều 1B). */
    val progressiveEatPenalty: Boolean = false,

    /** Ăn chốt đền: người ăn chốt sau cùng phải đền thay cả làng nếu có người Ù trong vòng cuối (mặc định bật). */
    val eatChotDenEnabled: Boolean = true,

    /** Người móm (không có phỏm để hạ) vẫn được gửi bài vào phỏm người khác (mặc định tắt). */
    val momCanLayOff: Boolean = false,

    // ── Tiền phạt ăn bài & chốt ──
    val normalEatCards: Int = 1,
    val secondEatCards: Int = 2,
    val thirdEatCards: Int = 3,
    val chotEatCards: Int = 4,

    // ── Tiền thưởng / phạt kết thúc ──
    val rank2PayCards: Int = 1, // Nhì trả Nhất
    val rank3PayCards: Int = 2, // Ba trả Nhất
    val rank4PayCards: Int = 3, // Bét trả Nhất
    val momPayCards: Int = 4,   // Móm trả Nhất
    val uWinCardsPerPlayer: Int = 5, // Mỗi người thua trả người Ù

    // ── Thời gian & bàn ──
    val turnSeconds: Int = 25,
    val minBalanceMultiplier: Int = 30,
) : RuleConfig {

    companion object {
        const val GAME_ID = "phom"
        val DEFAULT = PhomRules()
    }
}
