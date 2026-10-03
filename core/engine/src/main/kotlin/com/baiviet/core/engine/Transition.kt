package com.baiviet.core.engine

/**
 * Kết quả chuyển trạng thái khi áp dụng một hành động.
 *
 * @param state trạng thái mới (bất biến)
 * @param events danh sách sự kiện phát sinh — UI tiêu thụ tuần tự
 */
data class Transition<S>(
    val state: S,
    val events: List<GameEvent>,
)
