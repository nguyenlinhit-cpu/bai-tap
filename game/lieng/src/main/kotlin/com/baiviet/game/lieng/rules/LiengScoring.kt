package com.baiviet.game.lieng.rules

import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.SettlementLine
import kotlinx.serialization.Serializable

@Serializable
data class LiengPlayerResult(
    val playerId: PlayerId,
    val hand: LiengHand?,
    val invested: Long,
    val won: Long,
    val delta: Long,
    val isFolded: Boolean,
    val reason: String,
)

/**
 * Chia pot Liêng có side pot nhiều tầng.
 *
 * - Mỗi tầng pot chỉ những người đã góp tới tầng đó và chưa úp mới được tranh.
 * - Hòa thì chia đều; xu lẻ cho người ngồi gần ghế mở vòng nhất.
 * - Nếu mọi người góp vào một tầng đều đã úp, tầng đó trả về cho chính họ.
 */
object LiengScoring {

    fun computeSettlement(
        players: List<PlayerId>,
        invested: Map<PlayerId, Long>,
        folded: Set<PlayerId>,
        hands: Map<PlayerId, LiengHand>,
        rules: LiengRules,
        firstSeat: Int = 0,
    ): Pair<Settlement, List<LiengPlayerResult>> {
        val nonFolded = players.filter { it !in folded }
        val payouts = players.associateWith { 0L }.toMutableMap()
        val seatOrder = { p: PlayerId -> (players.indexOf(p) - firstSeat + players.size) % players.size }

        fun bestOf(candidates: List<PlayerId>): List<PlayerId> {
            if (candidates.size <= 1) return candidates
            var best = mutableListOf(candidates.first())
            for (p in candidates.drop(1)) {
                val cmp = LiengEvaluator.compare(hands.getValue(p), hands.getValue(best.first()), rules)
                if (cmp > 0) best = mutableListOf(p) else if (cmp == 0) best += p
            }
            return best
        }

        fun distribute(amount: Long, receivers: List<PlayerId>) {
            if (receivers.isEmpty() || amount <= 0) return
            val sorted = receivers.sortedBy(seatOrder)
            val share = amount / sorted.size
            var rem = amount % sorted.size
            for (r in sorted) {
                payouts[r] = payouts.getValue(r) + share + if (rem > 0) 1 else 0
                if (rem > 0) rem--
            }
        }

        if (nonFolded.size == 1) {
            distribute(invested.values.sum(), nonFolded)
        } else {
            var prev = 0L
            for (level in invested.values.filter { it > 0 }.distinct().sorted()) {
                val contributors = players.filter { (invested[it] ?: 0L) >= level }
                val tier = (level - prev) * contributors.size
                val contenders = contributors.filter { it !in folded }
                distribute(tier, if (contenders.isEmpty()) contributors else bestOf(contenders))
                prev = level
            }
        }

        val deltas = players.associateWith { payouts.getValue(it) - (invested[it] ?: 0L) }

        // Dòng thanh toán: người âm trả người dương (ghép tham lam theo thứ tự ghế)
        val winners = ArrayDeque(players.filter { deltas.getValue(it) > 0 }.map { it to deltas.getValue(it) })
        val lines = mutableListOf<SettlementLine>()
        for (loser in players.filter { deltas.getValue(it) < 0 }) {
            var owe = -deltas.getValue(loser)
            while (owe > 0 && winners.isNotEmpty()) {
                val (w, want) = winners.removeFirst()
                val pay = minOf(owe, want)
                val reason = if (loser in folded) "Úp bài" else "Thua pot (${hands[loser]?.description() ?: ""})"
                lines += SettlementLine(loser, w, pay, reason)
                owe -= pay
                if (want > pay) winners.addFirst(w to want - pay)
            }
        }
        val settlement = Settlement.fromLines(players, lines)

        val results = players.map { pid ->
            val isF = pid in folded
            val hand = hands[pid]
            val myInvested = invested[pid] ?: 0L
            val myDelta = deltas.getValue(pid)
            val reason = when {
                isF -> "Đã úp bài (−$myInvested)"
                myDelta > 0 -> "Thắng pot (+$myDelta xu, ${hand?.description() ?: "bài mạnh nhất"})"
                myDelta == 0L -> "Hòa (${hand?.description() ?: ""})"
                else -> "Thua pot (−${-myDelta} xu, ${hand?.description() ?: ""})"
            }
            LiengPlayerResult(
                playerId = pid,
                hand = hand,
                invested = myInvested,
                won = payouts.getValue(pid),
                delta = myDelta,
                isFolded = isF,
                reason = reason,
            )
        }
        return settlement to results
    }
}
