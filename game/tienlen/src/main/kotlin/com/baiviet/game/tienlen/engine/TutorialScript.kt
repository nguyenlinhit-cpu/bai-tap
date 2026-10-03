package com.baiviet.game.tienlen.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit

/** Một bước trong ván tập. */
sealed interface TutorialStep {
    /** Người chơi phải đánh đúng [cards] (null = phải bỏ lượt). [index] = số thứ tự lời hướng dẫn (1..). */
    data class Human(val index: Int, val cards: Set<Card>?) : TutorialStep

    /** Máy đánh nước định sẵn. */
    data class Bot(val action: TlAction) : TutorialStep
}

/**
 * Ván tập Tiến lên (2 người, bài sắp sẵn): đánh rác → chặt heo bằng 3 đôi thông → đánh đôi →
 * chặn đôi → bỏ lượt → chặn rác khi máy đi tự do → về Nhất.
 */
object TutorialScript {
    private fun c(r: Rank, s: Suit) = Card(r, s)

    val YOU: List<Card> = listOf(
        c(Rank.THREE, Suit.SPADE), c(Rank.FOUR, Suit.SPADE), c(Rank.FOUR, Suit.DIAMOND),
        c(Rank.SIX, Suit.CLUB), c(Rank.SIX, Suit.DIAMOND), c(Rank.SEVEN, Suit.SPADE), c(Rank.SEVEN, Suit.HEART),
        c(Rank.EIGHT, Suit.CLUB), c(Rank.EIGHT, Suit.DIAMOND), c(Rank.JACK, Suit.DIAMOND), c(Rank.JACK, Suit.HEART),
        c(Rank.QUEEN, Suit.SPADE), c(Rank.ACE, Suit.CLUB),
    )

    val BOT: List<Card> = listOf(
        c(Rank.TWO, Suit.HEART), c(Rank.TEN, Suit.CLUB), c(Rank.TEN, Suit.DIAMOND), c(Rank.KING, Suit.HEART),
        c(Rank.KING, Suit.DIAMOND), c(Rank.FIVE, Suit.HEART), c(Rank.NINE, Suit.HEART), c(Rank.FOUR, Suit.CLUB),
        c(Rank.THREE, Suit.HEART), c(Rank.SEVEN, Suit.DIAMOND), c(Rank.SIX, Suit.HEART), c(Rank.QUEEN, Suit.DIAMOND),
        c(Rank.FIVE, Suit.CLUB),
    )

    val STEPS: List<TutorialStep> = listOf(
        TutorialStep.Human(1, setOf(c(Rank.THREE, Suit.SPADE))),
        TutorialStep.Bot(TlAction.Play(listOf(c(Rank.TWO, Suit.HEART)))),
        TutorialStep.Human(
            2,
            setOf(
                c(Rank.SIX, Suit.CLUB), c(Rank.SIX, Suit.DIAMOND), c(Rank.SEVEN, Suit.SPADE),
                c(Rank.SEVEN, Suit.HEART), c(Rank.EIGHT, Suit.CLUB), c(Rank.EIGHT, Suit.DIAMOND),
            ),
        ),
        TutorialStep.Bot(TlAction.Pass),
        TutorialStep.Human(3, setOf(c(Rank.FOUR, Suit.SPADE), c(Rank.FOUR, Suit.DIAMOND))),
        TutorialStep.Bot(TlAction.Play(listOf(c(Rank.TEN, Suit.CLUB), c(Rank.TEN, Suit.DIAMOND)))),
        TutorialStep.Human(4, setOf(c(Rank.JACK, Suit.DIAMOND), c(Rank.JACK, Suit.HEART))),
        TutorialStep.Bot(TlAction.Play(listOf(c(Rank.KING, Suit.HEART), c(Rank.KING, Suit.DIAMOND)))),
        TutorialStep.Human(5, null),
        TutorialStep.Bot(TlAction.Play(listOf(c(Rank.FIVE, Suit.HEART)))),
        TutorialStep.Human(6, setOf(c(Rank.QUEEN, Suit.SPADE))),
        TutorialStep.Bot(TlAction.Pass),
        TutorialStep.Human(7, setOf(c(Rank.ACE, Suit.CLUB))),
    )
}
