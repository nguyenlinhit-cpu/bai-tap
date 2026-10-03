package com.baiviet.game.samloc.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit

/** Một bước trong ván tập Sâm Lốc. */
sealed interface SlTutorialStep {
    /** Lời nhắc cho người chơi. [expectedAction] là hành động bắt buộc phải thực hiện. */
    data class Human(
        val index: Int,
        val prompt: String,
        val expectedAction: SlAction,
    ) : SlTutorialStep

    /** Máy đánh nước định sẵn. */
    data class Bot(
        val action: SlAction,
    ) : SlTutorialStep
}

/**
 * Ván tập Sâm Lốc (2 người, bài sắp sẵn):
 * 1. Pha Báo Sâm: Bấm Báo Sâm với bài có sảnh dài và tứ quý.
 * 2. Đánh Sảnh 5→10.
 * 3. Máy không chặn được (bỏ qua).
 * 4. Đánh Tứ quý 9.
 * 5. Máy không chặn được. Bạn còn 1 lá (tự động Báo 1).
 * 6. Đánh lá cuối cùng (Át) để về và Ăn Sâm thành công!
 */
object SamLocTutorialScript {
    private fun c(
        r: Rank,
        s: Suit,
    ) = Card(r, s)

    val YOU: List<Card> =
        listOf(
            c(Rank.FIVE, Suit.SPADE),
            c(Rank.SIX, Suit.CLUB),
            c(Rank.SEVEN, Suit.DIAMOND),
            c(Rank.EIGHT, Suit.HEART),
            c(Rank.NINE, Suit.SPADE),
            c(Rank.NINE, Suit.CLUB),
            c(Rank.NINE, Suit.DIAMOND),
            c(Rank.NINE, Suit.HEART),
            c(Rank.TEN, Suit.SPADE),
            c(Rank.ACE, Suit.HEART),
        )

    val BOT: List<Card> =
        listOf(
            c(Rank.THREE, Suit.SPADE),
            c(Rank.THREE, Suit.CLUB),
            c(Rank.FOUR, Suit.HEART),
            c(Rank.FOUR, Suit.DIAMOND),
            c(Rank.JACK, Suit.SPADE),
            c(Rank.JACK, Suit.CLUB),
            c(Rank.QUEEN, Suit.HEART),
            c(Rank.KING, Suit.DIAMOND),
            c(Rank.TWO, Suit.SPADE),
            c(Rank.TWO, Suit.HEART),
        )

    val STEPS: List<SlTutorialStep> =
        listOf(
            SlTutorialStep.Human(
                1,
                "Bài của bạn có sảnh dài 5→10 và tứ quý 9 rất mạnh! Hãy chọn 'Báo Sâm'.",
                SlAction.CallSam,
            ),
            SlTutorialStep.Human(
                2,
                "Sau khi Báo Sâm, bạn có quyền đi trước. Hãy chọn và đánh sảnh 5-6-7-8-9♠-10.",
                SlAction.Play(
                    listOf(
                        c(Rank.FIVE, Suit.SPADE),
                        c(Rank.SIX, Suit.CLUB),
                        c(Rank.SEVEN, Suit.DIAMOND),
                        c(Rank.EIGHT, Suit.HEART),
                        c(Rank.NINE, Suit.SPADE),
                        c(Rank.TEN, Suit.SPADE),
                    ),
                ),
            ),
            SlTutorialStep.Bot(SlAction.Pass),
            SlTutorialStep.Human(
                3,
                "Đối thủ không chặn được! Hãy tiếp tục đánh Tứ quý 9 (3 lá 9 còn lại... chờ, tứ quý là 4 lá, ở đây bạn đánh 3 lá 9 sám hoặc tứ quý). Hãy đánh Sám 9.",
                SlAction.Play(
                    listOf(
                        c(Rank.NINE, Suit.CLUB),
                        c(Rank.NINE, Suit.DIAMOND),
                        c(Rank.NINE, Suit.HEART),
                    ),
                ),
            ),
            SlTutorialStep.Bot(SlAction.Pass),
            SlTutorialStep.Human(
                4,
                "Bạn chỉ còn 1 lá bài Át (hệ thống tự động Báo 1). Hãy đánh lá Át để về Nhất và hoàn tất Ăn Sâm!",
                SlAction.Play(listOf(c(Rank.ACE, Suit.HEART))),
            ),
        )
}
