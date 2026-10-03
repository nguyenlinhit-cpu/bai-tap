package com.baiviet.game.samloc.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank

/**
 * Liệt kê các bộ bài có thể tạo từ một tay bài trong Sâm Lốc.
 */
object SamLocComboFinder {
    /** Mọi bộ hợp lệ trong tay (dùng khi đi tự do). */
    fun all(hand: List<Card>): List<SamLocCombo> {
        val byRank = groupByRank(hand)
        return buildList {
            addAll(sets(byRank, 1))
            addAll(sets(byRank, 2))
            addAll(sets(byRank, 3))
            addAll(sets(byRank, 4))
            for (len in 3..10) {
                addAll(straights(byRank, len))
            }
        }
    }

    /**
     * Các bộ trong tay đánh được lên [top] (chặn thường hoặc chặt).
     */
    fun beating(
        hand: List<Card>,
        top: SamLocCombo,
        rules: SamLocRules,
    ): List<SamLocCombo> {
        val byRank = groupByRank(hand)
        val candidates =
            buildList {
                when (top.type) {
                    SamLocComboType.SINGLE -> {
                        addAll(sets(byRank, 1))
                        if (top.top.isTwo) addAll(sets(byRank, 4))
                    }

                    SamLocComboType.PAIR -> {
                        addAll(sets(byRank, 2))
                        if (top.isTwos && rules.quadCutsPairOfTwos) addAll(sets(byRank, 4))
                    }

                    SamLocComboType.TRIPLE -> {
                        addAll(sets(byRank, 3))
                    }

                    SamLocComboType.QUAD -> {
                        addAll(sets(byRank, 4))
                    }

                    SamLocComboType.STRAIGHT -> {
                        addAll(straights(byRank, top.size))
                    }
                }
            }
        return candidates.filter { SamLocBeatRules.canBeat(it, top, rules) }
    }

    private fun groupByRank(hand: List<Card>): Array<List<Card>> {
        val arr = Array<MutableList<Card>>(13) { mutableListOf() }
        hand.forEach { arr[it.slRank].add(it) }
        return Array(13) { arr[it].sortedSl() }
    }

    private fun sets(
        byRank: Array<List<Card>>,
        size: Int,
    ): List<SamLocCombo> =
        buildList {
            for (r in 0..12) {
                val cards = byRank[r]
                if (cards.size < size) continue
                for (combo in combinations(cards, size)) {
                    SamLocCombo.classify(combo)?.let { add(it) }
                }
            }
        }

    /**
     * Tìm tất cả sảnh có độ dài [length] trong tay bài.
     */
    private fun straights(
        byRank: Array<List<Card>>,
        length: Int,
    ): List<SamLocCombo> =
        buildList {
            if (length < 3 || length > 10) return@buildList

            // 1. Sảnh bắt đầu bằng A-2-3-4...
            // Rank sequence: A (11), 2 (12), 3 (0), 4 (1), ..., length - 1
            val a2Sequence =
                buildList {
                    add(SlOrder.ACE_INDEX)
                    add(SlOrder.TWO_INDEX)
                    for (i in 0 until (length - 2)) add(i)
                }
            if (a2Sequence.all { byRank[it].isNotEmpty() }) {
                product(a2Sequence.map { r -> byRank[r].map { listOf(it) } }) { cards ->
                    SamLocCombo.classify(cards)?.let { add(it) }
                }
            }

            // 2. Sảnh bắt đầu bằng 2-3-4-5...
            // Rank sequence: 2 (12), 3 (0), 4 (1), ..., length - 2
            val twoSequence =
                buildList {
                    add(SlOrder.TWO_INDEX)
                    for (i in 0 until (length - 1)) add(i)
                }
            if (twoSequence.all { byRank[it].isNotEmpty() }) {
                product(twoSequence.map { r -> byRank[r].map { listOf(it) } }) { cards ->
                    SamLocCombo.classify(cards)?.let { add(it) }
                }
            }

            // 3. Sảnh thông thường không chứa 2: từ start đến start + length - 1 trong 0..11
            for (start in 0..(12 - length)) {
                val ranks = (start until start + length).toList()
                if (ranks.all { byRank[it].isNotEmpty() }) {
                    product(ranks.map { r -> byRank[r].map { listOf(it) } }) { cards ->
                        SamLocCombo.classify(cards)?.let { add(it) }
                    }
                }
            }
        }

    private fun product(
        options: List<List<List<Card>>>,
        emit: (List<Card>) -> Unit,
    ) {
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

    private fun <T> combinations(
        list: List<T>,
        k: Int,
    ): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (list.size < k) return emptyList()
        if (list.size == k) return listOf(list)
        val head = list[0]
        val tail = list.subList(1, list.size)
        val withHead = combinations(tail, k - 1).map { listOf(head) + it }
        val withoutHead = combinations(tail, k)
        return withHead + withoutHead
    }
}
