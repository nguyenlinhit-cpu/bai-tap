package com.baiviet.game.samloc

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit

/**
 * Viết lá bài ngắn gọn trong test: "3s" = 3♠, "10h" = 10♥, "2d" = 2♦, "Ac" = A♣.
 * Chất: s = bích, c = chuồn, d = rô, h = cơ.
 */
fun c(code: String): Card {
    val suit =
        when (code.last()) {
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
