package com.baiviet.core.cards

import kotlinx.serialization.Serializable

/**
 * Một lá bài — bất biến, value-based equality.
 */
@Serializable
data class Card(val rank: Rank, val suit: Suit) {

    /** Ký hiệu ngắn: "A♠", "10♥", "K♦" */
    val shortName: String get() = "${rank.label}${suit.symbol}"

    /** Tên tiếng Việt đầy đủ cho TalkBack: "Át bích", "Mười cơ" */
    val viFullName: String get() = "${rank.viName} ${suit.viName}"

    /** contentDescription cho Compose accessibility */
    val contentDescription: String get() = viFullName

    override fun toString(): String = shortName
}
