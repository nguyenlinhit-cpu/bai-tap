package com.baiviet.game.phom.ui

import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.Color
import com.baiviet.core.cards.Card
import com.baiviet.core.ui.card.CardStyle
import com.baiviet.core.ui.effects.BannerData
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.table.SeatInfo
import com.baiviet.game.phom.engine.PhomPhase
import com.baiviet.game.phom.rules.PhomMeld
import com.baiviet.game.phom.rules.PhomPlayerResult

data class PhomSeatUi(
    val seat: Int,
    val info: SeatInfo,
)

data class PhomResultUi(
    val title: String,
    val isWin: Boolean,
    val deltaCoins: Long,
    val names: List<String> = emptyList(),
    val deltas: List<Long> = emptyList(),
    val results: List<PhomPlayerResult>,
    val rankedSeats: List<Int>,
)

val PHOM_SEAT_COLORS = listOf(
    Color(0xFFF5C451),
    Color(0xFF38BDF8),
    Color(0xFFF472B6),
    Color(0xFFA78BFA),
)

data class PhomUiState(
    val hand: List<Card> = emptyList(),
    val selected: Set<Card> = emptySet(),
    val eatenCards: Set<Card> = emptySet(),
    val seats: List<PhomSeatUi> = emptyList(),
    val discardPiles: List<List<Card>> = emptyList(),
    val exposedMelds: List<List<PhomMeld>> = emptyList(),
    val stockCount: Int = 0,
    val lastDiscard: Card? = null,
    val lastDiscarder: Int? = null,
    val phase: PhomPhase = PhomPhase.DRAW_OR_EAT,
    val turn: Int = 0,
    val isMyTurn: Boolean = false,
    val canDraw: Boolean = false,
    val canEat: Boolean = false,
    val canDiscard: Boolean = false,
    val canMeld: Boolean = false,
    val canLayOff: Boolean = false,
    val canPassLayOff: Boolean = false,
    val turnStartedAt: Long = 0L,
    val turnMillis: Long = 25_000L,
    val message: String? = null,
    val notes: List<String> = emptyList(),
    val canPlayAgain: Boolean = true,
    val canU: Boolean = false,
    val canUKhan: Boolean = false,
    val banner: BannerData? = null,
    val result: PhomResultUi? = null,
    val felt: FeltColor = FeltColor.GREEN,
    val cardStyle: CardStyle = CardStyle(),
    val playerCount: Int = 4,
    val betUnit: Long = 100L,
    val exitConfirmOpen: Boolean = false,
    val reviewOpen: Boolean = false,
    val quickGuideOpen: Boolean = false,
)

sealed interface PhomIntent {
    data class Toggle(val card: Card) : PhomIntent
    data object Draw : PhomIntent
    data object Eat : PhomIntent
    data object Discard : PhomIntent
    data object Meld : PhomIntent
    data object LayOff : PhomIntent
    data object PassLayOff : PhomIntent
    data object DeclareU : PhomIntent
    data object DeclareUKhan : PhomIntent
    data object ExitClicked : PhomIntent
    data object ConfirmExit : PhomIntent
    data object DismissExit : PhomIntent
    data object OpenReview : PhomIntent
    data object DismissReview : PhomIntent
    data object OpenQuickGuide : PhomIntent
    data object DismissQuickGuide : PhomIntent
    data object PlayAgain : PhomIntent
}

sealed interface PhomEffect {
    data class Haptic(val strong: Boolean) : PhomEffect
    data object NavigateBack : PhomEffect
}

object PhomSeatLayout {
    fun anchors(playerCount: Int): List<Alignment> = when (playerCount) {
        2 -> listOf(
            BiasAlignment(-1f, 1f), // Bạn (góc dưới trái, cạnh tay bài)
            BiasAlignment(0f, -0.88f), // Đối thủ (trên)
        )
        3 -> listOf(
            BiasAlignment(-1f, 1f),   // Bạn (góc dưới trái, cạnh tay bài)
            BiasAlignment(-0.85f, -0.3f), // Người bên trái
            BiasAlignment(0.85f, -0.3f),  // Người bên phải
        )
        else -> listOf(
            BiasAlignment(-1f, 1f),    // Bạn (góc dưới trái, cạnh tay bài)
            BiasAlignment(-0.88f, 0f),   // Trái
            BiasAlignment(0f, -0.88f),   // Đối diện (trên)
            BiasAlignment(0.88f, 0f),    // Phải
        )
    }
}
