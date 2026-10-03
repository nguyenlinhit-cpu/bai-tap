package com.baiviet.core.ai

/**
 * Hợp đồng Bot — mỗi game implement riêng.
 *
 * @param V kiểu view (chỉ thông tin bot được phép thấy)
 * @param A kiểu hành động
 */
interface Bot<V, A> {
    /**
     * Quyết định hành động dựa trên view và danh sách hành động hợp lệ.
     *
     * Chạy trên [kotlinx.coroutines.Dispatchers.Default], có thể bị hủy.
     * Bot Khó: giới hạn ≤ 800ms cho Monte Carlo.
     *
     * **Không bao giờ** được truy cập bài úp — chỉ dùng [view].
     */
    suspend fun decide(view: V, legal: List<A>, level: BotLevel): A
}
