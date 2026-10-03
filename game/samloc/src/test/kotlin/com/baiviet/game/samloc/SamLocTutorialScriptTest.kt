package com.baiviet.game.samloc

import com.baiviet.core.engine.PlayerId
import com.baiviet.game.samloc.engine.SamLocEngine
import com.baiviet.game.samloc.engine.SamLocTutorialScript
import com.baiviet.game.samloc.engine.SlTutorialStep
import com.baiviet.game.samloc.rules.SamLocRules
import com.baiviet.game.samloc.rules.sortedSl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SamLocTutorialScriptTest {
    @Test
    fun `tutorial script completes successfully with Bao Sam win`() {
        val engine = SamLocEngine()
        val hands = listOf(SamLocTutorialScript.YOU.sortedSl(), SamLocTutorialScript.BOT.sortedSl())
        var state =
            engine.startWithHands(
                rules = SamLocRules.DEFAULT,
                playerCount = 2,
                betUnit = 100L,
                hands = hands,
                previousWinner = PlayerId(0),
            )

        for (step in SamLocTutorialScript.STEPS) {
            when (step) {
                is SlTutorialStep.Human -> {
                    assertEquals("Lượt của người chơi (0)", 0, state.turn)
                    val trans = engine.apply(state, PlayerId(0), step.expectedAction)
                    state = trans.state
                }

                is SlTutorialStep.Bot -> {
                    assertEquals("Lượt của máy (1)", 1, state.turn)
                    val trans = engine.apply(state, PlayerId(1), step.action)
                    state = trans.state
                }
            }
        }

        assertTrue("Ván tập kết thúc", state.finished)
        assertEquals("Người chơi thắng Ăn Sâm", 0, state.winner)
        val settlement = engine.settle(state)
        assertEquals(0L, settlement.deltas.values.sum())
    }
}
