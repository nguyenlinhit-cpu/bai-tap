package com.baiviet.game.bacay

import com.baiviet.core.ai.BotLevel
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.bacay.bot.BaCayBot
import com.baiviet.game.bacay.engine.BaCayEngine
import com.baiviet.game.bacay.rules.BaCayMode
import com.baiviet.game.bacay.rules.BaCayRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class BaCaySimulationTest {

    private val engine = BaCayEngine()
    private val bot = BaCayBot()

    @Test
    fun simulate10000GamesZeroSum() = runBlocking {
        val rng = Random(1234)

        for (i in 1..10_000) {
            val mode = if (i % 2 == 0) BaCayMode.WITH_DEALER else BaCayMode.WINNER_TAKES_ALL
            val config = TableConfig(
                playerCount = 4,
                rules = BaCayRules(gameMode = mode),
                betUnit = 100L,
            )
            val seed = rng.nextLong()
            var state = engine.start(config, seed)

            while (!engine.isFinished(state)) {
                val actors = engine.currentActors(state)
                if (actors.isEmpty()) break
                val actor = actors.first()
                val legal = engine.legalActions(state, actor)
                if (legal.isEmpty()) break

                val view = engine.viewOf(state, actor)
                val action = bot.decide(view, legal, BotLevel.NORMAL)
                state = engine.apply(state, actor, action).state
            }

            val settlement = engine.settle(state)
            assertEquals("Ván Ba Cây #$i ($mode) không đạt zero-sum!", 0L, settlement.deltas.values.sum())
        }
    }
}
