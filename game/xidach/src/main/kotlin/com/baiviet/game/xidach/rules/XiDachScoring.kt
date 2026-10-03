package com.baiviet.game.xidach.rules

import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.SettlementLine
import kotlinx.serialization.Serializable

/**
 * Kết quả so sánh giữa một nhà con và nhà cái.
 */
@Serializable
data class XiDachPlayerResult(
    val playerId: PlayerId,
    val playerHand: XiDachHand,
    val dealerHand: XiDachHand,
    val outcome: Int, // 1: con thắng, -1: cái thắng, 0: hòa
    val multiplier: Int,
    val betAmount: Long,
    val delta: Long, // số xu con nhận (+) hoặc mất (-)
    val reason: String,
)

/**
 * Tính toán trả thưởng và thanh toán cho ván Xì Dách.
 */
object XiDachScoring {

    fun computeSettlement(
        dealerId: PlayerId,
        players: List<PlayerId>,
        bets: Map<PlayerId, Long>,
        hands: Map<PlayerId, XiDachHand>,
        rules: XiDachRules,
    ): Pair<Settlement, List<XiDachPlayerResult>> {
        val dealerHand = hands[dealerId] ?: error("Không tìm thấy bài nhà cái")
        val lines = mutableListOf<SettlementLine>()
        val results = mutableListOf<XiDachPlayerResult>()

        val activePlayers = players.filter { it != dealerId }

        for (player in activePlayers) {
            val playerHand = hands[player] ?: error("Không tìm thấy bài của người chơi $player")
            val bet = bets[player] ?: 100L
            val outcome = XiDachEvaluator.compare(playerHand, dealerHand, rules.bothBustPlayerLoses)

            val multiplier = when (outcome) {
                1 -> XiDachEvaluator.payoutMultiplier(playerHand, rules)
                -1 -> XiDachEvaluator.payoutMultiplier(dealerHand, rules)
                else -> 1
            }

            val amount = bet * multiplier

            val (delta, reason) = when (outcome) {
                1 -> {
                    val r = "Thắng nhà cái (${playerHand.description()} vs ${dealerHand.description()}) ×${multiplier}"
                    lines.add(SettlementLine(from = dealerId, to = player, amount = amount, reason = r))
                    amount to r
                }
                -1 -> {
                    val r = "Thua nhà cái (${playerHand.description()} vs ${dealerHand.description()}) ×${multiplier}"
                    lines.add(SettlementLine(from = player, to = dealerId, amount = amount, reason = r))
                    -amount to r
                }
                else -> {
                    val r = "Hòa nhà cái (${playerHand.description()} vs ${dealerHand.description()})"
                    0L to r
                }
            }

            results.add(
                XiDachPlayerResult(
                    playerId = player,
                    playerHand = playerHand,
                    dealerHand = dealerHand,
                    outcome = outcome,
                    multiplier = multiplier,
                    betAmount = bet,
                    delta = delta,
                    reason = reason,
                ),
            )
        }

        val allPlayers = listOf(dealerId) + activePlayers
        val settlement = Settlement.fromLines(allPlayers, lines)
        return settlement to results
    }
}
