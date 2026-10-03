package com.baiviet.game.lieng

import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.lieng.engine.LiengAction
import com.baiviet.game.lieng.engine.LiengEngine
import com.baiviet.game.lieng.engine.LiengPhase
import com.baiviet.game.lieng.rules.LiengRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiengEngineTest {

    private val engine = LiengEngine()
    private val config = TableConfig(playerCount = 4, rules = LiengRules(), betUnit = 100L)

    @Test
    fun testStartGameDeals3CardsAndPostsAnte() {
        val state = engine.start(config, seed = 12345L)
        assertNotNull(state)
        assertEquals(4, state.players.size)
        assertEquals(LiengPhase.BETTING, state.phase)
        assertEquals(400L, state.pot) // 4 người × 100 xu ante
        for (pid in state.players) {
            assertEquals(3, state.playerStates[pid]!!.cards.size)
            assertEquals(100L, state.playerStates[pid]!!.invested)
        }
    }

    @Test
    fun testAllFoldExceptOneInstantWin() {
        var state = engine.start(config, seed = 99999L)
        // 3 người đầu úp bài
        state = engine.apply(state, PlayerId(0), LiengAction.Fold).state
        state = engine.apply(state, PlayerId(1), LiengAction.Fold).state
        state = engine.apply(state, PlayerId(2), LiengAction.Fold).state

        // Ván bài kết thúc, người cuối cùng thắng pot
        assertTrue(engine.isFinished(state))
        val settlement = engine.settle(state)
        assertEquals(0L, settlement.deltas.values.sum())
        assertEquals(300L, settlement.deltas[PlayerId(3)]) // Thắng 300 xu từ 3 người úp
    }

    @Test
    fun testShowdownSettlementZeroSum() {
        var state = engine.start(config, seed = 55555L)
        // Cả 4 người xem bài vòng 1
        state = engine.apply(state, PlayerId(0), LiengAction.Check).state
        state = engine.apply(state, PlayerId(1), LiengAction.Check).state
        state = engine.apply(state, PlayerId(2), LiengAction.Check).state
        state = engine.apply(state, PlayerId(3), LiengAction.Check).state

        // Vòng 2
        state = engine.apply(state, PlayerId(0), LiengAction.Check).state
        state = engine.apply(state, PlayerId(1), LiengAction.Check).state
        state = engine.apply(state, PlayerId(2), LiengAction.Check).state
        state = engine.apply(state, PlayerId(3), LiengAction.Check).state

        // Vòng 3
        state = engine.apply(state, PlayerId(0), LiengAction.Check).state
        state = engine.apply(state, PlayerId(1), LiengAction.Check).state
        state = engine.apply(state, PlayerId(2), LiengAction.Check).state
        state = engine.apply(state, PlayerId(3), LiengAction.Check).state

        assertTrue(engine.isFinished(state))
        val settlement = engine.settle(state)
        assertEquals(0L, settlement.deltas.values.sum())
    }
}
