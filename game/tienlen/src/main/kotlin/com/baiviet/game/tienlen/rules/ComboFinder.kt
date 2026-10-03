package com.baiviet.game.tienlen.rules

import com.baiviet.core.cards.Card

/**
 * Liệt kê các bộ bài có thể tạo từ một tay bài.
 */
object ComboFinder {

    /** Mọi bộ hợp lệ trong tay (dùng khi đi tự do). */
    fun all(hand: List<Card>): List<Combo> {
        val byRank = groupByRank(hand)
        return buildList {
            addAll(sets(byRank, 1))
            addAll(sets(byRank, 2))
            addAll(sets(byRank, 3))
            addAll(sets(byRank, 4))
            for (len in 3..12) addAll(straights(byRank, len))
            for (pairs in 3..6) addAll(pairSequences(byRank, pairs))
        }
    }

    /**
     * Các bộ trong tay đánh được lên [top] (chặn thường hoặc chặt).
     *
     * @param outOfTurnOnly chỉ lấy nước chặt không cần vòng (4 đôi thông)
     */
    fun beating(hand: List<Card>, top: Combo, rules: TienLenRules, outOfTurnOnly: Boolean = false): List<Combo> {
        val byRank = groupByRank(hand)
        val candidates = buildList {
            if (!outOfTurnOnly) {
                when (top.type) {
                    ComboType.SINGLE -> addAll(sets(byRank, 1))
                    ComboType.PAIR -> addAll(sets(byRank, 2))
                    ComboType.TRIPLE -> addAll(sets(byRank, 3))
                    ComboType.STRAIGHT -> addAll(straights(byRank, top.size))
                    ComboType.PAIR_SEQUENCE, ComboType.QUAD -> Unit
                }
                addAll(sets(byRank, 4))
                addAll(pairSequences(byRank, 3))
            }
            for (pairs in 4..6) addAll(pairSequences(byRank, pairs))
        }
        return candidates.filter {
            if (outOfTurnOnly) BeatRules.isOutOfTurnCut(it, top, rules) else BeatRules.canBeat(it, top, rules)
        }
    }

    private fun groupByRank(hand: List<Card>): Array<List<Card>> {
        val arr = Array<MutableList<Card>>(13) { mutableListOf() }
        hand.forEach { arr[it.tlRank].add(it) }
        return Array(13) { arr[it].sortedTl() }
    }

    /** Rác/đôi/sám/tứ quý: tất cả tổ hợp [size] lá cùng số. */
    private fun sets(byRank: Array<List<Card>>, size: Int): List<Combo> = buildList {
        for (r in 0..12) {
            val cards = byRank[r]
            if (cards.size < size) continue
            for (combo in combinations(cards, size)) {
                Combo.classify(combo)?.let { add(it) }
            }
        }
    }

    /** Sảnh độ dài [length]: mọi cách chọn chất cho từng số (không chứa 2). */
    private fun straights(byRank: Array<List<Card>>, length: Int): List<Combo> = buildList {
        for (start in 0..(12 - length)) {
            val ranks = start until start + length
            if (ranks.any { byRank[it].isEmpty() }) continue
            product(ranks.map { r -> byRank[r].map { listOf(it) } }) { cards ->
                add(Combo(ComboType.STRAIGHT, cards.sortedTl()))
            }
        }
    }

    /** Đôi thông [pairs] đôi: mọi cách chọn đôi cho từng số (không chứa 2). */
    private fun pairSequences(byRank: Array<List<Card>>, pairs: Int): List<Combo> = buildList {
        for (start in 0..(12 - pairs)) {
            val ranks = start until start + pairs
            if (ranks.any { byRank[it].size < 2 }) continue
            product(ranks.map { r -> combinations(byRank[r], 2) }) { cards ->
                add(Combo(ComboType.PAIR_SEQUENCE, cards.sortedTl()))
            }
        }
    }

    private fun product(options: List<List<List<Card>>>, emit: (List<Card>) -> Unit) {
        val current = ArrayList<Card>()
        fun rec(i: Int) {
            if (i == options.size) {
                emit(ArrayList(current))
                return
            }
            for (choice in options[i]) {
                current.addAll(choice)
                rec(i + 1)
                repeat(choice.size) { current.removeAt(current.size - 1) }
            }
        }
        rec(0)
    }

    fun <T> combinations(items: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (items.size < k) return emptyList()
        val result = mutableListOf<List<T>>()
        fun rec(start: Int, acc: List<T>) {
            if (acc.size == k) {
                result += acc
                return
            }
            for (i in start until items.size) rec(i + 1, acc + items[i])
        }
        rec(0, emptyList())
        return result
    }
}
