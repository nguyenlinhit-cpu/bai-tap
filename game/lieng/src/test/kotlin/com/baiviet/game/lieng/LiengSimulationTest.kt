package com.baiviet.game.lieng

import com.baiviet.core.ai.BotLevel
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.lieng.bot.LiengBot
import com.baiviet.game.lieng.engine.LiengEngine
import com.baiviet.game.lieng.rules.LiengRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Bot đấu bot 10.000 ván: không kẹt, không crash, tổng tiền = 0, không ai mất quá stack. */
class LiengSimulationTest {

    private val engine = LiengEngine()

    @Test
    fun simulate10000GamesZeroSum() = runBlocking {
        val rng = Random(777)
        val bot = LiengBot(Random(1))
        for (i in 1..10_000) {
            val n = 2 + i % 3
            val config = TableConfig(playerCount = n, rules = LiengRules(), betUnit = 100L)
            val stacks = List(n) { 100L * rng.nextLong(2, 120) }
            var state = engine.startHand(config, rng.nextLong(), stacks, firstSeat = i % n)
            var turns = 0
            while (!engine.isFinished(state)) {
                assertTrue("Ván #$i quá dài ($turns lượt)", turns < 200)
                val actor = state.currentActor
                assertNotNull("Ván #$i: chưa kết thúc nhưng không ai đến lượt", actor)
                val legal = engine.legalActions(state, actor!!)
                assertTrue("Ván #$i: không có nước hợp lệ", legal.isNotEmpty())
                val level = BotLevel.entries[(i + actor.seat) % 3]
                state = engine.apply(state, actor, bot.decide(engine.viewOf(state, actor), legal, level)).state
                turns++
            }
            val settlement = engine.settle(state)
            assertEquals("Ván Liêng #$i không zero-sum", 0L, settlement.deltas.values.sum())
            for (p in 0 until n) {
                assertTrue("Ván #$i: ghế $p mất quá stack", -(settlement.deltas[PlayerId(p)] ?: 0L) <= stacks[p])
            }
        }
    }
}
