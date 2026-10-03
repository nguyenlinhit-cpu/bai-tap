package com.baiviet.game.maubinh.ui

import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.Color
import com.baiviet.core.cards.Card
import com.baiviet.core.engine.Settlement
import com.baiviet.core.ui.card.CardStyle
import com.baiviet.core.ui.effects.BannerData
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.table.SeatInfo
import com.baiviet.game.maubinh.engine.MauBinhOtherPlayerView
import com.baiviet.game.maubinh.engine.MauBinhPhase
import com.baiviet.game.maubinh.engine.MauBinhRevealStep
import com.baiviet.game.maubinh.rules.MauBinhArrangement
import com.baiviet.game.maubinh.rules.MauBinhChiHand
import com.baiviet.game.maubinh.rules.MauBinhInstantWinType
import com.baiviet.game.maubinh.rules.MauBinhPairwiseComparison
import com.baiviet.game.maubinh.rules.MauBinhRules

data class MauBinhSeatUi(
    val seat: Int,
    val info: SeatInfo,
    val isSubmitted: Boolean = false,
    val instantWin: MauBinhInstantWinType? = null,
    val visibleChi1: List<Card>? = null,
    val visibleChi2: List<Card>? = null,
    val visibleChi3: List<Card>? = null,
    val chi1Hand: MauBinhChiHand? = null,
    val chi2Hand: MauBinhChiHand? = null,
    val chi3Hand: MauBinhChiHand? = null,
    val isFoul: Boolean = false,
)

data class MauBinhResultUi(
    val title: String,
    val isWin: Boolean,
    val totalChi: Int,
    val deltaCoins: Long,
    val pairwiseComparisons: List<MauBinhPairwiseComparison>,
    val settlement: Settlement?,
)

val MAUBINH_SEAT_COLORS = listOf(
    Color(0xFFF5C451),
    Color(0xFF38BDF8),
    Color(0xFFF472B6),
    Color(0xFFA78BFA),
)

data class MauBinhUiState(
    val resultNotes: List<String> = emptyList(),
    val initialCards: List<Card> = emptyList(),
    val unassignedCards: List<Card> = emptyList(),
    val chi1Cards: List<Card> = emptyList(),
    val chi2Cards: List<Card> = emptyList(),
    val chi3Cards: List<Card> = emptyList(),
    val selectedCard: Card? = null,

    // Đánh giá trực tiếp phương án xếp bài hiện tại
    val chi1Hand: MauBinhChiHand? = null,
    val chi2Hand: MauBinhChiHand? = null,
    val chi3Hand: MauBinhChiHand? = null,
    val isFull13: Boolean = false,
    val isFoul: Boolean = false,
    val validityMessage: String = "",

    // Tới trắng
    val detectedInstantWin: MauBinhInstantWinType? = null,
    val instantWinDialogOpen: Boolean = false,
    val isInstantWinDeclared: Boolean = false,

    // Trạng thái bàn chơi
    val phase: MauBinhPhase = MauBinhPhase.ARRANGING,
    val revealStep: MauBinhRevealStep = MauBinhRevealStep.NOT_STARTED,
    val timeLeftSeconds: Int = 60,
    val isSubmitted: Boolean = false,
    val seats: List<MauBinhSeatUi> = emptyList(),

    // Kết quả
    val pairwiseComparisons: List<MauBinhPairwiseComparison> = emptyList(),
    val result: MauBinhResultUi? = null,
    val banner: BannerData? = null,

    // Cấu hình
    val felt: FeltColor = FeltColor.GREEN,
    val cardStyle: CardStyle = CardStyle(),
    val playerCount: Int = 4,
    val betUnit: Long = 100L,
    val rules: MauBinhRules = MauBinhRules.DEFAULT,

    // Dialogs
    val exitConfirmOpen: Boolean = false,
    val quickGuideOpen: Boolean = false,
    val pairwiseMatrixOpen: Boolean = false,
)

sealed interface MauBinhIntent {
    data class SelectCard(val card: Card) : MauBinhIntent
    data class AssignCardToChi(val card: Card, val chiIndex: Int) : MauBinhIntent
    data class SwapCards(val cardA: Card, val cardB: Card) : MauBinhIntent
    data object AutoArrange : MauBinhIntent
    data object ResetArrangement : MauBinhIntent
    data object Submit : MauBinhIntent
    data object DeclareInstantWin : MauBinhIntent
    data object DismissInstantWinDialog : MauBinhIntent
    data object NextRevealStep : MauBinhIntent
    data object OpenPairwiseMatrix : MauBinhIntent
    data object DismissPairwiseMatrix : MauBinhIntent
    data object ExitClicked : MauBinhIntent
    data object ConfirmExit : MauBinhIntent
    data object DismissExit : MauBinhIntent
    data object OpenQuickGuide : MauBinhIntent
    data object DismissQuickGuide : MauBinhIntent
    data object PlayAgain : MauBinhIntent
}

sealed interface MauBinhEffect {
    data class Haptic(val strong: Boolean) : MauBinhEffect
    data object NavigateBack : MauBinhEffect
}

object MauBinhSeatLayout {
    fun anchors(playerCount: Int): List<Alignment> = when (playerCount) {
        2 -> listOf(
            BiasAlignment(0f, 0.90f),  // Bạn (dưới)
            BiasAlignment(0f, -0.90f), // Đối thủ (trên)
        )
        3 -> listOf(
            BiasAlignment(0f, 0.90f),     // Bạn (dưới)
            BiasAlignment(-0.85f, -0.35f), // Người bên trái
            BiasAlignment(0.85f, -0.35f),  // Người bên phải
        )
        else -> listOf(
            BiasAlignment(0f, 0.90f),     // Bạn (dưới)
            BiasAlignment(-0.88f, -0.15f), // Bên trái
            BiasAlignment(0f, -0.90f),    // Đối diện (trên)
            BiasAlignment(0.88f, -0.15f),  // Bên phải
        )
    }
}
