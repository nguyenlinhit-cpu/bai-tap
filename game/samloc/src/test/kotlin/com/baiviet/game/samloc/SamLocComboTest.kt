package com.baiviet.game.samloc

import com.baiviet.core.cards.Rank
import com.baiviet.game.samloc.rules.SamLocBeatRules
import com.baiviet.game.samloc.rules.SamLocCombo
import com.baiviet.game.samloc.rules.SamLocComboType
import com.baiviet.game.samloc.rules.SamLocRules
import com.baiviet.game.samloc.rules.slRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamLocComboTest {
    private val defaultRules = SamLocRules.DEFAULT

    @Test
    fun `straights A-2-3 and 2-3-4 are valid`() {
        val a23 = SamLocCombo.classify(cards("As 2h 3d"))
        assertNotNull("A-2-3 phải là sảnh hợp lệ", a23)
        assertEquals(SamLocComboType.STRAIGHT, a23!!.type)
        assertEquals(Rank.THREE, a23.top.rank)

        val s234 = SamLocCombo.classify(cards("2c 3s 4h"))
        assertNotNull("2-3-4 phải là sảnh hợp lệ", s234)
        assertEquals(SamLocComboType.STRAIGHT, s234!!.type)
        assertEquals(Rank.FOUR, s234.top.rank)

        val qka = SamLocCombo.classify(cards("Qc Kd Ah"))
        assertNotNull("Q-K-A phải là sảnh hợp lệ", qka)
        assertEquals(SamLocComboType.STRAIGHT, qka!!.type)
        assertEquals(Rank.ACE, qka.top.rank)
    }

    @Test
    fun `K-A-2 and wrap around are invalid`() {
        val ka2 = SamLocCombo.classify(cards("Ks Ah 2d"))
        assertNull("K-A-2 không được là sảnh hợp lệ (không vòng qua)", ka2)

        val jqka2 = SamLocCombo.classify(cards("Js Qc Kd Ah 2s"))
        assertNull("J-Q-K-A-2 không hợp lệ", jqka2)
    }

    @Test
    fun `straight order with same length matches ending rank`() {
        val a23 = SamLocCombo.classify(cards("As 2h 3d"))!!
        val s234 = SamLocCombo.classify(cards("2c 3s 4h"))!!
        val s345 = SamLocCombo.classify(cards("3s 4d 5c"))!!
        val qka = SamLocCombo.classify(cards("Qs Kd Ah"))!!

        // A-2-3 < 2-3-4 < 3-4-5 < ... < Q-K-A
        assertTrue("2-3-4 phải chặn được A-2-3", SamLocBeatRules.canBeat(s234, a23, defaultRules))
        assertTrue("3-4-5 phải chặn được 2-3-4", SamLocBeatRules.canBeat(s345, s234, defaultRules))
        assertTrue("Q-K-A phải chặn được 3-4-5", SamLocBeatRules.canBeat(qka, s345, defaultRules))

        assertFalse("A-2-3 không thể chặn 2-3-4", SamLocBeatRules.canBeat(a23, s234, defaultRules))
        assertFalse("2-3-4 không thể chặn 3-4-5", SamLocBeatRules.canBeat(s234, s345, defaultRules))
    }

    @Test
    fun `no suit comparison - equal combos cannot beat each other`() {
        // Rác bằng nhau không chặn được
        val s7h = SamLocCombo.classify(cards("7h"))!!
        val s7s = SamLocCombo.classify(cards("7s"))!!
        assertFalse("7 cơ không chặn được 7 bích", SamLocBeatRules.canBeat(s7h, s7s, defaultRules))
        assertFalse("7 bích không chặn được 7 cơ", SamLocBeatRules.canBeat(s7s, s7h, defaultRules))

        // Đôi bằng nhau không chặn được
        val pair9A = SamLocCombo.classify(cards("9s 9h"))!!
        val pair9B = SamLocCombo.classify(cards("9c 9d"))!!
        assertFalse("Đôi 9 không chặn được đôi 9", SamLocBeatRules.canBeat(pair9A, pair9B, defaultRules))

        // Sảnh bằng nhau không chặn được
        val str1 = SamLocCombo.classify(cards("3s 4d 5c"))!!
        val str2 = SamLocCombo.classify(cards("3h 4c 5d"))!!
        assertFalse("Sảnh 3-4-5 không chặn được sảnh 3-4-5", SamLocBeatRules.canBeat(str1, str2, defaultRules))

        // Heo bằng nhau không chặn được
        val two1 = SamLocCombo.classify(cards("2h"))!!
        val two2 = SamLocCombo.classify(cards("2s"))!!
        assertFalse("Heo không chặn được heo", SamLocBeatRules.canBeat(two1, two2, defaultRules))
    }

    @Test
    fun `quad cuts single two`() {
        val two = SamLocCombo.classify(cards("2s"))!!
        val quad3 = SamLocCombo.classify(cards("3s 3c 3d 3h"))!!
        val quad5 = SamLocCombo.classify(cards("5s 5c 5d 5h"))!!

        assertTrue("Tứ quý 3 chặt được heo", SamLocBeatRules.canBeat(quad3, two, defaultRules))
        assertTrue("Tứ quý 3 là nước chặt", SamLocBeatRules.isCut(quad3, two, defaultRules))

        assertTrue("Tứ quý 5 chặt được tứ quý 3", SamLocBeatRules.canBeat(quad5, quad3, defaultRules))
        assertTrue("Tứ quý 5 chặt tứ quý 3 là nước chặt", SamLocBeatRules.isCut(quad5, quad3, defaultRules))

        assertFalse("Tứ quý 3 không chặt được tứ quý 5", SamLocBeatRules.canBeat(quad3, quad5, defaultRules))
    }

    @Test
    fun `quad cuts pair of twos only with house rule`() {
        val pairTwos = SamLocCombo.classify(cards("2s 2h"))!!
        val quad = SamLocCombo.classify(cards("9s 9c 9d 9h"))!!

        // Mặc định: tắt
        assertFalse(
            "Mặc định: tứ quý không chặt được đôi heo",
            SamLocBeatRules.canBeat(quad, pairTwos, defaultRules),
        )

        // Bật luật nhà
        val houseRules = defaultRules.copy(quadCutsPairOfTwos = true)
        assertTrue(
            "Khi bật luật nhà: tứ quý chặt được đôi heo",
            SamLocBeatRules.canBeat(quad, pairTwos, houseRules),
        )
    }

    @Test
    fun `no pair sequence in sam loc`() {
        // 3 đôi thông trong Tiến Lên không phải bộ hợp lệ trong Sâm
        val pairSeq = SamLocCombo.classify(cards("3s 3c 4s 4c 5s 5c"))
        assertNull("Sâm Lốc không có đôi thông", pairSeq)
    }
}
