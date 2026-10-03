package com.baiviet.game.phom.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.SettlementLine
import com.baiviet.game.phom.rules.PhomMeld
import com.baiviet.game.phom.rules.PhomRules
import kotlinx.serialization.Serializable

/**
 * Các pha trong một lượt chơi Phỏm:
 * - DRAW_OR_EAT: Chọn Ăn bài người trước vừa đánh hoặc Bốc bài từ Nọc.
 * - MELD_AND_LAYOFF: Vòng cuối (lượt 4): Hạ phỏm và Gửi bài trước khi đánh cây rác thứ 4.
 * - DISCARD: Chọn 1 lá bài rác hợp lệ (không bị trói) để đánh ra.
 */
@Serializable
enum class PhomPhase {
    DRAW_OR_EAT,
    MELD_AND_LAYOFF,
    DISCARD,
}

/**
 * Hành động của người chơi trong Phỏm.
 */
@Serializable
sealed interface PhomAction {
    /** Bốc 1 lá từ nọc. */
    @Serializable
    data object Draw : PhomAction

    /** Ăn lá vừa đánh của người trước; [meldCards] gồm lá ăn + các lá trên tay tạo thành phỏm. */
    @Serializable
    data class Eat(val meldCards: List<Card>) : PhomAction

    /** Đánh 1 lá rác ra bàn. */
    @Serializable
    data class Discard(val card: Card) : PhomAction

    /** Hạ các phỏm trên tay. */
    @Serializable
    data class Meld(val melds: List<PhomMeld>) : PhomAction

    /**
     * Gửi các lá rác vào phỏm của người chơi khác đã hạ.
     * Key: lá rác trên tay muốn gửi; Value: cặp (ghế người có phỏm, chỉ số phỏm của người đó).
     */
    @Serializable
    data class LayOff(val layOffs: List<Pair<Card, Pair<Int, Int>>>) : PhomAction

    /** Hoàn thành gửi bài hoặc không gửi. */
    @Serializable
    data object PassLayOff : PhomAction

    /** Báo Ù ngay lập tức. */
    @Serializable
    data object DeclareU : PhomAction

    /** Báo Ù khan khi vừa chia bài. */
    @Serializable
    data object DeclareUKhan : PhomAction
}

/**
 * Trạng thái một ván Phỏm (Tá Lả) — bất biến; chuyển trạng thái qua [PhomEngine.apply].
 */
@Serializable
data class PhomState(
    val rules: PhomRules,
    val playerCount: Int,
    val betUnit: Long,
    val seed: Long,
    val isFirstGame: Boolean,
    val leader: Int,
    val turn: Int,
    val phase: PhomPhase,
    val hands: List<List<Card>>,
    val eatenCards: List<Set<Card>> = List(playerCount) { emptySet() },
    val discardPiles: List<List<Card>> = List(playerCount) { emptyList() },
    val stock: List<Card>,
    val lastDiscard: Card? = null,
    val lastDiscarder: Int? = null,
    val exposedMelds: List<List<PhomMeld>> = List(playerCount) { emptyList() },
    val meldOrderCount: Int = 0,
    val playerMeldOrder: List<Int?> = List(playerCount) { null },
    val eatCountMatrix: List<List<Int>> = List(playerCount) { List(playerCount) { 0 } },
    val lastChotEater: Int? = null,
    val uDenSeat: Int? = null,
    val winner: Int? = null,
    val isU: Boolean = false,
    val isUKhan: Boolean = false,
    val finished: Boolean = false,
    val moveCount: Int = 0,
    val eatLines: List<SettlementLine> = emptyList(),
) {
    /** Kiểm tra xem ghế [seat] đã đánh bao nhiêu lá rác. */
    fun discardCount(seat: Int): Int = discardPiles[seat].size

    /** Ghế [seat] đã đến vòng hạ phỏm chưa (chuẩn bị đánh lá thứ 4). */
    fun isFinalRound(seat: Int): Boolean = discardCount(seat) == 3

    /** Ghế [seat] đã hạ phỏm chưa. */
    fun hasMelded(seat: Int): Boolean = playerMeldOrder[seat] != null
}

/**
 * Trạng thái hiển thị mà ghế [me] được phép nhìn thấy (không thấy bài úp trên tay người khác).
 */
@Serializable
data class PhomView(
    val me: Int,
    val playerCount: Int,
    val rules: PhomRules,
    val betUnit: Long,
    val myHand: List<Card>,
    val handCounts: List<Int>,
    val turn: Int,
    val phase: PhomPhase,
    val eatenCards: List<Set<Card>>,
    val discardPiles: List<List<Card>>,
    val stockCount: Int,
    val lastDiscard: Card?,
    val lastDiscarder: Int?,
    val exposedMelds: List<List<PhomMeld>>,
    val winner: Int?,
    val isU: Boolean,
    val isUKhan: Boolean,
    val finished: Boolean,
) {
    val opponents: List<Int> get() = (0 until playerCount).filter { it != me }
}
