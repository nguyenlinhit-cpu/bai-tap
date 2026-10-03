package com.baiviet.game.maubinh.ui

import androidx.compose.runtime.Composable
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.tutorial.ScriptedTutorial
import com.baiviet.core.ui.tutorial.TutorialRow
import com.baiviet.core.ui.tutorial.TutorialStep

private fun c(r: Rank, s: Suit) = Card(r, s)

/** Bài sắp sẵn của ván tập Mậu binh (13 lá). */
internal object MauBinhTutorial {
    val flush = listOf(c(Rank.TWO, Suit.HEART), c(Rank.FIVE, Suit.HEART), c(Rank.EIGHT, Suit.HEART), c(Rank.JACK, Suit.HEART), c(Rank.KING, Suit.HEART))
    val straight = listOf(c(Rank.FIVE, Suit.SPADE), c(Rank.SIX, Suit.DIAMOND), c(Rank.SEVEN, Suit.CLUB), c(Rank.EIGHT, Suit.SPADE), c(Rank.NINE, Suit.DIAMOND))
    val pair = listOf(c(Rank.QUEEN, Suit.CLUB), c(Rank.QUEEN, Suit.DIAMOND), c(Rank.THREE, Suit.SPADE))
    val all: List<Card> = (flush + straight + pair).shuffled(kotlin.random.Random(7))

    val minhChi1 = listOf(c(Rank.ACE, Suit.CLUB), c(Rank.ACE, Suit.DIAMOND), c(Rank.FOUR, Suit.CLUB), c(Rank.SIX, Suit.SPADE), c(Rank.TEN, Suit.CLUB))
    val minhChi2 = listOf(c(Rank.TEN, Suit.SPADE), c(Rank.TEN, Suit.HEART), c(Rank.TWO, Suit.CLUB), c(Rank.FOUR, Suit.DIAMOND), c(Rank.SEVEN, Suit.HEART))
    val minhChi3 = listOf(c(Rank.KING, Suit.DIAMOND), c(Rank.NINE, Suit.SPADE), c(Rank.SIX, Suit.CLUB))

    val steps = listOf(
        TutorialStep(
            coach = "Bạn nhận 13 lá và có 60 giây để xếp thành 3 chi: Chi 1 (5 lá) ≥ Chi 2 (5 lá) ≥ Chi 3 (3 lá). " +
                "Không so chất. Nhìn bài: có 5 lá Cơ (Thùng), dãy 5-6-7-8-9 (Sảnh) và đôi Q. Bấm Xếp bài.",
            rows = listOf(TutorialRow("Bài của bạn", all, "13 lá", isYou = true, highlight = true)),
            actions = listOf("Nộp ngay", "Xếp bài"),
            correct = 1,
            wrongHint = "Chưa xếp mà nộp thì máy sẽ tự xếp — hãy tự xếp để học nhé.",
        ),
        TutorialStep(
            coach = "Đây là cách xếp SAI: Chi 1 là Sảnh, Chi 2 là Thùng. Thùng lớn hơn Sảnh nên chi dưới lớn hơn chi trên → BINH LỦNG, " +
                "thua 6 chi mỗi người. Bấm Đổi chi 1 ↔ chi 2 để sửa.",
            rows = listOf(
                TutorialRow("Chi 1", straight, "Sảnh"),
                TutorialRow("Chi 2", flush, "Thùng ⚠ lớn hơn chi 1"),
                TutorialRow("Chi 3", pair, "Đôi Q"),
            ),
            actions = listOf("Nộp bài", "Đổi chi 1 ↔ chi 2"),
            correct = 1,
            wrongHint = "Nộp bây giờ là binh lủng — phải đổi chi trước.",
        ),
        TutorialStep(
            coach = "Đúng rồi: Thùng ≥ Sảnh ≥ Đôi Q. Bấm Nộp bài.",
            rows = listOf(
                TutorialRow("Chi 1", flush, "Thùng", isYou = true, highlight = true),
                TutorialRow("Chi 2", straight, "Sảnh", isYou = true),
                TutorialRow("Chi 3", pair, "Đôi Q", isYou = true),
            ),
            actions = listOf("Nộp bài"),
            correct = 0,
        ),
        TutorialStep(
            coach = "So chi với Minh: Thùng thắng Đôi A, Sảnh thắng Đôi 10, Đôi Q thắng Mậu thầu K. " +
                "Bạn thắng cả 3 chi → SẬP 3 CHI, tiền với Minh nhân đôi: 3 chi × 2 = 6 chi!",
            rows = listOf(
                TutorialRow("Minh · Chi 1", minhChi1, "Đôi A"),
                TutorialRow("Minh · Chi 2", minhChi2, "Đôi 10"),
                TutorialRow("Minh · Chi 3", minhChi3, "Mậu thầu"),
                TutorialRow("Bạn", flush + straight + pair, "+6 chi", isYou = true, highlight = true),
            ),
            banner = "SẬP 3 CHI!",
        ),
        TutorialStep(
            coach = "Chi thưởng: Sám ở chi 3, Cù lũ ở chi 2, Tứ quý, Thùng phá sảnh được cộng thêm chi. " +
                "Mậu binh tới trắng (Rồng cuốn, Sảnh rồng, 6 đôi…) thắng ngay không cần so. Bấm Hoàn thành để vào chơi.",
            rows = listOf(TutorialRow("Sám chi 3 (+3 chi)", listOf(c(Rank.ACE, Suit.SPADE), c(Rank.ACE, Suit.HEART), c(Rank.ACE, Suit.CLUB)))),
        ),
    )
}

/** Ván tập Mậu binh: xếp 3 chi đúng, ví dụ binh lủng, sập 3 chi. */
@Composable
fun MauBinhTutorialScreen(onBack: () -> Unit) {
    ScriptedTutorial(title = "Ván tập Mậu Binh", steps = MauBinhTutorial.steps, onBack = onBack, felt = FeltColor.GREEN)
}
