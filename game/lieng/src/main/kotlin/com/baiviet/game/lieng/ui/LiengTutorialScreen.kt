package com.baiviet.game.lieng.ui

import androidx.compose.runtime.Composable
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.tutorial.ScriptedTutorial
import com.baiviet.core.ui.tutorial.TutorialRow
import com.baiviet.core.ui.tutorial.TutorialStep

private fun c(r: Rank, s: Suit) = Card(r, s)

/** Bài sắp sẵn của ván tập Liêng. */
internal object LiengTutorial {
    val round1Me = listOf(c(Rank.TWO, Suit.SPADE), c(Rank.SEVEN, Suit.CLUB), c(Rank.NINE, Suit.DIAMOND))
    val round1Minh = listOf(c(Rank.FOUR, Suit.SPADE), c(Rank.FOUR, Suit.HEART), c(Rank.FOUR, Suit.CLUB))
    val round2Me = listOf(c(Rank.FIVE, Suit.DIAMOND), c(Rank.SIX, Suit.CLUB), c(Rank.SEVEN, Suit.SPADE))
    val round2Minh = listOf(c(Rank.JACK, Suit.SPADE), c(Rank.JACK, Suit.DIAMOND), c(Rank.QUEEN, Suit.CLUB))
    val jqk = listOf(c(Rank.JACK, Suit.CLUB), c(Rank.QUEEN, Suit.HEART), c(Rank.KING, Suit.SPADE))

    val steps = listOf(
        TutorialStep(
            coach = "Mỗi người đặt cược sàn rồi nhận 3 lá. Thứ hạng: Sáp (3 lá cùng số) > Liêng (3 lá liên tiếp) > Ảnh (3 lá J/Q/K) > Điểm. " +
                "Bài bạn 2♠ 7♣ 9♦ = 18 → 8 điểm. Minh vừa Tố rất mạnh 10 cược. Ván tập này hãy Úp để giữ chip.",
            rows = listOf(
                TutorialRow("Minh", listOf(null, null, null), "Tố 10"),
                TutorialRow("Hùng", listOf(null, null, null), "Theo 10"),
                TutorialRow("Bạn", round1Me, "8 điểm", isYou = true, highlight = true),
                TutorialRow("Pot", emptyList(), "23 chip"),
            ),
            actions = listOf("Úp", "Theo", "Tố"),
            correct = 0,
            wrongHint = "8 điểm chỉ là bài Điểm, đối thủ tố lớn — Úp là khôn ngoan.",
        ),
        TutorialStep(
            coach = "Úp đúng lúc! Minh lật Sáp 4 — thắng mọi bài Liêng, Ảnh, Điểm. Bạn chỉ mất cược sàn.",
            rows = listOf(
                TutorialRow("Minh", round1Minh, "Sáp 4 · ăn pot", highlight = true),
                TutorialRow("Bạn", round1Me, "Đã úp", isYou = true),
            ),
            banner = "SÁP!",
        ),
        TutorialStep(
            coach = "Ván mới: bạn có 5♦ 6♣ 7♠ — Liêng! Chỉ thua Sáp. Minh tố 2 cược. Hãy Tố lên để ăn nhiều hơn.",
            rows = listOf(
                TutorialRow("Minh", listOf(null, null, null), "Tố 2"),
                TutorialRow("Hùng", listOf(null, null, null), "Úp"),
                TutorialRow("Bạn", round2Me, "Liêng 7", isYou = true, highlight = true),
            ),
            actions = listOf("Úp", "Theo", "Tố"),
            correct = 2,
            wrongHint = "Bài Liêng rất mạnh — đừng chỉ Theo, hãy Tố!",
        ),
        TutorialStep(
            coach = "Minh Theo. Lật bài: Minh có J♠ J♦ Q♣ — Ảnh (3 lá hình nhưng không liên tiếp). Liêng > Ảnh, bạn ăn pot!",
            rows = listOf(
                TutorialRow("Minh", round2Minh, "Ảnh"),
                TutorialRow("Bạn", round2Me, "Liêng · ăn pot", isYou = true, highlight = true),
            ),
            banner = "LIÊNG!",
        ),
        TutorialStep(
            coach = "Lưu ý: J-Q-K là Liêng chứ không phải Ảnh. A-2-3 và Q-K-A cũng là Liêng. Bằng nhau thì so điểm/lá cao, " +
                "rồi so chất Rô > Cơ > Chuồn > Bích. Bấm Hoàn thành để vào chơi.",
            rows = listOf(TutorialRow("Liêng J-Q-K", jqk, "Liêng, không phải Ảnh", highlight = true)),
        ),
    )
}

/** Ván tập Liêng: tố/theo/úp và so Sáp – Liêng – Ảnh – Điểm. */
@Composable
fun LiengTutorialScreen(onNavigateBack: () -> Unit) {
    ScriptedTutorial(title = "Ván tập Liêng", steps = LiengTutorial.steps, onBack = onNavigateBack, felt = FeltColor.RED)
}
