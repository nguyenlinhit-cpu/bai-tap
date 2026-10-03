package com.baiviet.game.poker.ui

import androidx.compose.runtime.Composable
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.tutorial.ScriptedTutorial
import com.baiviet.core.ui.tutorial.TutorialRow
import com.baiviet.core.ui.tutorial.TutorialStep

private fun c(r: Rank, s: Suit) = Card(r, s)

/** Bài sắp sẵn của ván tập Poker (bạn ngồi nút, Minh mù nhỏ 50, Hùng mù lớn 100). */
internal object PokerTutorial {
    val me = listOf(c(Rank.ACE, Suit.SPADE), c(Rank.KING, Suit.SPADE))
    val minh = listOf(c(Rank.QUEEN, Suit.SPADE), c(Rank.JACK, Suit.SPADE))
    val hung = listOf(c(Rank.SEVEN, Suit.CLUB), c(Rank.SEVEN, Suit.DIAMOND))
    val flop = listOf(c(Rank.KING, Suit.HEART), c(Rank.SEVEN, Suit.SPADE), c(Rank.TWO, Suit.SPADE))
    val turn = c(Rank.NINE, Suit.SPADE)
    val river = c(Rank.THREE, Suit.DIAMOND)

    private fun board(cards: List<Card>, pot: String) = TutorialRow("Bài chung", cards + List(5 - cards.size) { null }, "Pot $pot")

    val steps = listOf(
        TutorialStep(
            coach = "Trước khi chia, Minh đặt mù nhỏ 50 và Hùng mù lớn 100. Bạn có A♠ K♠ — bài tẩy rất mạnh. " +
                "Vòng Pre-flop: hãy Tố lên 300.",
            rows = listOf(
                TutorialRow("Minh (SB)", listOf(null, null), "50"),
                TutorialRow("Hùng (BB)", listOf(null, null), "100"),
                board(emptyList(), "150"),
                TutorialRow("Bạn (D)", me, "Stack 3.000", isYou = true, highlight = true),
            ),
            actions = listOf("Úp", "Theo 100", "Tố 300"),
            correct = 2,
            wrongHint = "A-K cùng chất là bài mạnh — Tố để lấy thế chủ động.",
        ),
        TutorialStep(
            coach = "Cả hai theo 300. Flop mở K♥ 7♠ 2♠: bạn có Đôi K kicker A, lại có 4 lá bích chờ Thùng. Hùng cược 200, Minh theo. Hãy Theo.",
            rows = listOf(
                TutorialRow("Minh", listOf(null, null), "Theo 200"),
                TutorialRow("Hùng", listOf(null, null), "Cược 200"),
                board(flop, "900"),
                TutorialRow("Bạn", me, "Đôi K", isYou = true, highlight = true),
            ),
            actions = listOf("Úp", "Theo 200", "Tố 800"),
            correct = 1,
            wrongHint = "Bài tốt và còn chờ Thùng — Theo để xem lá tiếp theo với giá rẻ.",
        ),
        TutorialStep(
            coach = "Turn 9♠ — bạn có Thùng! Hùng chỉ còn 500 nên Tất tay 500; Minh Tất tay 1.500. " +
                "Bạn còn 2.500 — hãy Theo 1.500. Pot chính (3 người × 500) và pot phụ (bạn với Minh) sẽ được tách riêng.",
            rows = listOf(
                TutorialRow("Minh", listOf(null, null), "Tất tay 1.500"),
                TutorialRow("Hùng", listOf(null, null), "Tất tay 500"),
                board(flop + turn, "1.500"),
                TutorialRow("Bạn", me, "Thùng A", isYou = true, highlight = true),
            ),
            actions = listOf("Úp", "Theo 1.500"),
            correct = 1,
            wrongHint = "Thùng A gần như chắc thắng — đừng Úp!",
        ),
        TutorialStep(
            coach = "Chia pot: Pot chính = 1.500 + 3 × 500 = 3.000 (cả ba tranh). Pot phụ = 2 × 1.000 = 2.000 (chỉ bạn và Minh). " +
                "Hùng tất tay ít nhất nên chỉ được tranh pot chính.",
            rows = listOf(
                TutorialRow("Pot chính", emptyList(), "3.000 · 3 người"),
                TutorialRow("Pot phụ", emptyList(), "2.000 · Bạn & Minh"),
                board(flop + turn + river, "5.000"),
            ),
        ),
        TutorialStep(
            coach = "Lật bài: Hùng Sám 7, Minh Thùng Q, bạn Thùng A. Hai Thùng so lá cao nhất: A > Q → bạn ăn cả pot chính lẫn pot phụ 5.000!",
            rows = listOf(
                TutorialRow("Minh", minh, "Thùng Q"),
                TutorialRow("Hùng", hung, "Sám 7"),
                board(flop + turn + river, "5.000"),
                TutorialRow("Bạn", me, "Thùng A · +5.000", isYou = true, highlight = true),
            ),
            banner = "THÙNG!",
        ),
        TutorialStep(
            coach = "Thứ hạng: Thùng phá sảnh > Tứ quý > Cù lũ > Thùng > Sảnh > Sám > Thú > Đôi > Mậu thầu. " +
                "Sảnh A-2-3-4-5 là sảnh nhỏ nhất. Bấm Hoàn thành để vào chơi.",
            rows = listOf(
                TutorialRow(
                    "Thùng phá sảnh",
                    listOf(c(Rank.TEN, Suit.HEART), c(Rank.JACK, Suit.HEART), c(Rank.QUEEN, Suit.HEART), c(Rank.KING, Suit.HEART), c(Rank.ACE, Suit.HEART)),
                    "Mạnh nhất",
                ),
            ),
        ),
    )
}

/** Ván tập Poker: blind, 4 vòng cược, all-in và side pot. */
@Composable
fun PokerTutorialScreen(onNavigateBack: () -> Unit) {
    ScriptedTutorial(title = "Ván tập Poker", steps = PokerTutorial.steps, onBack = onNavigateBack, felt = FeltColor.BLUE)
}
