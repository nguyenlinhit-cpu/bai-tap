package com.baiviet.game.phom.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.SettlementLine

/**
 * Kết quả phân định của một người chơi cuối ván.
 */
data class PhomPlayerResult(
    val seat: Int,
    val isU: Boolean = false,
    val isUKhan: Boolean = false,
    val isMom: Boolean = false,
    val points: Int = 0,
    val melds: List<PhomMeld> = emptyList(),
    val remainingTrash: List<Card> = emptyList(),
    val meldOrder: Int = Int.MAX_VALUE, // Thứ tự hạ phỏm (ai hạ trước số này nhỏ hơn)
)

/**
 * Tính toán xếp hạng và trả thưởng / phạt trong Phỏm (Tá Lả).
 */
object PhomScoring {

    /**
     * Xếp hạng người chơi (trả về danh sách ghế từ Nhất đến Bét).
     *
     * Quy tắc:
     * 1. Ù / Ù khan luôn đứng đầu (Nhất).
     * 2. Người Móm (không có phỏm nào) luôn xếp sau cùng (sau tất cả người có phỏm).
     * 3. Các người có phỏm: điểm thấp hơn xếp trên.
     * 4. Bằng điểm: người HẠ TRƯỚC xếp trên (meldOrder nhỏ hơn).
     */
    fun rankPlayers(results: List<PhomPlayerResult>): List<Int> {
        val uWinner = results.firstOrNull { it.isU || it.isUKhan }
        if (uWinner != null) {
            val others = results.filter { it.seat != uWinner.seat }
                .sortedWith(
                    compareBy<PhomPlayerResult> { it.isMom }
                        .thenBy { it.points }
                        .thenBy { it.meldOrder },
                )
            return listOf(uWinner.seat) + others.map { it.seat }
        }

        val nonMom = results.filter { !it.isMom }
            .sortedWith(
                compareBy<PhomPlayerResult> { it.points }
                    .thenBy { it.meldOrder },
            )

        val mom = results.filter { it.isMom }
            .sortedBy { it.meldOrder }

        return (nonMom + mom).map { it.seat }
    }

    /**
     * Tạo các dòng thanh toán tiền cuối ván.
     */
    fun createSettlementLines(
        results: List<PhomPlayerResult>,
        rankedSeats: List<Int>,
        rules: PhomRules,
        betUnit: Long,
        eatLines: List<SettlementLine>,
        uDenSeat: Int?,
    ): List<SettlementLine> {
        val lines = mutableListOf<SettlementLine>()
        // 1. Giữ các khoản phạt tiền ăn bài phát sinh trong ván
        lines.addAll(eatLines)

        val winnerSeat = rankedSeats.first()
        val winnerPlayer = PlayerId(winnerSeat)
        val winnerResult = results.first { it.seat == winnerSeat }

        if (winnerResult.isU || winnerResult.isUKhan) {
            val uReward = rules.uWinCardsPerPlayer * betUnit
            val otherSeats = rankedSeats.drop(1)

            if (uDenSeat != null) {
                // Có người đền làng (ăn 3 cây Ù, đánh chốt Ù, hoặc ăn chốt đền)
                val denPlayer = PlayerId(uDenSeat)
                val totalDen = uReward * otherSeats.size
                val reason = when {
                    winnerResult.isUKhan -> "Đền Ù khan: −${rules.uWinCardsPerPlayer * otherSeats.size}B"
                    else -> "Đền Ù thay cả làng: −${rules.uWinCardsPerPlayer * otherSeats.size}B"
                }
                lines.add(
                    SettlementLine(
                        from = denPlayer,
                        to = winnerPlayer,
                        amount = totalDen,
                        reason = reason,
                    ),
                )
            } else {
                // Bình thường: mỗi người thua trả người Ù 5B
                for (seat in otherSeats) {
                    lines.add(
                        SettlementLine(
                            from = PlayerId(seat),
                            to = winnerPlayer,
                            amount = uReward,
                            reason = if (winnerResult.isUKhan) "Thua Ù khan: −${rules.uWinCardsPerPlayer}B" else "Thua Ù: −${rules.uWinCardsPerPlayer}B",
                        ),
                    )
                }
            }
            return lines
        }

        // Kết thúc thông thường (tính điểm theo thứ hạng Nhất, Nhì, Ba, Bét, Móm)
        for (idx in 1 until rankedSeats.size) {
            val loserSeat = rankedSeats[idx]
            val loserResult = results.first { it.seat == loserSeat }
            val loserPlayer = PlayerId(loserSeat)

            val (cardsPay, rankName) = when {
                loserResult.isMom -> rules.momPayCards to "Móm"
                idx == 1 -> rules.rank2PayCards to "Nhì (${loserResult.points}đ)"
                idx == 2 -> rules.rank3PayCards to "Ba (${loserResult.points}đ)"
                else -> rules.rank4PayCards to "Bét (${loserResult.points}đ)"
            }

            lines.add(
                SettlementLine(
                    from = loserPlayer,
                    to = winnerPlayer,
                    amount = cardsPay * betUnit,
                    reason = "Về $rankName: −${cardsPay}B",
                ),
            )
        }

        return lines
    }
}
