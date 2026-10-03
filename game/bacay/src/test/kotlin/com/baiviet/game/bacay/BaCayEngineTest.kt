package com.baiviet.game.bacay

import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.bacay.engine.BaCayAction
import com.baiviet.game.bacay.engine.BaCayEngine
import com.baiviet.game.bacay.engine.BaCayPhase
import com.baiviet.game.bacay.rules.BaCayMode
import com.baiviet.game.bacay.rules.BaCayRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaCayEngineTest {

    private val engine = BaCayEngine()

    @Test
    fun testStartGameHas3CardsPerPlayer() {
        val config = TableConfig(playerCount = 4, rules = BaCayRules(), betUnit = 100L)
        val state = engine.start(config, seed = 12345L)

        assertNotNull(state)
        assertEquals(4, state.players.size)
        for (pid in state.players) {
            assertEquals(3, state.playerStates[pid]!!.cards.size)
            assertEquals(false, state.playerStates[pid]!!.isRevealed)
        }
        assertEquals(BaCayPhase.SQUEEZING, state.phase)
    }

    @Test
    fun testRevealAllEndsGameWithZeroSum() {
        val config = TableConfig(playerCount = 4, rules = BaCayRules(), betUnit = 100L)
        val state = engine.start(config, seed = 99999L)

        val transition = engine.apply(state, PlayerId(0), BaCayAction.RevealAll)
        val next = transition.state

        assertEquals(BaCayPhase.FINISHED, next.phase)
        assertTrue(engine.isFinished(next))

        val settlement = engine.settle(next)
        assertEquals(0L, settlement.deltas.values.sum())
    }

    @Test
    fun testWinnerTakesAllModeZeroSum() {
        val config = TableConfig(
            playerCount = 4,
            rules = BaCayRules(gameMode = BaCayMode.WINNER_TAKES_ALL),
            betUnit = 200L,
        )
        val state = engine.start(config, seed = 55555L)

        val transition = engine.apply(state, PlayerId(0), BaCayAction.RevealAll)
        val next = transition.state

        assertTrue(engine.isFinished(next))
        val settlement = engine.settle(next)
        assertEquals(0L, settlement.deltas.values.sum())
    }
}
