package com.baiviet.core.cards

import kotlinx.serialization.Serializable

/**
 * Số bài — thứ tự khai báo KHÔNG dùng làm thứ tự so sánh.
 * Tiến lên: 2 lớn nhất. Phỏm: A nhỏ nhất. Poker: A cao/thấp.
 * → Mỗi game phải có Comparator riêng.
 */
@Serializable
enum class Rank(val label: String, val viName: String) {
    ACE("A", "Át"),
    TWO("2", "Hai"),
    THREE("3", "Ba"),
    FOUR("4", "Bốn"),
    FIVE("5", "Năm"),
    SIX("6", "Sáu"),
    SEVEN("7", "Bảy"),
    EIGHT("8", "Tám"),
    NINE("9", "Chín"),
    TEN("10", "Mười"),
    JACK("J", "Bồi"),
    QUEEN("Q", "Đầm"),
    KING("K", "Già"),
    ;

    /** Điểm dùng trong Phỏm: A=1, 2–10 theo số, J=11, Q=12, K=13 */
    val phomPoints: Int
        get() = when (this) {
            ACE -> 1; TWO -> 2; THREE -> 3; FOUR -> 4; FIVE -> 5
            SIX -> 6; SEVEN -> 7; EIGHT -> 8; NINE -> 9; TEN -> 10
            JACK -> 11; QUEEN -> 12; KING -> 13
        }

    /** Điểm dùng trong Xì dách: J/Q/K = 10, A xử lý riêng */
    val xidachBasePoints: Int
        get() = when (this) {
            ACE -> 0 // A xử lý linh hoạt ở engine
            TWO -> 2; THREE -> 3; FOUR -> 4; FIVE -> 5
            SIX -> 6; SEVEN -> 7; EIGHT -> 8; NINE -> 9
            TEN -> 10; JACK -> 10; QUEEN -> 10; KING -> 10
        }

    /** Điểm dùng trong Ba cây / Liêng: A=1, 10/J/Q/K=0, 2–9 theo số */
    val bacayPoints: Int
        get() = when (this) {
            ACE -> 1; TWO -> 2; THREE -> 3; FOUR -> 4; FIVE -> 5
            SIX -> 6; SEVEN -> 7; EIGHT -> 8; NINE -> 9
            TEN -> 0; JACK -> 0; QUEEN -> 0; KING -> 0
        }

    /** Tên đầy đủ dùng cho TalkBack: "Át", "Hai", ... "Già" */
    val accessibilityName: String get() = viName
}
