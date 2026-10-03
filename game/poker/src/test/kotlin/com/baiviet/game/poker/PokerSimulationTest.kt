package com.baiviet.game.poker

import com.baiviet.core.ai.BotLevel
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.poker.bot.PokerBot
import com.baiviet.game.poker.engine.PokerEngine
import com.baiviet.game.poker.rules.PokerRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Bot đấu bot 10.000 ván với stack mang qua ván và nút dealer xoay vòng. */
class PokerSimulationTest {

    private val engine = PokerEngine()

    @Test
    fun simulate10000GamesZeroSum() = runBlocking {
        val rng = Random(999)
        val bot = PokerBot(Random(2))
        val bb = 100L
        var stacks = mutableListOf<Long>()
        var n = 0
        var dealer = 0
        for (i in 1..10_000) {
            if (i % 50 == 1 || stacks.any { it <= 0 }) {
                n = 2 + (i / 50) % 3
                stacks = MutableList(n) { bb * rng.nextLong(20, 200) }
            }
            val config = TableConfig(playerCount = n, rules = PokerRules(), betUnit = bb)
            dealer = (dealer + 1) % n
            var state = engine.startHand(config, rng.nextLong(), stacks, dealer)
            var turns = 0
            while (!engine.isFinished(state)) {
                assertTrue("Ván #$i quá dài", turns < 200)
                val actor = state.currentActor
                assertNotNull("Ván #$i: không ai đến lượt", actor)
                val legal = engine.legalActions(state, actor!!)
                assertTrue("Ván #$i: không có nước hợp lệ", legal.isNotEmpty())
                val level = BotLevel.entries[(i + actor.seat) % 3]
                state = engine.apply(state, actor, bot.decide(engine.viewOf(state, actor), legal, level)).state
                turns++
            }
            val settlement = engine.settle(state)
            assertEquals("Ván Poker #$i không zero-sum", 0L, settlement.deltas.values.sum())
            for (p in 0 until n) {
                val delta = settlement.deltas[PlayerId(p)] ?: 0L
                assertTrue("Ván #$i: ghế $p mất quá stack", -delta <= stacks[p])
                stacks[p] += delta
                assertEquals(stacks[p], state.playerStates.getValue(PlayerId(p)).stack)
            }
        }
    }
}
