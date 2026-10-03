package com.baiviet.game.bacay.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.game.bacay.rules.BaCayHand
import com.baiviet.game.bacay.rules.BaCayPlayerResult
import com.baiviet.game.bacay.rules.BaCayRules
import kotlinx.serialization.Serializable

@Serializable
enum class BaCayPhase {
    /** Đang chia bài & nặn bài */
    SQUEEZING,
    /** Đã lật bài tất cả */
    REVEALED,
    /** Đã kết thúc và tính tiền */
    FINISHED,
}

@Serializable
data class BaCayPlayerState(
    val id: PlayerId,
    val cards: List<Card>,
    val betAmount: Long,
    val isRevealed: Boolean = false,
)

@Serializable
data class BaCayState(
    val tableId: String = "bacay_table",
    val players: List<PlayerId>,
    val dealerId: PlayerId,
    val currentActor: PlayerId? = null,
    val phase: BaCayPhase = BaCayPhase.SQUEEZING,
    val playerStates: Map<PlayerId, BaCayPlayerState>,
    val rules: BaCayRules = BaCayRules(),
    val results: List<BaCayPlayerResult> = emptyList(),
)

@Serializable
data class BaCayPlayerInfo(
    val id: PlayerId,
    val cardCount: Int,
    val cards: List<Card>? = null, // null nếu úp
    val hand: BaCayHand? = null,   // null nếu úp
    val betAmount: Long,
    val isRevealed: Boolean,
)

@Serializable
data class BaCayView(
    val myId: PlayerId,
    val dealerId: PlayerId,
    val phase: BaCayPhase,
    val myHand: BaCayHand,
    val players: List<BaCayPlayerInfo>,
    val rules: BaCayRules,
    val results: List<BaCayPlayerResult> = emptyList(),
)
