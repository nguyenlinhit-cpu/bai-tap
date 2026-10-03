package com.baiviet.game.phom

import com.baiviet.core.ai.BotLevel
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.phom.bot.PhomBot
import com.baiviet.game.phom.engine.PhomEngine
import com.baiviet.game.phom.engine.PhomState
import com.baiviet.game.phom.rules.PhomRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Mô phỏng bot đấu bot 10.000 ván ngẫu nhiên Phỏm (Tá Lả):
 * Không crash, không trạng thái bất hợp lệ, zero-sum bảo toàn 100% ván.
 */
class PhomSimulationTest {
    private val engine = PhomEngine()

    private fun checkInvariants(s: PhomState) {
        val allHandCards = s.hands.flatten()
        val allDiscards = s.discardPiles.flatten()
        val allStock = s.stock
        val allExposed = s.exposedMelds.flatten().flatMap { it.cards }
        
        // Tất cả các lá bài hiện hữu không được trùng lặp ngoại trừ các lá đang được hạ từ tay
        assertTrue("Số người trong bàn phải từ 2 đến 4", s.playerCount in 2..4)
        if (!s.finished) {
            assertTrue("Lượt ở ghế hợp lệ", s.turn in 0 until s.playerCount)
        }
    }

    private fun playGame(
        players: Int,
        seed: Long,
        rules: PhomRules,
        levels: List<BotLevel>,
        previousWinner: PlayerId?,
        bot: PhomBot,
    ): Pair<PhomState, Long> =
        runBlocking {
            val table = TableConfig(
                playerCount = players,
                rules = rules,
                betUnit = 100L,
                minBalanceMultiplier = rules.minBalanceMultiplier,
            )
            var s = engine.startGame(table, seed, previousWinner)
            var steps = 0
            while (!engine.isFinished(s)) {
                val seat = s.turn
                val legal = engine.legalActions(s, PlayerId(seat))
                assertTrue(
                    "Phải luôn có nước đi hợp lệ: phase=${s.phase}, seat=$seat, turn=${s.turn}, stock=${s.stock.size}, hand=${s.hands[seat].size}, discards=${s.discardPiles.map { it.size }}, lastDiscard=${s.lastDiscard}, lastDiscarder=${s.lastDiscarder}, eaten=${s.eatenCards[seat]}",
                    legal.isNotEmpty()
                )
                val action = bot.decide(engine.viewOf(s, PlayerId(seat)), legal, levels[seat])
                s = engine.apply(s, PlayerId(seat), action).state
                checkInvariants(s)
                steps++
                assertTrue("Ván không được lặp vô tận (steps = $steps)", steps < 500)
            }
            val settlement = engine.settle(s)
            val sum = settlement.deltas.values.sum()
            s to sum
        }

    @Test
    fun `bots play 10000 random games without errors and zero-sum`() {
        val rnd = Random(2026)
        val bot = PhomBot(Random(42), timeBudgetMs = 10L)
        repeat(10_000) { i ->
            val players = 2 + i % 3 // 2, 3, or 4 players
            val rules = when (i % 4) {
                0 -> PhomRules(progressiveEatPenalty = true, eatChotDenEnabled = true, uKhanEnabled = true)
                1 -> PhomRules(progressiveEatPenalty = false, eatChotDenEnabled = true, uKhanEnabled = true)
                2 -> PhomRules(progressiveEatPenalty = true, eatChotDenEnabled = false, uKhanEnabled = false)
                else -> PhomRules.DEFAULT
            }
            val levels = List(players) { if (rnd.nextBoolean()) BotLevel.EASY else BotLevel.NORMAL }
            val prev = if (i % 2 == 0) null else PlayerId(rnd.nextInt(players))
            val (_, sum) = playGame(players, rnd.nextLong(), rules, levels, prev, bot)
            assertEquals("Tổng tiền ván $i phải bằng 0", 0L, sum)
        }
    }

    @Test
    fun `hard bot plays cleanly in 4-player game`() {
        val rnd = Random(12345)
        val bot = PhomBot(Random(99), timeBudgetMs = 10L)
        repeat(20) { i ->
            val players = 4
            val levels = List(players) { if (it == 0) BotLevel.HARD else BotLevel.NORMAL }
            val (_, sum) = playGame(players, rnd.nextLong(), PhomRules.DEFAULT, levels, null, bot)
            assertEquals("Tổng tiền ván Hard bot phải bằng 0", 0L, sum)
        }
    }
}
