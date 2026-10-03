package com.baiviet.game.lieng.engine

import kotlinx.serialization.Serializable

@Serializable
sealed interface LiengAction {
    /** Úp bài — bỏ ván và mất số tiền đã cược */
    @Serializable
    data object Fold : LiengAction

    /** Xem bài — chuyển lượt khi không có ai tố */
    @Serializable
    data object Check : LiengAction

    /** Theo cược — bỏ thêm xu bằng mức cược cao nhất hiện tại */
    @Serializable
    data object Call : LiengAction

    /** Tố thêm — cược thêm một khoản tiền (tối thiểu minRaise, tối đa pot) */
    @Serializable
    data class Raise(val amount: Long) : LiengAction

    /** Tất tay — cược toàn bộ số xu mang vào bàn */
    @Serializable
    data object AllIn : LiengAction
}
