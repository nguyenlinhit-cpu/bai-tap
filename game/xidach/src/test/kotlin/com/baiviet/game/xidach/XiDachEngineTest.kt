package com.baiviet.game.xidach

import com.baiviet.core.engine.TableConfig
import com.baiviet.game.xidach.engine.XiDachAction
import com.baiviet.game.xidach.engine.XiDachEngine
import com.baiviet.game.xidach.engine.XiDachPhase
import com.baiviet.game.xidach.rules.XiDachRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XiDachEngineTest {

    private val engine = XiDachEngine()
    private val config = TableConfig(playerCount = 4, rules = XiDachRules(), betUnit = 100L)

    @Test
    fun testStartGameHasValidPhase() {
        val state = engine.start(config, seed = 12345L)
        assertNotNull(state)
        assertEquals(4, state.players.size)
        // Mỗi người có 2 lá ban đầu
        for (pid in state.players) {
            assertEquals(2, state.playerStates[pid]!!.cards.size)
        }
        assertTrue(state.phase == XiDachPhase.PLAYER_TURNS || state.phase == XiDachPhase.FINISHED)
    }

    @Test
    fun testLegalActionsForCurrentActor() {
        val state = engine.start(config, seed = 54321L)
        if (state.phase == XiDachPhase.PLAYER_TURNS) {
            val actor = state.currentActor!!
            val legal = engine.legalActions(state, actor)
            assertTrue(legal.isNotEmpty())
            // Phải có ít nhất Rút (Hit) nếu chưa đủ tuổi hoặc chưa quắc
            assertTrue(legal.contains(XiDachAction.Hit) || legal.contains(XiDachAction.Stand))
        }
    }

    @Test
    fun testSettlementZeroSum() {
        // Tìm 1 seed kết thúc hoặc chơi xong và settle
        var state = engine.start(config, seed = 99999L)
        while (!engine.isFinished(state)) {
            val actor = state.currentActor ?: break
            val legal = engine.legalActions(state, actor)
            if (legal.isEmpty()) break
            val action = if (legal.contains(XiDachAction.Stand)) XiDachAction.Stand else legal.first()
            state = engine.apply(state, actor, action).state
        }

        if (engine.isFinished(state)) {
            val settlement = engine.settle(state)
            assertEquals(0L, settlement.deltas.values.sum())
        }
    }
}
