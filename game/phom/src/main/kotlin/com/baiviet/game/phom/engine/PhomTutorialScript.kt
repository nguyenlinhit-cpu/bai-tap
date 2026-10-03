package com.baiviet.game.phom.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.game.phom.rules.PhomMeld

sealed interface PhomTutorialStep {
    data class Human(
        val index: Int,
        val prompt: String,
        val checkAction: (PhomAction) -> Boolean,
    ) : PhomTutorialStep

    data class Bot(val action: PhomAction) : PhomTutorialStep
}

/**
 * Kịch bản ván tập tương tác hướng dẫn chơi Phỏm (Tá Lả).
 */
object PhomTutorialScript {

    private fun c(r: Rank, s: Suit) = Card(r, s)

    val YOU: List<Card> = listOf(
        c(Rank.SEVEN, Suit.SPADE), c(Rank.SEVEN, Suit.HEART), // cạ ngang 7s-7h
        c(Rank.NINE, Suit.DIAMOND), c(Rank.TEN, Suit.DIAMOND), c(Rank.JACK, Suit.DIAMOND), // phỏm dọc 9-10-J rô
        c(Rank.FOUR, Suit.CLUB), c(Rank.FIVE, Suit.CLUB), // cạ dọc 4-5 chuồn
        c(Rank.KING, Suit.SPADE), // rác to
        c(Rank.ACE, Suit.HEART),  // rác nhỏ
    )

    val BOT: List<Card> = listOf(
        c(Rank.SEVEN, Suit.DIAMOND), // bot sẽ đánh lá này cho bạn ăn
        c(Rank.THREE, Suit.HEART), c(Rank.THREE, Suit.DIAMOND), c(Rank.THREE, Suit.CLUB),
        c(Rank.EIGHT, Suit.HEART), c(Rank.NINE, Suit.HEART), c(Rank.TEN, Suit.HEART),
        c(Rank.TWO, Suit.SPADE), c(Rank.THREE, Suit.SPADE), c(Rank.FOUR, Suit.SPADE),
    )

    val STOCK: List<Card> = listOf(
        c(Rank.SIX, Suit.CLUB), // lá bốc ở lượt 2 (ghép với 4-5 chuồn thành phỏm 4-5-6!)
        c(Rank.EIGHT, Suit.DIAMOND),
        c(Rank.QUEEN, Suit.HEART),
        c(Rank.KING, Suit.HEART),
        c(Rank.ACE, Suit.SPADE),
    )

    val STEPS: List<PhomTutorialStep> = listOf(
        // Bước 0: Bot đi đầu đánh 7 rô
        PhomTutorialStep.Bot(PhomAction.Discard(c(Rank.SEVEN, Suit.DIAMOND))),

        // Bước 1: Người chơi Ăn lá 7 rô
        PhomTutorialStep.Human(
            index = 1,
            prompt = "Đối thủ vừa đánh 7♦. Bài bạn có 7♠ và 7♥! Hãy bấm \"Ăn bài\" để tạo phỏm 7-7-7.",
            checkAction = { it is PhomAction.Eat },
        ),

        // Bước 2: Người chơi đánh lá K bích (rác điểm cao)
        PhomTutorialStep.Human(
            index = 2,
            prompt = "Sau khi ăn bài, hãy chọn lá K♠ (rác 13 điểm lớn nhất không có cạ) và bấm \"Đánh\".",
            checkAction = { it is PhomAction.Discard && it.card == c(Rank.KING, Suit.SPADE) },
        ),

        // Bước 3: Bot bốc bài và đánh rác vô hại 2 bích
        PhomTutorialStep.Bot(PhomAction.Draw),
        PhomTutorialStep.Bot(PhomAction.Discard(c(Rank.TWO, Suit.SPADE))),

        // Bước 4: Người chơi Bốc bài từ Nọc
        PhomTutorialStep.Human(
            index = 3,
            prompt = "Lá 2♠ của đối thủ không ghép được phỏm. Hãy bấm \"Bốc bài\" từ Nọc!",
            checkAction = { it is PhomAction.Draw },
        ),

        // Bước 5: Người chơi đánh lá A cơ
        PhomTutorialStep.Human(
            index = 4,
            prompt = "Bạn vừa bốc được 6♣ ghép thành phỏm 4-5-6♣! Hãy chọn lá A♥ và đánh đi.",
            checkAction = { it is PhomAction.Discard && it.card == c(Rank.ACE, Suit.HEART) },
        ),
    )
}
