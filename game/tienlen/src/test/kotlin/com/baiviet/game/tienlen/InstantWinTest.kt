package com.baiviet.game.tienlen

import com.baiviet.game.tienlen.rules.InstantWin
import com.baiviet.game.tienlen.rules.InstantWinType
import com.baiviet.game.tienlen.rules.TienLenRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstantWinTest {

    private val rules = TienLenRules()

    @Test
    fun dragon() {
        val hand = cards("3s 4h 5d 6c 7s 8h 9d 10c Js Qh Kd Ac 9s")
        assertEquals(InstantWinType.DRAGON, InstantWin.detect(hand, rules, false))
    }

    @Test
    fun `four twos`() {
        val hand = cards("2s 2h 2d 2c 3s 5h 7d 9c Js Kh Kd Ac 9s")
        assertEquals(InstantWinType.FOUR_TWOS, InstantWin.detect(hand, rules, false))
    }

    @Test
    fun `five consecutive pairs`() {
        val hand = cards("3s 3h 4s 4h 5d 5c 6s 6h 7d 7c Js Kh 2d")
        assertEquals(InstantWinType.FIVE_PAIR_SEQUENCE, InstantWin.detect(hand, rules, false))
        assertEquals(
            null,
            InstantWin.detect(hand, rules.copy(fiveConsecutivePairsInstantWin = false), false),
        )
    }

    @Test
    fun `six pairs`() {
        val hand = cards("3s 3h 5s 5h 7d 7c 9s 9h Jd Jc Ks Kh 2d")
        assertEquals(InstantWinType.SIX_PAIRS, InstantWin.detect(hand, rules, false))
    }

    @Test
    fun `same color 13 and 12 with house rule`() {
        val red13 = cards("3h 4h 5h 7h 9h Jh Kh 2h 3d 6d 8d 10d Qd")
        assertEquals(InstantWinType.SAME_COLOR, InstantWin.detect(red13, rules, false))
        val red12 = cards("3h 4h 5h 7h 9h Jh Kh 2h 3d 6d 8d 10d Qs")
        assertNull(InstantWin.detect(red12, rules, false))
        assertEquals(
            InstantWinType.SAME_COLOR,
            InstantWin.detect(red12, rules.copy(sameColorInstantWinCount = 12), false),
        )
    }

    @Test
    fun `four threes only first game`() {
        val hand = cards("3s 3h 3d 3c 5s 7h 9d Jc Ks Ah 2d 6c 8s")
        assertEquals(InstantWinType.FOUR_THREES, InstantWin.detect(hand, rules, true))
        assertNull(InstantWin.detect(hand, rules, false))
    }

    @Test
    fun `resolve prefers stronger type then higher card`() {
        val hands = listOf(
            cards("3s 3h 5s 5h 7d 7c 9s 9h Jd Jc Ks Kh 2d"),
            cards("3d 4h 5d 6c 7s 8h 9d 10c Js Qh Kd Ac 9c"),
        )
        val winner = InstantWin.resolve(
            mapOf(0 to InstantWinType.SIX_PAIRS, 1 to InstantWinType.DRAGON),
            hands,
        )
        assertEquals(1, winner)
        val tie = InstantWin.resolve(
            mapOf(0 to InstantWinType.SIX_PAIRS, 1 to InstantWinType.SIX_PAIRS),
            hands,
        )
        assertEquals(0, tie) // tay 0 có 2♦ lớn nhất
    }
}
