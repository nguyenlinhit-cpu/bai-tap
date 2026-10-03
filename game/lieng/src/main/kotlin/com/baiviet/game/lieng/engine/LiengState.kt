package com.baiviet.game.lieng.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.game.lieng.rules.LiengHand
import com.baiviet.game.lieng.rules.LiengPlayerResult
import com.baiviet.game.lieng.rules.LiengRules
import kotlinx.serialization.Serializable

@Serializable
enum class LiengPhase {
    BETTING,
    FINISHED,
}

/**
 * @param stack xu còn trước mặt (chưa bỏ vào pot)
 * @param invested tổng xu đã bỏ vào pot trong ván (kể cả cược sàn)
 * @param currentRoundBet xu đã bỏ trong vòng tố hiện tại
 */
@Serializable
data class LiengPlayerState(
    val id: PlayerId,
    val cards: List<Card>,
    val invested: Long,
    val stack: Long = 0L,
    val currentRoundBet: Long = 0L,
    val isFolded: Boolean = false,
) {
    val isAllIn: Boolean get() = !isFolded && stack == 0L
}

@Serializable
data class LiengState(
    val tableId: String = "lieng_table",
    val players: List<PlayerId>,
    val currentActor: PlayerId? = null,
    val phase: LiengPhase = LiengPhase.BETTING,
    val playerStates: Map<PlayerId, LiengPlayerState>,
    val currentHighBet: Long = 0L,
    val minRaise: Long = 100L,
    val currentRound: Int = 1,
    val lastBettor: PlayerId? = null,
    val initialMinRaise: Long = 100L,
    val actedThisRound: Set<PlayerId> = emptySet(),
    val raisesThisRound: Int = 0,
    /** Ghế mở vòng tố (đi đầu mỗi vòng). */
    val firstSeat: Int = 0,
    val rules: LiengRules = LiengRules(),
    val results: List<LiengPlayerResult> = emptyList(),
) {
    val pot: Long get() = playerStates.values.sumOf { it.invested }
}

@Serializable
data class LiengPlayerInfo(
    val id: PlayerId,
    val cardCount: Int,
    val cards: List<Card>? = null, // null nếu úp bài
    val hand: LiengHand? = null,
    val invested: Long,
    val currentRoundBet: Long,
    val isFolded: Boolean,
    val isAllIn: Boolean,
    val stack: Long = 0L,
)

@Serializable
data class LiengView(
    val myId: PlayerId,
    val currentActor: PlayerId?,
    val phase: LiengPhase,
    val myHand: LiengHand,
    val players: List<LiengPlayerInfo>,
    val pot: Long,
    val currentHighBet: Long,
    val toCall: Long,
    val minRaise: Long,
    val currentRound: Int,
    val rules: LiengRules,
    val results: List<LiengPlayerResult> = emptyList(),
    val myStack: Long = 0L,
    val raisesThisRound: Int = 0,
)
