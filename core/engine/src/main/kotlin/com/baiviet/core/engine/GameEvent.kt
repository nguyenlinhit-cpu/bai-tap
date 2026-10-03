package com.baiviet.core.engine

import com.baiviet.core.cards.Card

/**
 * Sự kiện phát ra từ engine — UI tiêu thụ hàng đợi sự kiện
 * và chạy animation tuần tự.
 *
 * Các sự kiện ở đây là chung cho mọi game; dữ liệu riêng từng game
 * truyền qua [description] hoặc danh sách lá bài.
 */
sealed interface GameEvent {
    /** Chia bài xong */
    data class Dealt(val playerHands: Map<PlayerId, Int>) : GameEvent

    /** Đánh bài */
    data class Played(
        val player: PlayerId,
        val cards: List<Card> = emptyList(),
        val description: String = "",
    ) : GameEvent

    /** Bỏ lượt */
    data class Passed(val player: PlayerId) : GameEvent

    /**
     * Chặt (heo, hàng).
     *
     * @param amount giá trị phần bị chặt tính tới thời điểm này (xu), đã cộng dồn nếu chặt chồng
     */
    data class Cut(
        val cutter: PlayerId,
        val victim: PlayerId,
        val amount: Long,
        val description: String,
    ) : GameEvent

    /** Vòng mới — [leader] được đi tự do */
    data class RoundStarted(val leader: PlayerId) : GameEvent

    /** Một người đã hết bài, về hạng [rank] (1 = Nhất) */
    data class Finished(val player: PlayerId, val rank: Int) : GameEvent

    /** Bốc bài từ nọc */
    data class Drew(val player: PlayerId) : GameEvent

    /** Ăn bài */
    data class Ate(val player: PlayerId, val from: PlayerId, val description: String) : GameEvent

    /** Hạ phỏm / xếp bài */
    data class Melded(val player: PlayerId, val description: String) : GameEvent

    /** Lật/tiết lộ bài */
    data class Revealed(val player: PlayerId, val description: String) : GameEvent

    /** Đặt cược / tố / theo */
    data class Bet(val player: PlayerId, val amount: Long, val action: String) : GameEvent

    /** Thắng pot */
    data class PotWon(val player: PlayerId, val amount: Long, val reason: String) : GameEvent

    /** Thông báo đặc biệt (báo Sâm, báo 1, tới trắng, Ù...) */
    data class Announced(val player: PlayerId, val message: String) : GameEvent

    /** Kết thúc ván — chuyển sang màn kết quả */
    data class Settled(val settlement: Settlement) : GameEvent
}
