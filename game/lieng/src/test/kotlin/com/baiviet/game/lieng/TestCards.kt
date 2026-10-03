package com.baiviet.game.lieng

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit

val C2S = Card(Rank.TWO, Suit.SPADE)
val C3H = Card(Rank.THREE, Suit.HEART)
val C4D = Card(Rank.FOUR, Suit.DIAMOND)
val C5C = Card(Rank.FIVE, Suit.CLUB)
val C6S = Card(Rank.SIX, Suit.SPADE)
val C7H = Card(Rank.SEVEN, Suit.HEART)
val C8D = Card(Rank.EIGHT, Suit.DIAMOND)
val C9C = Card(Rank.NINE, Suit.CLUB)
val C10S = Card(Rank.TEN, Suit.SPADE)
val CJH = Card(Rank.JACK, Suit.HEART)
val CQD = Card(Rank.QUEEN, Suit.DIAMOND)
val CKC = Card(Rank.KING, Suit.CLUB)
val CKS = Card(Rank.KING, Suit.SPADE)
val CAS = Card(Rank.ACE, Suit.SPADE)
val CAH = Card(Rank.ACE, Suit.HEART)
val CAD = Card(Rank.ACE, Suit.DIAMOND)

/** "As" = A♠, "10h" = 10♥ … (s = bích, c = chuồn, d = rô, h = cơ). */
fun c(code: String): Card {
    val suit = when (code.last()) {
        's' -> Suit.SPADE
        'c' -> Suit.CLUB
        'd' -> Suit.DIAMOND
        'h' -> Suit.HEART
        else -> error("Chất không hợp lệ: $code")
    }
    val label = code.dropLast(1).uppercase()
    return Card(Rank.entries.first { it.label == label }, suit)
}

fun cards(codes: String): List<Card> = codes.trim().split(Regex("\\s+")).map { c(it) }
