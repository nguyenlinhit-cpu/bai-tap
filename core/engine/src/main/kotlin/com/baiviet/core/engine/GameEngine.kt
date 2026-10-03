package com.baiviet.core.engine

/**
 * Hợp đồng lõi cho engine luật — Kotlin thuần, bất biến.
 *
 * @param S trạng thái ván bài (data class bất biến)
 * @param A hành động người chơi (sealed class/enum)
 * @param V phần trạng thái người chơi được phép thấy (view)
 * @param R cấu hình luật cụ thể
 */
interface GameEngine<S : Any, A : Any, V : Any, R : RuleConfig> {

    /**
     * Bắt đầu ván mới.
     *
     * @param table cấu hình bàn (số người, luật, mức cược)
     * @param seed RNG seed — lưu để replay/debug
     * @return trạng thái khởi đầu
     */
    fun start(table: TableConfig<R>, seed: Long): S

    /**
     * Danh sách người đến lượt.
     * Thường 1 người; Mậu binh: nhiều người xếp bài cùng lúc.
     */
    fun currentActors(state: S): Set<PlayerId>

    /**
     * Các hành động hợp lệ cho [player] ở trạng thái hiện tại.
     * Rỗng nếu không phải lượt của player.
     */
    fun legalActions(state: S, player: PlayerId): List<A>

    /**
     * Áp dụng hành động — trả về trạng thái mới + sự kiện.
     *
     * @throws IllegalActionException nếu hành động không hợp lệ
     */
    fun apply(state: S, player: PlayerId, action: A): Transition<S>

    /** Ván đã kết thúc? */
    fun isFinished(state: S): Boolean

    /**
     * Thanh toán cuối ván — tổng deltas = 0 (zero-sum).
     * Gọi khi [isFinished] = true.
     */
    fun settle(state: S): Settlement

    /**
     * Phần trạng thái mà [player] được phép thấy.
     * Bot CHỈ nhận view này — không bao giờ thấy bài úp người khác.
     */
    fun viewOf(state: S, player: PlayerId): V
}

/** Ném khi hành động không hợp lệ */
class IllegalActionException(message: String) : IllegalStateException(message)
