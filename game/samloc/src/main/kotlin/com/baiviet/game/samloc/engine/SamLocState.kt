package com.baiviet.game.samloc.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.SettlementLine
import com.baiviet.game.samloc.rules.SamLocCombo
import com.baiviet.game.samloc.rules.SamLocInstantWinType
import com.baiviet.game.samloc.rules.SamLocRules
import kotlinx.serialization.Serializable

/** Giai đoạn trong ván Sâm Lốc. */
@Serializable
enum class SlPhase {
    /** Pha hỏi Báo Sâm (mỗi người có lượt chọn Báo Sâm / Bỏ qua). */
    BAO_SAM,

    /** Pha đánh bài chính (hoặc người Báo Sâm đang đánh). */
    PLAYING,
}

/** Hành động trong Sâm Lốc. */
@Serializable
sealed interface SlAction {
    @Serializable
    data object CallSam : SlAction

    @Serializable
    data object SkipSam : SlAction

    @Serializable
    data class Play(
        val cards: List<Card>,
    ) : SlAction

    @Serializable
    data object Pass : SlAction
}

/**
 * Chuỗi chặt đang mở: [victim] sẽ trả [amount] xu cho [cutter]
 * khi chuỗi kết thúc (hết vòng hoặc hết ván).
 */
@Serializable
data class SlCutChain(
    val cutter: Int,
    val victim: Int,
    val amount: Long,
    val description: String,
)

@Serializable
data class SlInstantWinResult(
    val player: Int,
    val type: SamLocInstantWinType,
)

/**
 * Trạng thái một ván Sâm Lốc — bất biến; mọi thay đổi qua [SamLocEngine.apply].
 */
@Serializable
data class SamLocState(
    val rules: SamLocRules,
    val playerCount: Int,
    val betUnit: Long,
    val seed: Long,
    val isFirstGame: Boolean,
    val hands: List<List<Card>>,
    val phase: SlPhase = SlPhase.BAO_SAM,
    val nominalLeader: Int,
    val turn: Int,
    val samCaller: Int? = null,
    val samBeaten: Boolean = false,
    val top: SamLocCombo? = null,
    val topOwner: Int? = null,
    val passed: Set<Int> = emptySet(),
    val baoMot: Set<Int> = emptySet(),
    val violatorOfBao1: Int? = null,
    val hasPlayed: Set<Int> = emptySet(),
    val cutChain: SlCutChain? = null,
    val cutLines: List<SettlementLine> = emptyList(),
    val instantWin: SlInstantWinResult? = null,
    val finishWithTwoPlayer: Int? = null,
    val winner: Int? = null,
    val playedCards: List<Card> = emptyList(),
    val finished: Boolean = false,
    val moveCount: Int = 0,
) {
    fun isActive(seat: Int): Boolean = hands[seat].isNotEmpty()

    val activeSeats: List<Int> get() = (0 until playerCount).filter { isActive(it) }
}

/**
 * Phần trạng thái một người được phép thấy. KHÔNG chứa bài trên tay người khác.
 */
@Serializable
data class SamLocView(
    val me: Int,
    val playerCount: Int,
    val rules: SamLocRules,
    val betUnit: Long,
    val myHand: List<Card>,
    val handCounts: List<Int>,
    val phase: SlPhase,
    val turn: Int,
    val samCaller: Int?,
    val top: SamLocCombo?,
    val topOwner: Int?,
    val passed: Set<Int>,
    val baoMot: Set<Int>,
    val instantWin: SlInstantWinResult?,
    val finishWithTwoPlayer: Int?,
    val winner: Int?,
    val finished: Boolean,
    val playedCards: List<Card>,
) {
    val opponents: List<Int> get() = (0 until playerCount).filter { it != me }
}
