package com.baiviet.game.tienlen.bot

import com.baiviet.core.cards.Card
import com.baiviet.game.tienlen.rules.Combo
import com.baiviet.game.tienlen.rules.ComboType
import com.baiviet.game.tienlen.rules.TlOrder
import com.baiviet.game.tienlen.rules.sortedTl
import com.baiviet.game.tienlen.rules.tlRank
import com.baiviet.game.tienlen.rules.tlValue

/**
 * Tách tay bài thành các bộ sao cho ít lượt đánh nhất (không tách sảnh/đôi vô lý).
 *
 * Thuật toán: tìm kiếm có nhớ trên vector số lượng lá theo từng số (3 → A).
 * Tại số nhỏ nhất còn lá, thử: lấy hết thành rác/đôi/sám/tứ quý, hoặc mở một sảnh / đôi thông
 * bắt đầu từ số đó. Heo (2) luôn để riêng làm bài khống chế.
 *
 * Chi phí mỗi bộ ≈ 1 lượt; rác nhỏ bị phạt thêm (khó thoát), hàng được thưởng (dùng để chặt).
 */
object HandAnalyzer {

    private sealed interface Group {
        data class Set(val rank: Int, val count: Int) : Group
        data class Straight(val start: Int, val length: Int) : Group
        data class Pairs(val start: Int, val length: Int) : Group
    }

    private data class Plan(val cost: Double, val groups: List<Group>)

    /** Kết quả tách bài: danh sách bộ, sắp theo lá nhỏ nhất. */
    fun decompose(hand: List<Card>): List<Combo> {
        val byRank = Array(13) { mutableListOf<Card>() }
        hand.sortedTl().forEach { byRank[it.tlRank].add(it) }
        val counts = IntArray(12) { byRank[it].size }
        val plan = search(counts, HashMap())

        val result = mutableListOf<Combo>()
        // Đôi thông và sảnh lấy các lá chất nhỏ trước, để lại chất lớn cho rác/đôi.
        plan.groups.filterIsInstance<Group.Pairs>().forEach { g ->
            val cards = (g.start until g.start + g.length).flatMap { r -> List(2) { byRank[r].removeAt(0) } }
            result += Combo(ComboType.PAIR_SEQUENCE, cards.sortedTl())
        }
        plan.groups.filterIsInstance<Group.Straight>().forEach { g ->
            val cards = (g.start until g.start + g.length).map { r -> byRank[r].removeAt(0) }
            result += Combo(ComboType.STRAIGHT, cards.sortedTl())
        }
        plan.groups.filterIsInstance<Group.Set>().forEach { g ->
            val cards = List(g.count) { byRank[g.rank].removeAt(0) }
            Combo.classify(cards)?.let { result += it }
        }
        // Heo
        val twos = byRank[TlOrder.TWO_INDEX]
        if (twos.isNotEmpty()) Combo.classify(twos)?.let { result += it }
        return result.sortedBy { it.cards.first().tlValue }
    }

    private fun key(counts: IntArray): Long {
        var k = 0L
        for (c in counts) k = k * 5 + c
        return k
    }

    private fun search(counts: IntArray, memo: HashMap<Long, Plan>): Plan {
        val r = counts.indexOfFirst { it > 0 }
        if (r < 0) return Plan(0.0, emptyList())
        val k = key(counts)
        memo[k]?.let { return it }

        var best: Plan? = null
        fun consider(cost: Double, group: Group, next: IntArray) {
            val sub = search(next, memo)
            val total = cost + sub.cost
            if (best == null || total < best!!.cost) best = Plan(total, listOf(group) + sub.groups)
        }

        // Lấy hết thành một bộ cùng số
        run {
            val c = counts[r]
            val next = counts.copyOf().also { it[r] = 0 }
            consider(setCost(r, c), Group.Set(r, c), next)
        }
        // Sảnh bắt đầu từ r
        var len = 1
        while (r + len < 12 && counts[r + len] > 0) len++
        for (l in 3..len) {
            val next = counts.copyOf()
            for (i in r until r + l) next[i]--
            consider(1.0 - 0.03 * l, Group.Straight(r, l), next)
        }
        // Đôi thông bắt đầu từ r
        var plen = 0
        while (r + plen < 12 && counts[r + plen] >= 2) plen++
        for (l in 3..plen) {
            val next = counts.copyOf()
            for (i in r until r + l) next[i] -= 2
            consider(0.3, Group.Pairs(r, l), next)
        }
        return best!!.also { memo[k] = it }
    }

    private fun setCost(rank: Int, count: Int): Double = when (count) {
        1 -> 1.0 + (11 - rank) * 0.04
        2 -> 0.9 + (11 - rank) * 0.02
        3 -> 0.8
        else -> 0.2
    }
}
