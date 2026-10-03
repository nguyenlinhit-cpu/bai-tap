package com.baiviet.core.ai

import kotlin.random.Random

/** Tốc độ bot trong Cài đặt. */
enum class BotSpeed(val factor: Double) {
    SLOW(1.5),
    NORMAL(1.0),
    FAST(0.45),
}

/**
 * Thời gian "suy nghĩ" giả lập của bot: 0.6–1.8 giây ngẫu nhiên, nhân hệ số tốc độ.
 */
object ThinkTime {
    const val MIN_MS = 600L
    const val MAX_MS = 1800L

    fun pick(speed: BotSpeed, random: Random = Random.Default): Long =
        (random.nextLong(MIN_MS, MAX_MS + 1) * speed.factor).toLong()
}
