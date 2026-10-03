package com.baiviet.core.cards

import java.security.SecureRandom
import kotlin.random.Random

/**
 * Bộ bài 52 lá — tạo ra danh sách cố định, xáo bài Fisher–Yates.
 *
 * RNG: seed từ [SecureRandom] mặc định, hoặc seed cố định cho test/replay.
 */
object Deck {

    /** 52 lá theo thứ tự cố định (không dùng để chơi trực tiếp) */
    val FULL_DECK: List<Card> = buildList {
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                add(Card(rank, suit))
            }
        }
    }

    /**
     * Tạo bộ bài đã xáo bằng Fisher–Yates shuffle.
     *
     * @param seed seed cho Random; dùng [SecureRandom] nếu null.
     *             Lưu seed + log hành động để tái hiện lỗi.
     * @return Pair(seed đã dùng, danh sách 52 lá đã xáo)
     */
    fun shuffled(seed: Long? = null): Pair<Long, List<Card>> {
        val actualSeed = seed ?: SecureRandom().nextLong()
        val rng = Random(actualSeed)
        val cards = FULL_DECK.toMutableList()
        // Fisher–Yates (Knuth) shuffle — đúng chuẩn, O(n)
        for (i in cards.size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = cards[i]
            cards[i] = cards[j]
            cards[j] = tmp
        }
        return actualSeed to cards.toList()
    }

    /**
     * Chia bài từ bộ đã xáo.
     *
     * @param deck bộ bài đã xáo (52 lá)
     * @param playerCount số người chơi
     * @param cardsPerPlayer số lá mỗi người
     * @param extraFirstPlayer số lá bổ sung cho người đi đầu (VD Phỏm: +1)
     * @return Pair(danh sách tay bài theo seat, phần dư còn lại — nọc/úp)
     */
    fun deal(
        deck: List<Card>,
        playerCount: Int,
        cardsPerPlayer: Int,
        extraFirstPlayer: Int = 0,
    ): Pair<List<List<Card>>, List<Card>> {
        require(deck.size >= playerCount * cardsPerPlayer + extraFirstPlayer) {
            "Không đủ bài để chia: cần ${playerCount * cardsPerPlayer + extraFirstPlayer}, có ${deck.size}"
        }
        var idx = 0
        val hands = List(playerCount) { seat ->
            val count = cardsPerPlayer + if (seat == 0) extraFirstPlayer else 0
            deck.subList(idx, idx + count).toList().also { idx += count }
        }
        val remainder = deck.subList(idx, deck.size).toList()
        return hands to remainder
    }
}
