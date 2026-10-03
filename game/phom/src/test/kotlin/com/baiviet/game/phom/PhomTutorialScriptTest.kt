package com.baiviet.game.phom

import com.baiviet.core.engine.PlayerId
import com.baiviet.game.phom.engine.PhomAction
import com.baiviet.game.phom.engine.PhomEngine
import com.baiviet.game.phom.engine.PhomTutorialScript
import com.baiviet.game.phom.engine.PhomTutorialStep
import com.baiviet.game.phom.rules.PhomRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhomTutorialScriptTest {

    @Test
    fun `tutorial script steps execute successfully with valid transitions`() {
        val engine = PhomEngine()
        val hands = listOf(PhomTutorialScript.YOU, PhomTutorialScript.BOT)
        // Leader is 1 (BOT) so BOT starts with 10 cards and phase DISCARD
        var state = engine.startWithHands(
            rules = PhomRules(uKhanEnabled = false),
            playerCount = 2,
            betUnit = 100L,
            hands = hands,
            stock = PhomTutorialScript.STOCK,
            leader = 1,
        )

        for (step in PhomTutorialScript.STEPS) {
            when (step) {
                is PhomTutorialStep.Bot -> {
                    assertEquals("Lượt của máy (ghế 1)", 1, state.turn)
                    val trans = engine.apply(state, PlayerId(1), step.action)
                    state = trans.state
                }
                is PhomTutorialStep.Human -> {
                    assertEquals("Lượt của bạn (ghế 0)", 0, state.turn)
                    val legal = engine.legalActions(state, PlayerId(0))
                    assertTrue("Nước đi người chơi phải nằm trong hành động hợp lệ", legal.any { step.checkAction(it) })
                    val action = legal.first { step.checkAction(it) }
                    val trans = engine.apply(state, PlayerId(0), action)
                    state = trans.state
                }
            }
        }

        // Sau 4 lượt hướng dẫn (2 vòng), người chơi hoàn thành các bài học bốc, ăn, đánh rác
        assertTrue("Sau ván tập người chơi còn bài trên tay", state.hands[0].isNotEmpty())
    }
}
