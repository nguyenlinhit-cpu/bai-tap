package com.baiviet.game.xidach.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import kotlinx.serialization.Serializable

/**
 * Loại bài trong Xì Dách (xếp theo thứ tự sức mạnh).
 */
@Serializable
enum class XiDachHandType {
    NORMAL,
    NGU_LINH,
    XI_DACH,
    XI_BANG,
}

/**
 * Trạng thái điểm số của tay bài.
 */
@Serializable
enum class XiDachHandStatus {
    /** Non: chưa đủ tuổi (< 16 với con, < 15 với cái) */
    NON,
    /** Đủ tuổi: 16–21 với con, 15–21 với cái */
    DU_TUOI,
    /** Quắc: > 21 điểm */
    QUAC,
}

/**
 * Thông tin chi tiết một tay bài Xì Dách đã được đánh giá.
 *
 * @param cards danh sách lá bài
 * @param score điểm tối ưu (≤ 21 cao nhất, hoặc quắc nhỏ nhất)
 * @param type loại bài (XI_BANG, XI_DACH, NGU_LINH, NORMAL)
 * @param isDealer có phải là nhà cái không (để tính mốc đủ tuổi 15 hay 16)
 */
@Serializable
data class XiDachHand(
    val cards: List<Card>,
    val score: Int,
    val type: XiDachHandType,
    val isDealer: Boolean,
) {
    val status: XiDachHandStatus get() = when {
        score > 21 -> XiDachHandStatus.QUAC
        type == XiDachHandType.XI_BANG || type == XiDachHandType.XI_DACH || type == XiDachHandType.NGU_LINH -> XiDachHandStatus.DU_TUOI
        score < (if (isDealer) 15 else 16) -> XiDachHandStatus.NON
        else -> XiDachHandStatus.DU_TUOI
    }

    val isBust: Boolean get() = score > 21

    val isSpecial: Boolean get() = type != XiDachHandType.NORMAL

    /** Mô tả ngắn gọn tiếng Việt */
    fun description(): String = when (type) {
        XiDachHandType.XI_BANG -> "Xì bàng"
        XiDachHandType.XI_DACH -> "Xì dách"
        XiDachHandType.NGU_LINH -> "Ngũ linh ($score điểm)"
        XiDachHandType.NORMAL -> when {
            isBust -> "Quắc ($score điểm)"
            status == XiDachHandStatus.NON -> "Non ($score điểm)"
            else -> "$score điểm"
        }
    }
}
