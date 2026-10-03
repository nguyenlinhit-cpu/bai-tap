package com.baiviet.core.ai

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

/**
 * Đánh giá các phương án bằng mô phỏng Monte Carlo, có giới hạn thời gian và hủy được.
 *
 * Các phương án được mô phỏng xoay vòng để mỗi phương án có số lần thử gần bằng nhau.
 */
object MonteCarlo {

    /**
     * @param candidates các phương án (≥ 1)
     * @param timeBudgetMs giới hạn thời gian (mặc định 800ms theo đặc tả)
     * @param maxIterations tổng số lần mô phỏng tối đa
     * @param simulate chạy một lần mô phỏng cho phương án, trả về điểm (càng cao càng tốt)
     * @return phương án có điểm trung bình cao nhất
     */
    suspend fun <T> best(
        candidates: List<T>,
        random: Random,
        timeBudgetMs: Long = 800L,
        maxIterations: Int = 4_000,
        simulate: (T, Random) -> Double,
    ): T {
        require(candidates.isNotEmpty())
        if (candidates.size == 1) return candidates.first()
        val totals = DoubleArray(candidates.size)
        val counts = IntArray(candidates.size)
        val deadline = System.nanoTime() + timeBudgetMs * 1_000_000
        var iter = 0
        while (iter < maxIterations && System.nanoTime() < deadline) {
            coroutineContext.ensureActive()
            val idx = iter % candidates.size
            totals[idx] += simulate(candidates[idx], random)
            counts[idx]++
            iter++
        }
        val best = candidates.indices.maxBy { if (counts[it] == 0) Double.NEGATIVE_INFINITY else totals[it] / counts[it] }
        return candidates[best]
    }

    /**
     * Chia ngẫu nhiên [pool] cho các ghế theo số lá [counts] (lấy mẫu bài ẩn).
     * Phần còn lại của pool bị bỏ (bài úp không ai dùng).
     */
    fun <C> deal(pool: List<C>, counts: List<Int>, random: Random): List<List<C>> {
        val shuffled = pool.shuffled(random)
        var idx = 0
        return counts.map { n -> shuffled.subList(idx, idx + n).also { idx += n } }
    }
}
