package com.baiviet.game.maubinh

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit

/**
 * Cú pháp ngắn gọn biểu diễn lá bài trong test:
 * "2s" = 2♠, "10h" = 10♥, "Ad" = A♦, "Kc" = K♣.
 * Chất: s = bích (spade), c = chuồn (club), d = rô (diamond), h = cơ (heart).
 */
fun c(code: String): Card {
    val suit = when (code.last()) {
        's' -> Suit.SPADE
        'c' -> Suit.CLUB
        'd' -> Suit.DIAMOND
        'h' -> Suit.HEART
        else -> error("Chất không hợp lệ: $code")
    }
    val label = code.dropLast(1).uppercase()
    val rank = Rank.entries.first { it.label == label }
    return Card(rank, suit)
}

fun cards(codes: String): List<Card> = codes.trim().split(Regex("\\s+")).map { c(it) }
