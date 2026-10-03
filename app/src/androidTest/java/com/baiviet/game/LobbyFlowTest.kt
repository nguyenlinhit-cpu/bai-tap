package com.baiviet.game

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Luồng Sảnh → Chọn bàn → Bàn chơi. */
@RunWith(AndroidJUnit4::class)
class LobbyFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun lobbyToTable() {
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Tiến Lên Miền Nam").fetchSemanticsNodes().isNotEmpty() }
        // Đóng hộp thoại chào mừng nếu có
        if (rule.onAllNodesWithText("Xác nhận").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Xác nhận").performClick()
        }
        rule.onNodeWithText("Tiến Lên Miền Nam").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Vào bàn").fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        // Hướng dẫn nhanh tự hiện lần đầu vào game
        if (rule.onAllNodesWithText("Bỏ qua").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Bỏ qua").performClick()
        }
        rule.onNodeWithText("Vào bàn").performScrollTo().performClick()
        rule.waitUntil(15_000) { rule.onAllNodesWithText("Đánh").fetchSemanticsNodes().isNotEmpty() }
        // Bỏ qua hướng dẫn nhanh lần đầu
        if (rule.onAllNodesWithText("Bỏ qua").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Bỏ qua").performClick()
        }
        // Chưa chọn lá nào → nút Đánh tắt
        rule.onNodeWithText("Đánh").assertIsNotEnabled()
    }
}
