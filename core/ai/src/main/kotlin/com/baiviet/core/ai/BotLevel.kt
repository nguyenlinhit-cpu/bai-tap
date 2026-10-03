package com.baiviet.core.ai

/**
 * 3 mức độ khó bot.
 *
 * - Dễ: quyết định đơn giản, đôi khi sai nhẹ.
 * - Thường: heuristic tốt theo từng game.
 * - Khó: heuristic + đếm bài + mô phỏng Monte Carlo (≤ 800ms).
 *
 * @param viName tên tiếng Việt hiển thị
 */
enum class BotLevel(val viName: String) {
    EASY("Dễ"),
    NORMAL("Thường"),
    HARD("Khó"),
}
