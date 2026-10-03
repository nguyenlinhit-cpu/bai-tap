package com.baiviet.game.tienlen.rules

/**
 * Luật chặn và chặt.
 *
 * Chặn thường: cùng loại, cùng số lá, lá lớn nhất lớn hơn.
 *
 * Chặt (mặc định):
 * | Hàng          | Chặt được                                                   |
 * |---------------|-------------------------------------------------------------|
 * | 3 đôi thông   | 1 heo; 3 đôi thông nhỏ hơn                                  |
 * | Tứ quý        | 1 heo, đôi heo (Luật nhà), 3 đôi thông, tứ quý nhỏ hơn      |
 * | 4 đôi thông   | 1 heo, đôi heo, 3 đôi thông, tứ quý, 4 đôi thông nhỏ hơn    |
 *
 * Sám heo không gì chặt được.
 */
object BeatRules {

    /** [candidate] có được đánh đè lên [top] không (chặn thường hoặc chặt). */
    fun canBeat(candidate: Combo, top: Combo, rules: TienLenRules): Boolean =
        isNormalBeat(candidate, top) || isCut(candidate, top, rules)

    /** Chặn thường: cùng loại, cùng số lá, lá lớn nhất lớn hơn. */
    fun isNormalBeat(candidate: Combo, top: Combo): Boolean =
        candidate.type == top.type &&
            candidate.size == top.size &&
            candidate.top.tlValue > top.top.tlValue

    /**
     * Nước đánh là "chặt" (có tính tiền): đánh hàng lên heo hoặc lên hàng.
     * Bao gồm cả hàng cùng loại lớn hơn (3 đôi thông chặt 3 đôi thông nhỏ hơn…).
     */
    fun isCut(candidate: Combo, top: Combo, rules: TienLenRules): Boolean {
        if (!candidate.isBomb) return false
        val pairs = candidate.pairCount
        return when {
            // Heo lẻ
            top.type == ComboType.SINGLE && top.isTwos ->
                candidate.type == ComboType.QUAD || pairs >= 3
            // Đôi heo
            top.type == ComboType.PAIR && top.isTwos ->
                (candidate.type == ComboType.QUAD && rules.quadCutsPairOfTwos) || pairs >= 4
            // 3 đôi thông (hoặc dài hơn, khi so với đôi thông ≥ 4 thì xét dưới)
            top.type == ComboType.PAIR_SEQUENCE && top.pairCount == 3 -> when {
                candidate.type == ComboType.QUAD -> true
                pairs == 3 -> candidate.top.tlValue > top.top.tlValue
                pairs >= 4 -> true
                else -> false
            }
            top.type == ComboType.QUAD -> when {
                candidate.type == ComboType.QUAD -> candidate.top.tlValue > top.top.tlValue
                pairs >= 4 -> true
                else -> false
            }
            top.type == ComboType.PAIR_SEQUENCE && top.pairCount >= 4 ->
                pairs > top.pairCount ||
                    (pairs == top.pairCount && candidate.top.tlValue > top.top.tlValue)
            else -> false
        }
    }

    /**
     * Có phải nước chặt "không cần vòng" (4 đôi thông trở lên) — được đánh dù đã bỏ lượt
     * khi bật [TienLenRules.fourPairsCutWithoutTurn].
     */
    fun isOutOfTurnCut(candidate: Combo, top: Combo, rules: TienLenRules): Boolean =
        rules.fourPairsCutWithoutTurn &&
            candidate.type == ComboType.PAIR_SEQUENCE &&
            candidate.pairCount >= 4 &&
            isCut(candidate, top, rules)
}
