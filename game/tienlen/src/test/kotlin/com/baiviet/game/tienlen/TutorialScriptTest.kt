package com.baiviet.game.tienlen

import com.baiviet.core.engine.GameEvent
import com.baiviet.core.engine.PlayerId
import com.baiviet.game.tienlen.engine.TienLenEngine
import com.baiviet.game.tienlen.engine.TlAction
import com.baiviet.game.tienlen.engine.TutorialScript
import com.baiviet.game.tienlen.engine.TutorialStep
import com.baiviet.game.tienlen.rules.TienLenRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kịch bản ván tập phải hợp lệ từng bước theo đúng engine. */
class TutorialScriptTest {

    @Test
    fun `tutorial script is legal and ends with player winning`() {
        val engine = TienLenEngine()
        var s = engine.startWithHands(TienLenRules(), 2, 100L, listOf(TutorialScript.YOU, TutorialScript.BOT))
        assertFalse("Bài ván tập không được tới trắng", s.finished)
        assertEquals(0, s.turn)
        var sawCut = false
        for (step in TutorialScript.STEPS) {
            val (seat, action) = when (step) {
                is TutorialStep.Human -> 0 to (step.cards?.let { TlAction.Play(it.toList()) } ?: TlAction.Pass)
                is TutorialStep.Bot -> 1 to step.action
            }
            assertEquals("Sai lượt ở bước $step", seat, s.turn)
            val t = engine.apply(s, PlayerId(seat), action)
            if (t.events.any { it is GameEvent.Cut }) sawCut = true
            s = t.state
        }
        assertTrue(s.finished)
        assertTrue(sawCut)
        assertEquals(listOf(0), s.finishOrder)
        assertTrue(engine.settle(s).deltas[PlayerId(0)]!! > 0)
    }
}
