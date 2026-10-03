package com.baiviet.game.samloc.rules

/**
 * Luật chặn và chặt trong Sâm Lốc.
 *
 * Nguyên tắc cốt lõi:
 * 1. KHÔNG SO CHẤT: hai bộ có cùng giá trị số thì không thể chặn nhau.
 * 2. Chặn thường: cùng loại, cùng số lá, lá quyết định (top) có số lớn hơn.
 * 3. Chặt:
 *    - Tứ quý chặt 1 heo.
 *    - Tứ quý lớn chặt tứ quý nhỏ hơn.
 *    - Tứ quý chặt đôi heo (nếu bật Luật nhà [SamLocRules.quadCutsPairOfTwos]).
 */
object SamLocBeatRules {
    /** [candidate] có được đánh đè lên [top] không (chặn thường hoặc chặt). */
    fun canBeat(
        candidate: SamLocCombo,
        top: SamLocCombo,
        rules: SamLocRules,
    ): Boolean = isNormalBeat(candidate, top) || isCut(candidate, top, rules)

    /** Chặn thường: cùng loại, cùng số lá, rank của top lớn hơn (không so chất). */
    fun isNormalBeat(
        candidate: SamLocCombo,
        top: SamLocCombo,
    ): Boolean =
        candidate.type == top.type &&
            candidate.size == top.size &&
            candidate.top.slRank > top.top.slRank

    /**
     * Nước đánh là "chặt" (tính tiền chặt):
     * - Tứ quý chặt 1 heo
     * - Tứ quý lớn chặt tứ quý nhỏ
     * - Tứ quý chặt đôi heo (nếu bật luật)
     */
    fun isCut(
        candidate: SamLocCombo,
        top: SamLocCombo,
        rules: SamLocRules,
    ): Boolean {
        if (!candidate.isQuad) return false
        return when {
            // Tứ quý chặt 1 heo
            top.type == SamLocComboType.SINGLE && top.top.isTwo -> true

            // Tứ quý chặt đôi heo (Luật nhà)
            top.type == SamLocComboType.PAIR && top.isTwos && rules.quadCutsPairOfTwos -> true

            // Tứ quý chặt tứ quý nhỏ hơn
            top.type == SamLocComboType.QUAD -> candidate.top.slRank > top.top.slRank

            else -> false
        }
    }
}
