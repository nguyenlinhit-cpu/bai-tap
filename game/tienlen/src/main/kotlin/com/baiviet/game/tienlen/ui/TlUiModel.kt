package com.baiviet.game.tienlen.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.baiviet.core.cards.Card
import com.baiviet.core.ui.card.CardStyle
import com.baiviet.core.ui.effects.BannerData
import com.baiviet.core.ui.effects.CoinFlight
import com.baiviet.core.ui.table.CenterPileState
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.table.SeatInfo
import com.baiviet.game.tienlen.rules.TienLenRules
import kotlin.math.sqrt

/** Cách xếp bài trên tay. */
enum class SortMode { BY_RANK, BY_GROUP }

@Immutable
data class SeatUi(
    val seat: Int,
    val info: SeatInfo,
    /** Bài lật ngửa khi kết thúc ván (bot). */
    val revealed: List<Card> = emptyList(),
)

@Immutable
data class ResultRow(
    val seat: Int,
    val name: String,
    val rankLabel: String,
    val delta: Long,
    val isHuman: Boolean,
    val reasons: List<String>,
)

@Immutable
data class ResultUi(
    val rows: List<ResultRow>,
    val capped: Boolean,
    val canPlayAgain: Boolean,
    val note: String? = null,
)

/** Trạng thái màn bàn chơi Tiến lên (MVI: UiState). */
@Immutable
data class TlUiState(
    val loading: Boolean = true,
    val playerCount: Int = 4,
    val betUnit: Long = 100L,
    val rules: TienLenRules = TienLenRules.DEFAULT,
    val seats: List<SeatUi> = emptyList(),
    val turnSeat: Int? = null,
    val turnStartedAt: Long = 0L,
    val turnMillis: Long = 20_000L,
    val hand: List<Card> = emptyList(),
    val selected: Set<Card> = emptySet(),
    val pile: CenterPileState = CenterPileState(0, emptyList()),
    val isMyTurn: Boolean = false,
    val canPlay: Boolean = false,
    val canPass: Boolean = false,
    val hintCards: Set<Card> = emptySet(),
    val dimCards: Set<Card> = emptySet(),
    val banner: BannerData? = null,
    val flights: List<CoinFlight> = emptyList(),
    val result: ResultUi? = null,
    val message: String? = null,
    val log: List<String> = emptyList(),
    val sortMode: SortMode = SortMode.BY_RANK,
    val cardStyle: CardStyle = CardStyle(),
    val felt: FeltColor = FeltColor.GREEN,
    val showGuide: Boolean = false,
    val showExitConfirm: Boolean = false,
    val exitPenalty: Long = 0L,
    val dealing: Boolean = false,
    val soundOn: Boolean = true,
)

/** Ý định người chơi (MVI: Intent). */
sealed interface TlIntent {
    data class Toggle(val card: Card) : TlIntent
    data class Sweep(val cards: List<Card>) : TlIntent
    data object Play : TlIntent
    data object Pass : TlIntent
    data object Hint : TlIntent
    data object Sort : TlIntent
    data object NewGame : TlIntent
    data object RequestExit : TlIntent
    data object ConfirmExit : TlIntent
    data object CancelExit : TlIntent
    data object DismissGuide : TlIntent
    data object ToggleSound : TlIntent
}

/** Hiệu ứng một lần (MVI: Effect). */
sealed interface TlEffect {
    data class Haptic(val strong: Boolean) : TlEffect
    data object Exit : TlEffect
}

/**
 * Vị trí ghế trên màn ngang (tọa độ tương đối 0..1).
 * Ghế 0 (bạn) ở dưới; chiều đi ngược chiều kim đồng hồ → phải → trên → trái.
 */
object SeatLayout {
    private val CENTER = Offset(0.5f, 0.42f)

    fun anchors(playerCount: Int): List<Offset> = when (playerCount) {
        2 -> listOf(Offset(0.5f, 0.86f), Offset(0.5f, 0.15f))
        3 -> listOf(Offset(0.5f, 0.86f), Offset(0.84f, 0.2f), Offset(0.16f, 0.2f))
        else -> listOf(Offset(0.5f, 0.86f), Offset(0.92f, 0.44f), Offset(0.5f, 0.15f), Offset(0.08f, 0.44f))
    }

    /** Hướng từ tâm bàn tới ghế (vector đơn vị) — để bài bay từ ghế vào giữa. */
    fun direction(playerCount: Int, seat: Int): Offset {
        val a = anchors(playerCount)[seat]
        val d = a - CENTER
        val len = sqrt(d.x * d.x + d.y * d.y).coerceAtLeast(0.001f)
        return Offset(d.x / len, d.y / len)
    }
}

/** Màu avatar từng ghế. */
val SEAT_COLORS = listOf(
    Color(0xFFF5C451),
    Color(0xFF38BDF8),
    Color(0xFFF472B6),
    Color(0xFFA78BFA),
)
