package com.baiviet.game.bacay.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import kotlinx.serialization.Serializable

@Serializable
enum class BaCayHandType {
    POINTS,
    BA_TIEN,
    SAP,
}

@Serializable
data class BaCayHand(
    val cards: List<Card>,
    val type: BaCayHandType,
    val points: Int, // 0..9 nếu là POINTS
    val strongestCard: Card,
) {
    fun description(): String = when (type) {
        BaCayHandType.SAP -> "Sáp ${cards.first().rank.label}"
        BaCayHandType.BA_TIEN -> "Ba tiên (Ba cào)"
        BaCayHandType.POINTS -> if (points == 0) "Bù (0 nút)" else "$points nút"
    }
}
