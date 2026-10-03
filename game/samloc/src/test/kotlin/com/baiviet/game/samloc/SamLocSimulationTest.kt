package com.baiviet.game.samloc

import com.baiviet.core.ai.BotLevel
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.samloc.bot.SamLocBot
import com.baiviet.game.samloc.engine.SamLocEngine
import com.baiviet.game.samloc.engine.SamLocState
import com.baiviet.game.samloc.rules.SamLocRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Mô phỏng bot đấu bot 10.000 ván ngẫu nhiên Sâm Lốc:
 * Không crash, không trạng thái bất hợp lệ, tổng tiền mỗi ván = 0.
 */
class SamLocSimulationTest {
    private val engine = SamLocEngine()

    private fun checkInvariants(s: SamLocState) {
        val all = s.hands.flatten() + s.playedCards
        assertEquals("Không được có lá trùng", all.size, all.toSet().size)
        assertEquals("Tổng số lá bài phải bằng số người × 10", s.playerCount * 10, all.size)
        if (!s.finished) {
            assertTrue("Lượt ở ghế hợp lệ", s.turn in 0 until s.playerCount)
        }
    }

    private fun playGame(
        players: Int,
        seed: Long,
        rules: SamLocRules,
        levels: List<BotLevel>,
        previousWinner: PlayerId?,
        bot: SamLocBot,
    ): Pair<SamLocState, Long> =
        runBlocking {
            val table =
                TableConfig(
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
                assertTrue("Phải luôn có nước đi hợp lệ", legal.isNotEmpty())
                val action = bot.decide(engine.viewOf(s, PlayerId(seat)), legal, levels[seat])
                s = engine.apply(s, PlayerId(seat), action).state
                checkInvariants(s)
                steps++
                assertTrue("Ván không được lặp vô tận (steps = $steps)", steps < 500)
            }
            val settlement = engine.settle(s)
            s to settlement.deltas.values.sum()
        }

    @Test
    fun `bots play 10000 random games without errors and zero-sum`() {
        val rnd = Random(2026)
        val bot = SamLocBot(Random(42), timeBudgetMs = 10L)
        repeat(10_000) { i ->
            val players = 2 + i % 3
            val rules =
                if (i % 5 == 0) {
                    SamLocRules(quadCutsPairOfTwos = true, threeTriplesInstantWin = true)
                } else if (i % 5 == 1) {
                    SamLocRules(differentiateTwoColors = true)
                } else {
                    SamLocRules.DEFAULT
                }
            val levels = List(players) { if (rnd.nextBoolean()) BotLevel.EASY else BotLevel.NORMAL }
            val prev = if (i % 2 == 0) null else PlayerId(rnd.nextInt(players))
            val (_, sum) = playGame(players, rnd.nextLong(), rules, levels, prev, bot)
            assertEquals("Tổng tiền ván $i phải bằng 0", 0L, sum)
        }
    }

    @Test
    fun `hard bot plays cleanly`() {
        val rnd = Random(12345)
        val bot = SamLocBot(Random(99), timeBudgetMs = 10L)
        repeat(20) { i ->
            val players = 2 + i % 3
            val levels = List(players) { if (it == 0) BotLevel.HARD else BotLevel.NORMAL }
            val (_, sum) = playGame(players, rnd.nextLong(), SamLocRules.DEFAULT, levels, null, bot)
            assertEquals(0L, sum)
        }
    }
}
