package com.baiviet.game.bacay.ui

import androidx.compose.runtime.Composable
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.core.ui.tutorial.ScriptedTutorial
import com.baiviet.core.ui.tutorial.TutorialRow
import com.baiviet.core.ui.tutorial.TutorialStep

private fun c(r: Rank, s: Suit) = Card(r, s)

/** Bài sắp sẵn của ván tập Ba cây. */
internal object BaCayTutorial {
    private val mine = listOf(c(Rank.FOUR, Suit.HEART), c(Rank.TWO, Suit.CLUB), c(Rank.THREE, Suit.DIAMOND))
    private val dealer = listOf(c(Rank.SEVEN, Suit.SPADE), c(Rank.KING, Suit.HEART), c(Rank.TWO, Suit.SPADE))

    val steps = listOf(
        TutorialStep(
            coach = "Mỗi người 3 lá. Điểm = tổng các lá rồi lấy hàng đơn vị (A = 1, 10/J/Q/K = 0). " +
                "Hai lá đầu của bạn 4♥ + 2♣ = 6. Lá thứ ba đang úp — bấm Nặn bài rồi kéo góc để hé dần.",
            rows = listOf(
                TutorialRow("👑 Nhà cái (Máy)", listOf(null, null, null)),
                TutorialRow("Bạn", listOf(mine[0], mine[1], null), "6 + ?", isYou = true, highlight = true),
            ),
            actions = listOf("Mở bài", "Nặn bài"),
            correct = 1,
            squeeze = mine[2],
            wrongHint = "Thử Nặn bài để cảm giác hồi hộp như bàn thật nhé!",
        ),
        TutorialStep(
            coach = "Lá 3♦! 4 + 2 + 3 = 9 — 9 nút, điểm cao nhất của bài thường. Bấm Mở bài để so với cái.",
            rows = listOf(
                TutorialRow("👑 Nhà cái (Máy)", listOf(null, null, null)),
                TutorialRow("Bạn", mine, "9 nút", isYou = true, highlight = true),
            ),
            actions = listOf("Mở bài"),
            correct = 0,
            banner = "9 NÚT!",
        ),
        TutorialStep(
            coach = "Cái lật 7♠ K♥ 2♠ = 19 → cũng 9 nút! Bằng nút thì so lá mạnh nhất theo chất Rô > Cơ > Chuồn > Bích. " +
                "Lá mạnh nhất của bạn là 3♦ (Rô), của cái là K♥ (Cơ) → Rô thắng Cơ, bạn ăn.",
            rows = listOf(
                TutorialRow("👑 Nhà cái (Máy)", dealer, "9 nút · K♥"),
                TutorialRow("Bạn", mine, "9 nút · 3♦ → Thắng", isYou = true, highlight = true),
            ),
        ),
        TutorialStep(
            coach = "Bài đặc biệt: Ba tiên (3 lá J/Q/K) lớn hơn mọi 9 nút. Sáp (3 lá cùng số) xếp dưới Ba tiên — có thể đổi trong Luật nhà. " +
                "Bấm Hoàn thành để vào chơi thật.",
            rows = listOf(
                TutorialRow("Ba tiên", listOf(c(Rank.JACK, Suit.SPADE), c(Rank.QUEEN, Suit.HEART), c(Rank.KING, Suit.CLUB)), "Cao nhất"),
                TutorialRow("Sáp", listOf(c(Rank.FIVE, Suit.SPADE), c(Rank.FIVE, Suit.HEART), c(Rank.FIVE, Suit.DIAMOND))),
            ),
            banner = "BA TIÊN!",
        ),
    )
}

/** Ván tập Ba cây: tính điểm, nặn bài, so chất khi bằng nút, Ba tiên. */
@Composable
fun BaCayTutorialScreen(onNavigateBack: () -> Unit) {
    ScriptedTutorial(title = "Ván tập Ba Cây", steps = BaCayTutorial.steps, onBack = onNavigateBack)
}
