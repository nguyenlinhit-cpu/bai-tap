package com.baiviet.core.cards

import kotlinx.serialization.Serializable

/**
 * Chất bài — thứ tự ở đây KHÔNG dùng làm thứ tự so sánh chung.
 * Mỗi game tự định nghĩa Comparator riêng.
 */
@Serializable
enum class Suit(val symbol: String, val viName: String) {
    SPADE("♠", "Bích"),
    CLUB("♣", "Chuồn"),
    DIAMOND("♦", "Rô"),
    HEART("♥", "Cơ"),
    ;

    /** Tên dùng cho TalkBack: "Bích", "Chuồn", "Rô", "Cơ" */
    val accessibilityName: String get() = viName

    /** Màu bài: đỏ (Cơ, Rô) hoặc đen (Bích, Chuồn) */
    val isRed: Boolean get() = this == DIAMOND || this == HEART
}
