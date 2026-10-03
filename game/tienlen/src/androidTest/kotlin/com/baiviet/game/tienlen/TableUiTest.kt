package com.baiviet.game.tienlen

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.game.tienlen.ui.ResultPanel
import com.baiviet.game.tienlen.ui.ResultRow
import com.baiviet.game.tienlen.ui.ResultUi
import com.baiviet.game.tienlen.ui.TienLenTable
import com.baiviet.game.tienlen.ui.TlIntent
import com.baiviet.game.tienlen.ui.TlUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Nút hành động chỉ sáng khi hợp lệ. */
@RunWith(AndroidJUnit4::class)
class TableUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val hand = listOf(Card(Rank.THREE, Suit.SPADE), Card(Rank.NINE, Suit.HEART))

    @Test
    fun playAndPassDisabledWhenNotMyTurn() {
        rule.setContent {
            TienLenTable(TlUiState(loading = false, playerCount = 2, hand = hand, isMyTurn = false), {}, {})
        }
        rule.onNodeWithText("Đánh").assertIsNotEnabled()
        rule.onNodeWithText("Bỏ lượt").assertIsNotEnabled()
    }

    @Test
    fun playEnabledOnlyWhenSelectionValid() {
        val intents = mutableListOf<TlIntent>()
        rule.setContent {
            TienLenTable(
                TlUiState(loading = false, playerCount = 2, hand = hand, isMyTurn = true, canPlay = true, canPass = false),
                { intents += it },
                {},
            )
        }
        rule.onNodeWithText("Đánh").assertIsEnabled().performClick()
        rule.onNodeWithText("Bỏ lượt").assertIsNotEnabled()
        assertTrue(TlIntent.Play in intents)
    }

    @Test
    fun resultPanelNewGameDisabledWhenBroke() {
        rule.setContent {
            ResultPanel(
                ResultUi(listOf(ResultRow(0, "Bạn", "Bét", -500, true, emptyList())), capped = false, canPlayAgain = false),
                {}, {}, {},
            )
        }
        rule.onNodeWithText("Ván mới").assertIsNotEnabled()
        rule.onNodeWithText("Đổi bàn").assertIsEnabled()
    }
}
