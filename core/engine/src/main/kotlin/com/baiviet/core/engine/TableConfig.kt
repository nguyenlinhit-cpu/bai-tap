package com.baiviet.core.engine

import kotlinx.serialization.Serializable

/**
 * Cấu hình một bàn chơi.
 *
 * @param playerCount số người (2–4)
 * @param rules cấu hình luật cụ thể của game
 * @param betUnit đơn vị cược B (xu)
 * @param minBalanceMultiplier số xu tối thiểu = minBalanceMultiplier × betUnit
 */
@Serializable
data class TableConfig<R : RuleConfig>(
    val playerCount: Int,
    val rules: R,
    val betUnit: Long = 100L,
    val minBalanceMultiplier: Int = 50,
) {
    init {
        require(playerCount in 2..4) { "Số người chơi phải từ 2 đến 4" }
        require(betUnit > 0) { "Đơn vị cược phải > 0" }
    }

    /** Số xu tối thiểu để vào bàn */
    val minBalance: Long get() = minBalanceMultiplier * betUnit
}
