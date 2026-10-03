package com.baiviet.game.lieng

import com.baiviet.game.lieng.rules.LiengEvaluator
import com.baiviet.game.lieng.rules.LiengHandType
import com.baiviet.game.lieng.rules.LiengRules
import com.baiviet.game.lieng.ui.LiengTutorial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Các bộ bài trong Ván tập Liêng phải khớp bộ luật thật. */
class LiengTutorialTest {
    private val rules = LiengRules.DEFAULT

    @Test
    fun scriptedHandsMatchRules() {
        val r1Me = LiengEvaluator.evaluate(LiengTutorial.round1Me)
        assertEquals(LiengHandType.DIEM, r1Me.type)
        assertEquals(8, r1Me.points)
        val r1Minh = LiengEvaluator.evaluate(LiengTutorial.round1Minh)
        assertEquals(LiengHandType.SAP, r1Minh.type)
        assertTrue(LiengEvaluator.compare(r1Minh, r1Me, rules) > 0)
        val r2Me = LiengEvaluator.evaluate(LiengTutorial.round2Me)
        val r2Minh = LiengEvaluator.evaluate(LiengTutorial.round2Minh)
        assertEquals(LiengHandType.LIENG, r2Me.type)
        assertEquals(LiengHandType.ANH, r2Minh.type)
        assertTrue(LiengEvaluator.compare(r2Me, r2Minh, rules) > 0)
        assertEquals(LiengHandType.LIENG, LiengEvaluator.evaluate(LiengTutorial.jqk).type)
    }
}
