package com.baiviet.game.xidach.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.game.xidach.rules.XiDachHand
import com.baiviet.game.xidach.rules.XiDachPlayerResult
import com.baiviet.game.xidach.rules.XiDachRules
import kotlinx.serialization.Serializable

@Serializable
enum class XiDachPhase {
    /** Kiểm tra Xì bàng / Xì dách đầu ván */
    INITIAL_CHECK,
    /** Lần lượt các nhà con rút hoặc dằn */
    PLAYER_TURNS,
    /** Lượt nhà cái rút và xét bài */
    DEALER_TURN,
    /** Ván bài đã kết thúc, tính tiền */
    FINISHED,
}

@Serializable
enum class XiDachPlayerStatus {
    WAITING,
    PLAYING,
    STOOD,
    BUST,
    RESOLVED,
}

@Serializable
data class XiDachPlayerState(
    val id: PlayerId,
    val cards: List<Card>,
    val betAmount: Long,
    val status: XiDachPlayerStatus = XiDachPlayerStatus.WAITING,
    val isRevealed: Boolean = false,
)

@Serializable
data class XiDachState(
    val tableId: String = "xidach_table",
    val players: List<PlayerId>,
    val dealerId: PlayerId,
    val currentActor: PlayerId? = null,
    val phase: XiDachPhase = XiDachPhase.INITIAL_CHECK,
    val playerStates: Map<PlayerId, XiDachPlayerState>,
    val remainingDeck: List<Card>,
    val rules: XiDachRules = XiDachRules(),
    val results: List<XiDachPlayerResult> = emptyList(),
)

@Serializable
data class XiDachPlayerInfo(
    val id: PlayerId,
    val cardCount: Int,
    val cards: List<Card>? = null, // null nếu úp
    val hand: XiDachHand? = null,   // null nếu úp
    val betAmount: Long,
    val status: XiDachPlayerStatus,
    val isRevealed: Boolean,
)

@Serializable
data class XiDachView(
    val myId: PlayerId,
    val dealerId: PlayerId,
    val currentActor: PlayerId?,
    val phase: XiDachPhase,
    val myHand: XiDachHand,
    val players: List<XiDachPlayerInfo>,
    val rules: XiDachRules,
    val results: List<XiDachPlayerResult> = emptyList(),
)
