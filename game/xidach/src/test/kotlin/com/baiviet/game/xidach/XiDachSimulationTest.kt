package com.baiviet.game.xidach

import com.baiviet.core.ai.BotLevel
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.xidach.bot.XiDachBot
import com.baiviet.game.xidach.engine.XiDachEngine
import com.baiviet.game.xidach.rules.XiDachRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class XiDachSimulationTest {

    private val engine = XiDachEngine()
    private val bot = XiDachBot()

    @Test
    fun simulate10000GamesZeroSum() = runBlocking {
        val config = TableConfig(playerCount = 4, rules = XiDachRules(), betUnit = 100L)
        val rng = Random(42)

        for (i in 1..10_000) {
            val seed = rng.nextLong()
            var state = engine.start(config, seed)
            var turns = 0

            while (!engine.isFinished(state) && turns < 50) {
                val actor = state.currentActor ?: break
                val legal = engine.legalActions(state, actor)
                if (legal.isEmpty()) break

                val view = engine.viewOf(state, actor)
                val action = bot.decide(view, legal, BotLevel.NORMAL)
                state = engine.apply(state, actor, action).state
                turns++
            }

            // Đảm bảo ván bài hoàn tất
            val settlement = engine.settle(state)
            assertEquals("Ván #$i không đạt zero-sum!", 0L, settlement.deltas.values.sum())
        }
    }
}
