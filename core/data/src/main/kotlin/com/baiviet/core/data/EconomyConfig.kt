package com.baiviet.core.data

/**
 * Cấu hình ví xu. Xu chỉ để giải trí: không nạp, không đổi thưởng, không chuyển.
 */
object EconomyConfig {
    /** Xu tặng khi tạo hồ sơ. */
    const val WELCOME_COINS = 50_000L

    /** Quà đăng nhập 7 ngày, tăng dần (ngày 1 → ngày 7). */
    val DAILY_GIFTS = listOf(2_000L, 3_000L, 5_000L, 7_000L, 10_000L, 15_000L, 25_000L)

    /** Cứu trợ khi số xu < mức tối thiểu của bàn nhỏ nhất. */
    const val RELIEF_COINS = 10_000L
    const val RELIEF_PER_DAY = 3

    /** Các mức cược B. */
    val BET_LEVELS = listOf(100L, 500L, 1_000L, 5_000L, 10_000L)
}
