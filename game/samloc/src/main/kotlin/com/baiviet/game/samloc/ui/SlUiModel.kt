package com.baiviet.game.samloc.ui

import androidx.compose.runtime.Immutable
import com.baiviet.core.cards.Card
import com.baiviet.core.ui.card.CardStyle
import com.baiviet.core.ui.effects.BannerData
import com.baiviet.core.ui.effects.CoinFlight
import com.baiviet.core.ui.table.CenterPileState
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.table.SeatInfo
import com.baiviet.game.samloc.engine.SlPhase
import com.baiviet.game.samloc.rules.SamLocRules

/** Cách xếp bài trên tay. */
enum class SlSortMode { BY_RANK, BY_GROUP }

@Immutable
data class SlSeatUi(
    val seat: Int,
    val info: SeatInfo,
    /** Bài lật ngửa khi kết thúc ván (bot). */
    val revealed: List<Card> = emptyList(),
)

@Immutable
data class SlResultRow(
    val seat: Int,
    val name: String,
    val rankLabel: String,
    val delta: Long,
    val isHuman: Boolean,
    val reasons: List<String>,
)

@Immutable
data class SlResultUi(
    val rows: List<SlResultRow>,
    val capped: Boolean,
    val canPlayAgain: Boolean,
    val note: String? = null,
)

/** Trạng thái màn bàn chơi Sâm Lốc (MVI: UiState). */
@Immutable
data class SlUiState(
    val loading: Boolean = true,
    val playerCount: Int = 4,
    val betUnit: Long = 100L,
    val rules: SamLocRules = SamLocRules.DEFAULT,
    val phase: SlPhase = SlPhase.BAO_SAM,
    val samCaller: Int? = null,
    val seats: List<SlSeatUi> = emptyList(),
    val turnSeat: Int? = null,
    val turnStartedAt: Long = 0L,
    val turnMillis: Long = 20_000L,
    val hand: List<Card> = emptyList(),
    val selected: Set<Card> = emptySet(),
    val pile: CenterPileState = CenterPileState(0, emptyList()),
    val isMyTurn: Boolean = false,
    val canPlay: Boolean = false,
    val canPass: Boolean = false,
    val canCallSam: Boolean = false,
    val canSkipSam: Boolean = false,
    val hintCards: Set<Card> = emptySet(),
    val dimCards: Set<Card> = emptySet(),
    val banner: BannerData? = null,
    val flights: List<CoinFlight> = emptyList(),
    val result: SlResultUi? = null,
    val message: String? = null,
    val log: List<String> = emptyList(),
    val sortMode: SlSortMode = SlSortMode.BY_RANK,
    val cardStyle: CardStyle = CardStyle(),
    val felt: FeltColor = FeltColor.GREEN,
    val showGuide: Boolean = false,
    val showExitConfirm: Boolean = false,
    val showBao1Warning: Boolean = false,
    val pendingBao1Cards: List<Card>? = null,
    val exitPenalty: Long = 0L,
    val dealing: Boolean = false,
    val soundOn: Boolean = true,
)

/** Ý định người chơi (MVI: Intent). */
sealed interface SlIntent {
    data class Toggle(
        val card: Card,
    ) : SlIntent

    data class Sweep(
        val cards: List<Card>,
    ) : SlIntent

    data object Play : SlIntent

    data object Pass : SlIntent

    data object CallSam : SlIntent

    data object SkipSam : SlIntent

    data object ConfirmBao1Play : SlIntent

    data object CancelBao1Play : SlIntent

    data object Hint : SlIntent

    data object Sort : SlIntent

    data object NewGame : SlIntent

    data object RequestExit : SlIntent

    data object ConfirmExit : SlIntent

    data object CancelExit : SlIntent

    data object DismissGuide : SlIntent

    data object ToggleSound : SlIntent
}

/** Hiệu ứng một lần (MVI: Effect). */
sealed interface SlEffect {
    data class Haptic(
        val strong: Boolean,
    ) : SlEffect

    data object Exit : SlEffect
}

/**
 * Vị trí ghế trên màn ngang (tọa độ tương đối 0..1).
 * Ghế 0 (bạn) ở dưới; chiều đi ngược chiều kim đồng hồ → phải → trên → trái.
 */
object SlSeatLayout {
    private val CENTER =
        androidx.compose.ui.geometry
            .Offset(0.5f, 0.42f)

    fun anchors(playerCount: Int): List<androidx.compose.ui.geometry.Offset> =
        when (playerCount) {
            2 -> {
                listOf(
                    androidx.compose.ui.geometry
                        .Offset(0.5f, 0.86f),
                    androidx.compose.ui.geometry
                        .Offset(0.5f, 0.15f),
                )
            }

            3 -> {
                listOf(
                    androidx.compose.ui.geometry
                        .Offset(0.5f, 0.86f),
                    androidx.compose.ui.geometry
                        .Offset(0.84f, 0.2f),
                    androidx.compose.ui.geometry
                        .Offset(0.16f, 0.2f),
                )
            }

            else -> {
                listOf(
                    androidx.compose.ui.geometry
                        .Offset(0.5f, 0.86f),
                    androidx.compose.ui.geometry
                        .Offset(0.92f, 0.44f),
                    androidx.compose.ui.geometry
                        .Offset(0.5f, 0.15f),
                    androidx.compose.ui.geometry
                        .Offset(0.08f, 0.44f),
                )
            }
        }

    fun direction(
        playerCount: Int,
        seat: Int,
    ): androidx.compose.ui.geometry.Offset {
        val a = anchors(playerCount)[seat]
        val d = a - CENTER
        val len = kotlin.math.sqrt(d.x * d.x + d.y * d.y).coerceAtLeast(0.001f)
        return androidx.compose.ui.geometry
            .Offset(d.x / len, d.y / len)
    }
}

val SL_SEAT_COLORS =
    listOf(
        androidx.compose.ui.graphics
            .Color(0xFFF5C451),
        androidx.compose.ui.graphics
            .Color(0xFF38BDF8),
        androidx.compose.ui.graphics
            .Color(0xFFF472B6),
        androidx.compose.ui.graphics
            .Color(0xFFA78BFA),
    )
