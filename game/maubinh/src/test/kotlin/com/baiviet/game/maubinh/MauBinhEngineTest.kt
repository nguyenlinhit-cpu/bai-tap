package com.baiviet.game.maubinh

import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.maubinh.engine.MauBinhAction
import com.baiviet.game.maubinh.engine.MauBinhEngine
import com.baiviet.game.maubinh.engine.MauBinhPhase
import com.baiviet.game.maubinh.engine.MauBinhRevealStep
import com.baiviet.game.maubinh.rules.MauBinhRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MauBinhEngineTest {

    private val engine = MauBinhEngine()

    @Test
    fun `start deals 13 cards to each player and enters arranging phase`() {
        val table = TableConfig(playerCount = 4, rules = MauBinhRules.DEFAULT, betUnit = 100L)
        val state = engine.start(table, seed = 42L)

        assertEquals(4, state.players.size)
        assertEquals(MauBinhPhase.ARRANGING, state.phase)

        val allCards = state.players.flatMap { it.initialCards }
        assertEquals(52, allCards.size)
        assertEquals(52, allCards.distinct().size)

        for (p in state.players) {
            assertEquals(13, p.initialCards.size)
            assertFalse(p.isSubmitted)
        }

        // Ban đầu tất cả 4 người đều có quyền hành động cùng lúc
        assertEquals(4, engine.currentActors(state).size)
    }

    @Test
    fun `players submit arrangements and transition to revealing and finished`() {
        val table = TableConfig(playerCount = 3, rules = MauBinhRules.DEFAULT, betUnit = 100L)
        var state = engine.start(table, seed = 123L)

        // Từng người nộp bài bằng AutoArrange
        for (seat in 0 until 3) {
            val player = PlayerId(seat)
            assertTrue(player in engine.currentActors(state))
            val transition = engine.apply(state, player, MauBinhAction.AutoArrange)
            state = transition.state
        }

        // Sau khi cả 3 người đã nộp, tự động chuyển sang REVEALING
        assertEquals(MauBinhPhase.REVEALING, state.phase)
        assertEquals(MauBinhRevealStep.CHI_1, state.revealStep)
        assertFalse(engine.isFinished(state))

        // Bước lật so chi tuần tự
        // CHI_1 -> CHI_2
        state = engine.apply(state, PlayerId(0), MauBinhAction.NextRevealStep).state
        assertEquals(MauBinhRevealStep.CHI_2, state.revealStep)

        // CHI_2 -> CHI_3
        state = engine.apply(state, PlayerId(0), MauBinhAction.NextRevealStep).state
        assertEquals(MauBinhRevealStep.CHI_3, state.revealStep)

        // CHI_3 -> SUMMARY
        state = engine.apply(state, PlayerId(0), MauBinhAction.NextRevealStep).state
        assertEquals(MauBinhRevealStep.SUMMARY, state.revealStep)

        // SUMMARY -> FINISHED
        state = engine.apply(state, PlayerId(0), MauBinhAction.NextRevealStep).state
        assertEquals(MauBinhPhase.FINISHED, state.phase)
        assertTrue(engine.isFinished(state))

        // Thanh toán cuối ván
        val settlement = engine.settle(state)
        assertEquals(3, settlement.deltas.size)
        assertEquals(0L, settlement.deltas.values.sum())
    }

    @Test
    fun `viewOf hides other players cards until reveal step`() {
        val table = TableConfig(playerCount = 4, rules = MauBinhRules.DEFAULT, betUnit = 100L)
        var state = engine.start(table, seed = 999L)

        val v0 = engine.viewOf(state, PlayerId(0))
        assertEquals(13, v0.myCards.size)
        // Khi ARRANGING, bài của người khác bị ẩn toàn bộ
        for (other in v0.otherPlayers) {
            assertEquals(null, other.visibleChi1)
            assertEquals(null, other.visibleChi2)
            assertEquals(null, other.visibleChi3)
        }

        // Tất cả nộp bài
        for (i in 0 until 4) {
            state = engine.apply(state, PlayerId(i), MauBinhAction.AutoArrange).state
        }

        // Đang ở CHI_1
        val v1 = engine.viewOf(state, PlayerId(0))
        for (other in v1.otherPlayers) {
            assertEquals(5, other.visibleChi1?.size)
            assertEquals(null, other.visibleChi2)
            assertEquals(null, other.visibleChi3)
        }

        // Lật CHI_2
        state = engine.apply(state, PlayerId(0), MauBinhAction.NextRevealStep).state
        val v2 = engine.viewOf(state, PlayerId(0))
        for (other in v2.otherPlayers) {
            assertEquals(5, other.visibleChi1?.size)
            assertEquals(5, other.visibleChi2?.size)
            assertEquals(null, other.visibleChi3)
        }
    }
}
