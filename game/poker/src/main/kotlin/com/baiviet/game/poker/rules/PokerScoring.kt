package com.baiviet.game.poker.rules

import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import kotlinx.serialization.Serializable

@Serializable
data class PokerPlayerResult(
    val id: PlayerId,
    val hand: PokerHand?,
    val invested: Long,
    val payout: Long,
    val delta: Long,
    val isWinner: Boolean,
)

object PokerScoring {

    /**
     * Phân bổ pot và side pots cho người chơi, đảm bảo zero-sum tuyệt đối.
     *
     * @param players Danh sách người chơi theo thứ tự ghế ngồi.
     * @param invested Số chip mỗi người chơi đã bỏ vào bàn trong ván này.
     * @param folded Tập hợp ID những người chơi đã úp bài.
     * @param hands Bài của từng người chơi (dành cho showdown).
     * @param dealerIndex Vị trí của người giữ nút Dealer.
     */
    fun computeSettlement(
        players: List<PlayerId>,
        invested: Map<PlayerId, Long>,
        folded: Set<PlayerId>,
        hands: Map<PlayerId, PokerHand>,
        dealerIndex: Int = 0,
    ): Pair<Settlement, List<PokerPlayerResult>> {
        val totalPot = invested.values.sum()
        val nonFolded = players.filter { it !in folded }

        val payouts = players.associateWith { 0L }.toMutableMap()

        if (nonFolded.size == 1) {
            // Chỉ còn duy nhất 1 người không úp bài: Thắng toàn bộ Pot
            val soleWinner = nonFolded.first()
            payouts[soleWinner] = totalPot
        } else if (nonFolded.isNotEmpty()) {
            // Showdown với hệ thống phân tách Side Pot nhiều tầng
            val distinctLevels = invested.values.filter { it > 0 }.distinct().sorted()
            var prevLevel = 0L

            for (level in distinctLevels) {
                val tierIncrement = level - prevLevel
                val contributors = players.filter { (invested[it] ?: 0L) >= level }
                val tierPot = tierIncrement * contributors.size

                val eligibleContenders = contributors.filter { it !in folded }
                if (eligibleContenders.isEmpty()) {
                    // Nếu không còn ai chưa úp ở tầng này, hoàn trả đều cho người góp
                    val refundEach = tierPot / contributors.size
                    var rem = tierPot % contributors.size
                    for (c in contributors) {
                        payouts[c] = (payouts[c] ?: 0L) + refundEach
                        if (rem > 0) {
                            payouts[c] = (payouts[c] ?: 0L) + 1
                            rem--
                        }
                    }
                } else {
                    // Tìm người có tay bài mạnh nhất trong số ứng viên hợp lệ
                    val maxHand = eligibleContenders.mapNotNull { hands[it] }.maxOrNull()
                    val winners = if (maxHand != null) {
                        eligibleContenders.filter { hands[it] == maxHand }
                    } else {
                        eligibleContenders
                    }

                    val splitAmount = tierPot / winners.size
                    var remainder = tierPot % winners.size

                    for (w in winners) {
                        payouts[w] = (payouts[w] ?: 0L) + splitAmount
                    }

                    // Xu lẻ được trao cho người chơi gần bên trái nút Dealer nhất
                    if (remainder > 0) {
                        val sortedByButtonDistance = winners.sortedBy { w ->
                            val seat = players.indexOf(w)
                            (seat - dealerIndex + players.size) % players.size
                        }
                        for (w in sortedByButtonDistance) {
                            if (remainder <= 0) break
                            payouts[w] = (payouts[w] ?: 0L) + 1
                            remainder--
                        }
                    }
                }
                prevLevel = level
            }
        }

        val deltas = players.associateWith { p ->
            (payouts[p] ?: 0L) - (invested[p] ?: 0L)
        }

        val results = players.map { p ->
            val pInv = invested[p] ?: 0L
            val pPay = payouts[p] ?: 0L
            val delta = deltas[p] ?: 0L
            PokerPlayerResult(
                id = p,
                hand = hands[p],
                invested = pInv,
                payout = pPay,
                delta = delta,
                isWinner = pPay > pInv,
            )
        }

        return Settlement(deltas, emptyList()) to results
    }
}
