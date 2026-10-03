package com.baiviet.core.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class SettlementTest {

    private val p0 = PlayerId(0)
    private val p1 = PlayerId(1)
    private val p2 = PlayerId(2)

    @Test
    fun `fromLines computes zero-sum deltas`() {
        val s = Settlement.fromLines(
            listOf(p0, p1, p2),
            listOf(
                SettlementLine(p1, p0, 500, "a"),
                SettlementLine(p2, p0, 300, "b"),
            ),
        )
        assertEquals(800L, s.deltas[p0])
        assertEquals(-500L, s.deltas[p1])
        assertEquals(-300L, s.deltas[p2])
    }

    @Test
    fun `cappedBy never takes more than balance and stays zero-sum`() {
        val s = Settlement.fromLines(
            listOf(p0, p1, p2),
            listOf(
                SettlementLine(p1, p0, 500, "a"),
                SettlementLine(p1, p2, 400, "b"),
            ),
        )
        val capped = s.cappedBy(mapOf(p0 to 1000L, p1 to 600L, p2 to 1000L))
        assertEquals(-600L, capped.deltas[p1])
        assertEquals(500L, capped.deltas[p0])
        assertEquals(100L, capped.deltas[p2])
        assertEquals(0L, capped.deltas.values.sum())
    }

    @Test
    fun `cappedBy drops lines of broke payer`() {
        val s = Settlement.fromLines(listOf(p0, p1), listOf(SettlementLine(p1, p0, 500, "a")))
        val capped = s.cappedBy(mapOf(p0 to 10L, p1 to 0L))
        assertEquals(0, capped.lines.size)
        assertEquals(0L, capped.deltas[p0])
    }
}
