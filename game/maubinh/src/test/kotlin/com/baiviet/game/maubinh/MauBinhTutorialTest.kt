package com.baiviet.game.maubinh

import com.baiviet.game.maubinh.rules.MauBinhArrangement
import com.baiviet.game.maubinh.rules.MauBinhEvaluator
import com.baiviet.game.maubinh.ui.MauBinhTutorial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cách xếp trong Ván tập Mậu binh phải khớp bộ luật thật. */
class MauBinhTutorialTest {
    @Test
    fun scriptedArrangementsMatchRules() {
        val t = MauBinhTutorial
        assertEquals(13, t.all.toSet().size)
        assertTrue("Sảnh trên, Thùng dưới phải là binh lủng", MauBinhArrangement(t.straight, t.flush, t.pair).isFoul())
        assertFalse(MauBinhArrangement(t.flush, t.straight, t.pair).isFoul())
        assertFalse(MauBinhArrangement(t.minhChi1, t.minhChi2, t.minhChi3).isFoul())
        // Bạn thắng cả 3 chi với Minh (sập 3 chi)
        assertTrue(MauBinhEvaluator.evaluateChi1OrChi2(t.flush) > MauBinhEvaluator.evaluateChi1OrChi2(t.minhChi1))
        assertTrue(MauBinhEvaluator.evaluateChi1OrChi2(t.straight) > MauBinhEvaluator.evaluateChi1OrChi2(t.minhChi2))
        assertTrue(MauBinhEvaluator.evaluateChi3(t.pair) > MauBinhEvaluator.evaluateChi3(t.minhChi3))
    }
}
