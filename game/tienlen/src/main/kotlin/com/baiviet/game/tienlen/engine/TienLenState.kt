package com.baiviet.game.tienlen.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.SettlementLine
import com.baiviet.game.tienlen.rules.Combo
import com.baiviet.game.tienlen.rules.InstantWinType
import com.baiviet.game.tienlen.rules.TienLenRules
import kotlinx.serialization.Serializable

/** Hành động trong Tiến lên. */
@Serializable
sealed interface TlAction {
    @Serializable
    data class Play(val cards: List<Card>) : TlAction

    @Serializable
    data object Pass : TlAction
}

/**
 * Chuỗi chặt đang mở: [victim] (người bị chặt cuối cùng) sẽ trả [amount] xu cho [cutter]
 * khi chuỗi kết thúc (hết vòng hoặc hết ván).
 */
@Serializable
data class CutChain(
    val cutter: Int,
    val victim: Int,
    val amount: Long,
    val description: String,
)

@Serializable
data class InstantWinResult(val player: Int, val type: InstantWinType)

/**
 * Trạng thái một ván Tiến lên — bất biến; mọi thay đổi qua [TienLenEngine.apply].
 *
 * Ghế đánh số 0..n-1; đi theo thứ tự tăng dần (ngược chiều kim đồng hồ trên màn hình).
 *
 * @param hands bài trên tay từng ghế (đã sắp)
 * @param turn ghế đang đến lượt
 * @param top bộ đang nằm trên bàn (null = đi tự do)
 * @param topOwner ghế đã đánh [top]
 * @param topIsCut [top] là một nước chặt (đang trong chuỗi chặt)
 * @param passed các ghế đã bỏ lượt trong vòng này
 * @param declined các ghế đã bỏ quyền chặt không cần vòng đối với [top] hiện tại
 * @param mustInclude lá bắt buộc có trong nước đầu tiên (3♠ ván đầu)
 * @param finishOrder thứ tự các ghế đã hết bài
 * @param hasPlayed các ghế đã đánh ít nhất một lá (để xét cóng)
 * @param cong các ghế bị cóng (chế độ Xếp hạng: bị loại khỏi ván)
 * @param cutLines các khoản chặt đã chốt
 * @param playedCards mọi lá đã lộ trên bàn
 */
@Serializable
data class TienLenState(
    val rules: TienLenRules,
    val playerCount: Int,
    val betUnit: Long,
    val seed: Long,
    val isFirstGame: Boolean,
    val hands: List<List<Card>>,
    val turn: Int,
    val top: Combo? = null,
    val topOwner: Int? = null,
    val topIsCut: Boolean = false,
    val passed: Set<Int> = emptySet(),
    val declined: Set<Int> = emptySet(),
    val mustInclude: Card? = null,
    val finishOrder: List<Int> = emptyList(),
    val hasPlayed: Set<Int> = emptySet(),
    val cong: Set<Int> = emptySet(),
    val cutChain: CutChain? = null,
    val cutLines: List<SettlementLine> = emptyList(),
    val instantWin: InstantWinResult? = null,
    val playedCards: List<Card> = emptyList(),
    val finished: Boolean = false,
    val moveCount: Int = 0,
) {
    /** Ghế còn đang chơi (còn bài, không bị cóng loại). */
    fun isActive(seat: Int): Boolean = hands[seat].isNotEmpty() && seat !in cong

    val activeSeats: List<Int> get() = (0 until playerCount).filter { isActive(it) }
}

/**
 * Phần trạng thái một người được phép thấy. KHÔNG chứa bài trên tay người khác.
 */
@Serializable
data class TienLenView(
    val me: Int,
    val playerCount: Int,
    val rules: TienLenRules,
    val betUnit: Long,
    val isFirstGame: Boolean,
    val myHand: List<Card>,
    val handCounts: List<Int>,
    val turn: Int,
    val top: Combo?,
    val topOwner: Int?,
    val topIsCut: Boolean,
    val passed: Set<Int>,
    val declined: Set<Int>,
    val mustInclude: Card?,
    val finishOrder: List<Int>,
    val hasPlayed: Set<Int>,
    val cong: Set<Int>,
    val playedCards: List<Card>,
    val cutAmount: Long,
    val cutVictim: Int?,
    val finished: Boolean,
) {
    fun isActive(seat: Int): Boolean = handCounts[seat] > 0 && seat !in cong

    /** Đối thủ còn chơi. */
    val opponents: List<Int> get() = (0 until playerCount).filter { it != me && isActive(it) }
}
