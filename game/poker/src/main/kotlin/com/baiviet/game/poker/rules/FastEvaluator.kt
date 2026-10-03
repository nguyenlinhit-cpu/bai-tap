package com.baiviet.game.poker.rules

import com.baiviet.core.cards.Card

/**
 * Bộ đánh giá nhanh 5–7 lá, trả về một số Long — càng lớn tay càng mạnh.
 * Thứ tự trùng khớp với [PokerEvaluator] (loại tay rồi kicker), không cấp phát object,
 * dùng cho mô phỏng Monte Carlo của bot (hàng nghìn lần mỗi quyết định).
 *
 * Mã hóa: loại tay (1..10) × 16^5 + tối đa 5 giá trị kicker (2..14) theo cơ số 16.
 */
object FastEvaluator {

    fun score(cards: List<Card>): Long {
        val counts = IntArray(15)
        val suitMasks = IntArray(4)
        val suitCounts = IntArray(4)
        var rankMask = 0
        for (c in cards) {
            val v = PokerHand.rankToValue(c.rank)
            counts[v]++
            val s = c.suit.ordinal
            suitMasks[s] = suitMasks[s] or (1 shl v)
            suitCounts[s]++
            rankMask = rankMask or (1 shl v)
        }

        // Thùng / thùng phá sảnh
        for (s in 0..3) {
            if (suitCounts[s] >= 5) {
                val sf = straightHigh(suitMasks[s])
                if (sf > 0) return encode(if (sf == 14) 10 else 9, sf)
                val top = topBits(suitMasks[s], 5)
                return encode(6, *top)
            }
        }

        var quad = 0
        var trip1 = 0
        var trip2 = 0
        var pair1 = 0
        var pair2 = 0
        for (v in 14 downTo 2) {
            when (counts[v]) {
                4 -> if (quad == 0) quad = v
                3 -> if (trip1 == 0) trip1 = v else if (trip2 == 0) trip2 = v
                2 -> if (pair1 == 0) pair1 = v else if (pair2 == 0) pair2 = v
            }
        }
        if (quad > 0) return encode(8, quad, kickers(counts, 1, quad)[0])
        if (trip1 > 0 && (trip2 > 0 || pair1 > 0)) return encode(7, trip1, maxOf(trip2, pair1))
        val straight = straightHigh(rankMask)
        if (straight > 0) return encode(5, straight)
        if (trip1 > 0) return encode(4, trip1, *kickers(counts, 2, trip1))
        if (pair1 > 0 && pair2 > 0) return encode(3, pair1, pair2, kickers(counts, 1, pair1, pair2)[0])
        if (pair1 > 0) return encode(2, pair1, *kickers(counts, 3, pair1))
        return encode(1, *kickers(counts, 5))
    }

    /** Lá cao nhất của sảnh trong bitmask (A thấp cho A-2-3-4-5), 0 nếu không có. */
    private fun straightHigh(mask: Int): Int {
        val m = if (mask and (1 shl 14) != 0) mask or (1 shl 1) else mask
        for (high in 14 downTo 5) {
            val need = 0b11111 shl (high - 4)
            if (m and need == need) return high
        }
        return 0
    }

    private fun topBits(mask: Int, n: Int): IntArray {
        val out = IntArray(n)
        var i = 0
        for (v in 14 downTo 2) if (mask and (1 shl v) != 0 && i < n) out[i++] = v
        return out
    }

    private fun kickers(counts: IntArray, n: Int, vararg exclude: Int): IntArray {
        val out = IntArray(n)
        var i = 0
        for (v in 14 downTo 2) {
            if (i >= n) break
            if (counts[v] > 0 && v !in exclude) out[i++] = v
        }
        return out
    }

    private fun encode(category: Int, vararg values: Int): Long {
        var code = category.toLong()
        for (i in 0 until 5) code = code * 16 + (values.getOrNull(i) ?: 0)
        return code
    }
}
