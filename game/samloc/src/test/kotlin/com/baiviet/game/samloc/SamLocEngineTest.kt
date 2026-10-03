package com.baiviet.game.samloc

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.game.samloc.engine.SamLocEngine
import com.baiviet.game.samloc.engine.SlAction
import com.baiviet.game.samloc.rules.SamLocRules
import com.baiviet.game.samloc.rules.sortedSl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamLocEngineTest {
    private val engine = SamLocEngine()
    private val rules = SamLocRules.DEFAULT
    private val betUnit = 100L

    @Test
    fun `bao sam success - caller wins 20 cards from everyone, zero sum`() {
        // Seat 0 có 2 sảnh 5 lá: 9..K và 3..7 (không phải sảnh rồng 10 lá để không bị ăn trắng)
        val s1Cards = cards("9s 10c Jd Qh Ks")
        val s2Cards = cards("3s 4c 5d 6h 7s")
        val p0 = s1Cards + s2Cards
        val p1 = cards("3c 3h 4d 4h 5h 6s 7c 8d Jc Qd")
        val hands = listOf(p0, p1)

        val s0 = engine.startWithHands(rules, 2, betUnit, hands, previousWinner = PlayerId(0))

        // Pha báo Sâm: seat 0 Báo Sâm
        val t1 = engine.apply(s0, PlayerId(0), SlAction.CallSam)
        val s1 = t1.state
        assertEquals(0, s1.samCaller)
        assertEquals(0, s1.turn)

        // Seat 0 đánh sảnh 5 lá: 9..K
        val t2 = engine.apply(s1, PlayerId(0), SlAction.Play(s1Cards))
        val s2 = t2.state
        assertEquals(1, s2.turn)

        // Seat 1 bỏ lượt
        val t3 = engine.apply(s2, PlayerId(1), SlAction.Pass)
        val s3 = t3.state
        assertEquals(0, s3.turn)

        // Seat 0 đánh tiếp sảnh 5 lá còn lại: 3..7 và về luôn
        val t4 = engine.apply(s3, PlayerId(0), SlAction.Play(s2Cards))
        val s4 = t4.state
        assertTrue("Ván phải kết thúc", s4.finished)
        assertEquals(0, s4.winner)

        val settlement = engine.settle(s4)
        assertEquals(0L, settlement.deltas.values.sum())
        assertEquals(20 * betUnit, settlement.deltas[PlayerId(0)])
        assertEquals(-20 * betUnit, settlement.deltas[PlayerId(1)])
    }

    @Test
    fun `bao sam beaten - caller pays 20 cards to everyone, ends immediately`() {
        val p0 = cards("3s 4c 5d 6h 7s 9s 10c Jd 2s 2c")
        val p1 = cards("9c 9d 9h 9s 3c 4d 5h 6s 7c 8d") // p1 có tứ quý 9
        val hands = listOf(p0, p1)

        val s0 = engine.startWithHands(rules, 2, betUnit, hands, previousWinner = PlayerId(0))

        // Seat 0 Báo Sâm
        val s1 = engine.apply(s0, PlayerId(0), SlAction.CallSam).state

        // Seat 0 đánh lá 2s
        val s2 = engine.apply(s1, PlayerId(0), SlAction.Play(cards("2s"))).state
        assertEquals(1, s2.turn)

        // Seat 1 dùng tứ quý 9 chặt 2s của người Báo Sâm!
        val s3 = engine.apply(s2, PlayerId(1), SlAction.Play(cards("9c 9d 9h 9s"))).state

        assertTrue("Ván phải dừng ngay khi người Báo Sâm bị chặn", s3.finished)
        assertTrue("Sâm bị chặn", s3.samBeaten)

        val settlement = engine.settle(s3)
        assertEquals("Tổng delta phải = 0", 0L, settlement.deltas.values.sum())
        assertEquals(-20 * betUnit, settlement.deltas[PlayerId(0)])
        assertEquals(20 * betUnit, settlement.deltas[PlayerId(1)])
    }

    @Test
    fun `bao 1 violation causes violator to pay for entire village`() {
        // 3 người chơi: Seat 0, Seat 1, Seat 2
        // Thứ tự đi: 0 -> 1 -> 2 -> 0
        // Seat 1 chỉ còn 1 lá (Báo 1)
        // Seat 0 ngồi ngay trước Seat 1
        val p0 = cards("3s 2h") // Seat 0 có lá 3s và lá 2h
        val p1 = cards("9s") // Seat 1 còn đúng 1 lá 9s (Báo 1!)
        val p2 = cards("4s 5s 6s 7s") // Seat 2 còn 4 lá
        val hands = listOf(p0, p1, p2)

        val s0 = engine.startWithHands(rules, 3, betUnit, hands, previousWinner = PlayerId(0))

        // Cả 3 người bỏ qua pha Báo Sâm
        val s1 = engine.apply(s0, PlayerId(0), SlAction.SkipSam).state
        val s2 = engine.apply(s1, PlayerId(1), SlAction.SkipSam).state
        val s3 = engine.apply(s2, PlayerId(2), SlAction.SkipSam).state

        // Đến lượt Seat 0 đi tự do. Seat 1 đang Báo 1.
        // Seat 0 có 3s và 2h. Lá rác lớn nhất là 2h.
        // Nhưng Seat 0 đánh 3s (vi phạm luật giữ cửa Báo 1!)
        val s4 = engine.apply(s3, PlayerId(0), SlAction.Play(cards("3s"))).state
        assertEquals("Seat 0 phải bị đánh dấu vi phạm Báo 1", 0, s4.violatorOfBao1)
        assertEquals(1, s4.turn)

        // Seat 1 đánh 9s đè 3s và về Nhất ngay!
        val s5 = engine.apply(s4, PlayerId(1), SlAction.Play(cards("9s"))).state
        assertTrue("Ván kết thúc", s5.finished)
        assertEquals(1, s5.winner)

        val settlement = engine.settle(s5)
        assertEquals("Zero-sum", 0L, settlement.deltas.values.sum())

        // Seat 2 chưa đánh được lá nào (bị cóng = 15 lá). Nhưng do Seat 0 phạm luật Báo 1,
        // Seat 2 KHÔNG phải trả đồng nào (delta = 0).
        assertEquals("Seat 2 được tha tiền nhờ luật đền làng", 0L, settlement.deltas[PlayerId(2)])

        // Seat 0 phải trả tiền cho tay bài của chính mình (còn 1 con 2h = thối heo 15 lá)
        // CỘNG THÊM 15 lá cóng của Seat 2 = tổng cộng 30 lá!
        val seat0Loss = -(15 + 15) * betUnit
        assertEquals(seat0Loss, settlement.deltas[PlayerId(0)])
        assertEquals(-seat0Loss, settlement.deltas[PlayerId(1)])
    }

    @Test
    fun `finishing with two causes thoi 2 penalty and ends game`() {
        val p0 = cards("3s 2h") // p0 đánh 3s trước, sau đó còn lại 2h
        val p1 = cards("4s 5s")
        val hands = listOf(p0, p1)

        val s0 = engine.startWithHands(rules, 2, betUnit, hands, previousWinner = PlayerId(0))
        val s1 = engine.apply(s0, PlayerId(0), SlAction.SkipSam).state
        val s2 = engine.apply(s1, PlayerId(1), SlAction.SkipSam).state

        // p0 đánh 3s
        val s3 = engine.apply(s2, PlayerId(0), SlAction.Play(cards("3s"))).state
        // p1 bỏ lượt
        val s4 = engine.apply(s3, PlayerId(1), SlAction.Pass).state

        // p0 đi tự do và đánh lá cuối cùng là lá 2h -> VỀ BẰNG HEO!
        val s5 = engine.apply(s4, PlayerId(0), SlAction.Play(cards("2h"))).state

        assertTrue("Ván phải kết thúc", s5.finished)
        assertEquals(0, s5.finishWithTwoPlayer)
        assertNull("Người về bằng heo không được tính thắng", s5.winner)

        val settlement = engine.settle(s5)
        assertEquals("Zero-sum", 0L, settlement.deltas.values.sum())
        assertEquals(-15 * betUnit, settlement.deltas[PlayerId(0)])
        assertEquals(15 * betUnit, settlement.deltas[PlayerId(1)])
    }

    @Test
    fun `bot view does not leak opponents hidden cards`() {
        val p0 = cards("3s 4c 5d 6h 7s 8c 9d 10h Js 2s")
        val p1 = cards("3c 4d 5h 6s 7c 8d 9h 10s Jc 2c")
        val hands = listOf(p0, p1)

        val state = engine.startWithHands(rules, 2, betUnit, hands, previousWinner = PlayerId(0))
        val view1 = engine.viewOf(state, PlayerId(1))

        assertEquals(1, view1.me)
        assertEquals(p1.sortedSl(), view1.myHand)
        // view1 không chứa danh sách bài p0
        assertEquals(listOf(10, 10), view1.handCounts)
    }
}
