package com.baiviet.game.bacay.rules

import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.SettlementLine
import kotlinx.serialization.Serializable

@Serializable
data class BaCayPlayerResult(
    val playerId: PlayerId,
    val hand: BaCayHand,
    val outcome: Int, // 1: thắng, -1: thua, 0: hòa
    val delta: Long,
    val reason: String,
)

object BaCayScoring {

    fun computeSettlement(
        dealerId: PlayerId,
        players: List<PlayerId>,
        bets: Map<PlayerId, Long>,
        hands: Map<PlayerId, BaCayHand>,
        rules: BaCayRules,
    ): Pair<Settlement, List<BaCayPlayerResult>> {
        return if (rules.gameMode == BaCayMode.WITH_DEALER) {
            computeWithDealer(dealerId, players, bets, hands, rules)
        } else {
            computeWinnerTakesAll(players, bets, hands, rules)
        }
    }

    private fun computeWithDealer(
        dealerId: PlayerId,
        players: List<PlayerId>,
        bets: Map<PlayerId, Long>,
        hands: Map<PlayerId, BaCayHand>,
        rules: BaCayRules,
    ): Pair<Settlement, List<BaCayPlayerResult>> {
        val dealerHand = hands[dealerId] ?: error("Không tìm thấy bài nhà cái")
        val lines = mutableListOf<SettlementLine>()
        val results = mutableListOf<BaCayPlayerResult>()

        val activeCon = players.filter { it != dealerId }

        for (player in activeCon) {
            val playerHand = hands[player] ?: error("Không tìm thấy bài của người chơi $player")
            val bet = bets[player] ?: 100L
            val outcome = BaCayEvaluator.compare(playerHand, dealerHand, rules)

            val multiplier = when (outcome) {
                1 -> BaCayEvaluator.payoutMultiplier(playerHand, rules)
                -1 -> BaCayEvaluator.payoutMultiplier(dealerHand, rules)
                else -> 1
            }

            val amount = bet * multiplier

            val (delta, reason) = when (outcome) {
                1 -> {
                    val r = "Thắng nhà cái (${playerHand.description()} vs ${dealerHand.description()}) ×$multiplier"
                    lines.add(SettlementLine(from = dealerId, to = player, amount = amount, reason = r))
                    amount to r
                }
                -1 -> {
                    val r = "Thua nhà cái (${playerHand.description()} vs ${dealerHand.description()}) ×$multiplier"
                    lines.add(SettlementLine(from = player, to = dealerId, amount = amount, reason = r))
                    -amount to r
                }
                else -> {
                    val r = "Hòa nhà cái (${playerHand.description()} vs ${dealerHand.description()})"
                    0L to r
                }
            }

            results.add(
                BaCayPlayerResult(
                    playerId = player,
                    hand = playerHand,
                    outcome = outcome,
                    delta = delta,
                    reason = reason,
                ),
            )
        }

        // Thêm kết quả của nhà cái vào danh sách hiển thị
        val dealerDelta = -results.sumOf { it.delta }
        val dealerOutcome = when {
            dealerDelta > 0 -> 1
            dealerDelta < 0 -> -1
            else -> 0
        }
        val dealerResult = BaCayPlayerResult(
            playerId = dealerId,
            hand = dealerHand,
            outcome = dealerOutcome,
            delta = dealerDelta,
            reason = "Nhà cái",
        )
        val allResults = listOf(dealerResult) + results

        val settlement = Settlement.fromLines(players, lines)
        return settlement to allResults
    }

    private fun computeWinnerTakesAll(
        players: List<PlayerId>,
        bets: Map<PlayerId, Long>,
        hands: Map<PlayerId, BaCayHand>,
        rules: BaCayRules,
    ): Pair<Settlement, List<BaCayPlayerResult>> {
        val lines = mutableListOf<SettlementLine>()

        // Tìm người có bài lớn nhất
        var bestPlayers = mutableListOf(players.first())
        var bestHand = hands[players.first()]!!

        for (i in 1 until players.size) {
            val pid = players[i]
            val hand = hands[pid]!!
            val cmp = BaCayEvaluator.compare(hand, bestHand, rules)
            if (cmp > 0) {
                bestPlayers = mutableListOf(pid)
                bestHand = hand
            } else if (cmp == 0) {
                bestPlayers.add(pid)
            }
        }

        val betPerPlayer = bets[players.first()] ?: 100L
        val losers = players.filter { it !in bestPlayers }

        // Mỗi người thua trả phần cược của mình chia đều cho những người thắng
        for (loser in losers) {
            val share = betPerPlayer / bestPlayers.size
            for (winner in bestPlayers) {
                lines.add(
                    SettlementLine(
                        from = loser,
                        to = winner,
                        amount = share,
                        reason = "Ăn tất: ${bestHand.description()} thắng",
                    ),
                )
            }
        }

        val settlement = Settlement.fromLines(players, lines)
        val results = players.map { pid ->
            val hand = hands[pid]!!
            val delta = settlement.deltas[pid] ?: 0L
            val outcome = when {
                delta > 0 -> 1
                delta < 0 -> -1
                else -> 0
            }
            BaCayPlayerResult(
                playerId = pid,
                hand = hand,
                outcome = outcome,
                delta = delta,
                reason = "${hand.description()}: ${if (delta > 0) "Thắng nhất ăn tất" else if (delta < 0) "Thua ván" else "Hòa"}",
            )
        }

        return settlement to results
    }
}
