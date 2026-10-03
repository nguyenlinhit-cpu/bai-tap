package com.baiviet.game.xidach.ui

import androidx.compose.runtime.Composable
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.tutorial.ScriptedTutorial
import com.baiviet.core.ui.tutorial.TutorialRow
import com.baiviet.core.ui.tutorial.TutorialStep

private fun c(r: Rank, s: Suit) = Card(r, s)

/** Bài sắp sẵn của ván tập Xì dách. */
internal object XiDachTutorial {
    private val tenC = c(Rank.TEN, Suit.CLUB)
    private val fourD = c(Rank.FOUR, Suit.DIAMOND)
    private val aceH = c(Rank.ACE, Suit.HEART)
    private val fiveS = c(Rank.FIVE, Suit.SPADE)
    private val minh = listOf(c(Rank.TWO, Suit.CLUB), c(Rank.THREE, Suit.HEART), c(Rank.FOUR, Suit.SPADE), c(Rank.FIVE, Suit.DIAMOND), c(Rank.ACE, Suit.CLUB))
    private val dealer = listOf(c(Rank.KING, Suit.SPADE), c(Rank.FIVE, Suit.HEART))
    private val dealerDraw = c(Rank.THREE, Suit.CLUB)

    private fun you(cards: List<Card>, note: String) = TutorialRow("Bạn", cards, note, isYou = true, highlight = true)
    private fun dealerRow(cards: List<Card?>, note: String? = null) = TutorialRow("👑 Nhà cái (Máy)", cards, note)
    private fun minhRow(cards: List<Card?>, note: String? = null) = TutorialRow("Minh", cards, note)

    val steps = listOf(
        TutorialStep(
            coach = "Bạn được chia 10♣ và 4♦ = 14 điểm. Dưới 16 gọi là Non — nhà con chưa được Dằn, phải Rút thêm. Bấm Rút.",
            rows = listOf(dealerRow(listOf(null, null)), minhRow(listOf(null, null)), you(listOf(tenC, fourD), "14 điểm · Non")),
            actions = listOf("Dằn", "Rút"),
            correct = 1,
            wrongHint = "14 điểm còn Non (dưới 16) nên chưa được Dằn — hãy Rút.",
        ),
        TutorialStep(
            coach = "Rút được A♥. Với 3 lá, lá A tính 1 hoặc 10: 14 + 10 = 24 sẽ quắc, nên A tự tính 1 → 15 điểm, vẫn Non. " +
                "Rút tiếp — lần này bấm Nặn bài rồi kéo góc lá để hé từ từ.",
            rows = listOf(dealerRow(listOf(null, null)), minhRow(listOf(null, null)), you(listOf(tenC, fourD, aceH), "15 điểm · Non")),
            actions = listOf("Dằn", "Nặn bài"),
            correct = 1,
            squeeze = fiveS,
            wrongHint = "15 điểm vẫn Non — phải rút thêm.",
        ),
        TutorialStep(
            coach = "Lá 5♠! Bạn có 20 điểm — đủ tuổi và rất cao. Rút nữa dễ quắc (quá 21). Bấm Dằn.",
            rows = listOf(dealerRow(listOf(null, null)), minhRow(listOf(null, null)), you(listOf(tenC, fourD, aceH, fiveS), "20 điểm")),
            actions = listOf("Rút", "Dằn"),
            correct = 1,
            wrongHint = "20 điểm rồi, rút thêm chỉ cần lá ≥ 2 là quắc. Hãy Dằn.",
        ),
        TutorialStep(
            coach = "Minh rút đủ 5 lá mà tổng chỉ 15 (A tính 1) → Ngũ linh! Ngũ linh thắng cả 21 điểm thường và ăn gấp đôi.",
            rows = listOf(dealerRow(listOf(null, null)), minhRow(minh, "Ngũ linh 15"), you(listOf(tenC, fourD, aceH, fiveS), "20 điểm · Dằn")),
            banner = "NGŨ LINH!",
        ),
        TutorialStep(
            coach = "Đến lượt cái: K♠ 5♥ = 15. Cái đủ 15 mới được Xét. Cái rút thêm 3♣ = 18 rồi Xét bạn: 20 > 18 — bạn thắng 1 cược. " +
                "Minh có Ngũ linh nên cái trả Minh 2 cược.",
            rows = listOf(dealerRow(dealer + dealerDraw, "18 điểm"), minhRow(minh, "+2 cược"), you(listOf(tenC, fourD, aceH, fiveS), "+1 cược")),
        ),
        TutorialStep(
            coach = "Ván sau bạn làm cái: 9♠ 6♥ = 15, đủ để Xét. Minh đã Dằn 2 lá rồi rút quắc 23. " +
                "Rút thêm bạn chỉ cần lá ≥ 7 là quắc — hãy Xét tất cả ngay.",
            rows = listOf(
                minhRow(listOf(null, null, null), "Đã dằn"),
                TutorialRow("👑 Bạn (cái)", listOf(c(Rank.NINE, Suit.SPADE), c(Rank.SIX, Suit.HEART)), "15 điểm", isYou = true, highlight = true),
            ),
            actions = listOf("Rút", "Xét tất cả"),
            correct = 1,
            wrongHint = "Đủ 15 rồi, rút thêm rủi ro — bấm Xét tất cả.",
        ),
        TutorialStep(
            coach = "Lật bài Minh: 6♦ 9♣ 8♥ = 23 — quắc! Bạn 15 điểm, không quắc nên thắng Minh. " +
                "(Nếu cả con và cái cùng quắc thì mặc định Hòa — xem Luật nhà.)",
            rows = listOf(
                minhRow(listOf(c(Rank.SIX, Suit.DIAMOND), c(Rank.NINE, Suit.CLUB), c(Rank.EIGHT, Suit.HEART)), "23 · Quắc"),
                TutorialRow("👑 Bạn (cái)", listOf(c(Rank.NINE, Suit.SPADE), c(Rank.SIX, Suit.HEART)), "+1 cược", isYou = true, highlight = true),
            ),
        ),
        TutorialStep(
            coach = "Bài mạnh nhất: Xì bàng (2 lá A) ăn 3 cược, rồi Xì dách (A + 10/J/Q/K) ăn 2 cược — có ngay khi chia là thắng luôn. " +
                "Bạn đã nắm Xì dách! Bấm Hoàn thành để ra chọn bàn.",
            rows = listOf(
                TutorialRow("Xì bàng", listOf(c(Rank.ACE, Suit.SPADE), c(Rank.ACE, Suit.DIAMOND)), "×3"),
                TutorialRow("Xì dách", listOf(c(Rank.ACE, Suit.HEART), c(Rank.KING, Suit.CLUB)), "×2"),
            ),
            banner = "XÌ BÀNG!",
        ),
    )
}

/** Ván tập Xì dách: rút/dằn, A đổi giá trị, nặn bài, Ngũ linh, cái xét bài. */
@Composable
fun XiDachTutorialScreen(onNavigateBack: () -> Unit) {
    ScriptedTutorial(title = "Ván tập Xì Dách", steps = XiDachTutorial.steps, onBack = onNavigateBack, felt = FeltColor.BLUE)
}
