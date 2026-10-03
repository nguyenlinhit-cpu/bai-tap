package com.baiviet.game.lieng.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import kotlinx.serialization.Serializable

@Serializable
enum class LiengHandType {
    DIEM,
    ANH,
    LIENG,
    SAP,
}

@Serializable
data class LiengHand(
    val cards: List<Card>,
    val type: LiengHandType,
    val points: Int, // 0..9 nếu là DIEM
    val highestRank: Rank, // Để so thứ tự dây liêng hoặc sáp
    val strongestCard: Card,
) {
    fun description(): String = when (type) {
        LiengHandType.SAP -> "Sáp ${highestRank.label}"
        LiengHandType.LIENG -> "Liêng (${cards.map { it.rank.label }.joinToString("-")})"
        LiengHandType.ANH -> "Ảnh (3 Tây)"
        LiengHandType.DIEM -> "$points điểm"
    }
}
