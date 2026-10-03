package com.baiviet.game.maubinh.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.maubinh.rules.MauBinhArrangement
import com.baiviet.game.maubinh.rules.MauBinhInstantWinType
import com.baiviet.game.maubinh.rules.MauBinhPairwiseComparison
import com.baiviet.game.maubinh.rules.MauBinhRules
import kotlinx.serialization.Serializable

/**
 * Giai đoạn của ván Mậu Binh.
 */
@Serializable
enum class MauBinhPhase {
    /** Giai đoạn xếp bài: mọi người cùng xếp trong tối đa 60 giây. */
    ARRANGING,

    /** Giai đoạn lật so bài theo từng chi (Chi 1 -> Chi 2 -> Chi 3). */
    REVEALING,

    /** Ván bài đã kết thúc và tính điểm xong. */
    FINISHED,
}

/**
 * Bước lật bài trong giai đoạn [MauBinhPhase.REVEALING].
 */
@Serializable
enum class MauBinhRevealStep {
    NOT_STARTED,
    CHI_1,
    CHI_2,
    CHI_3,
    SUMMARY,
}

/**
 * Trạng thái của một người chơi tại bàn.
 */
@Serializable
data class MauBinhPlayerState(
    val playerId: PlayerId,
    val initialCards: List<Card>,
    val instantWinDeclared: MauBinhInstantWinType? = null,
    val arrangement: MauBinhArrangement? = null,
    val isSubmitted: Boolean = false,
)

/**
 * Toàn bộ trạng thái ván bài Mậu Binh (server/engine view).
 */
@Serializable
data class MauBinhState(
    val tableConfig: TableConfig<MauBinhRules>,
    val seed: Long,
    val phase: MauBinhPhase,
    val revealStep: MauBinhRevealStep = MauBinhRevealStep.NOT_STARTED,
    val players: List<MauBinhPlayerState>,
    val pairwiseComparisons: List<MauBinhPairwiseComparison> = emptyList(),
    val finalSettlement: Settlement? = null,
)

/**
 * Các hành động có thể thực hiện trong Mậu Binh.
 */
@Serializable
sealed interface MauBinhAction {
    /** Báo Mậu binh tới trắng (nếu tay bài có tới trắng). */
    @Serializable
    data class DeclareInstantWin(val type: MauBinhInstantWinType) : MauBinhAction

    /** Nộp phương án xếp 3 chi (bấm nút "Xếp xong"). */
    @Serializable
    data class SubmitArrangement(val arrangement: MauBinhArrangement) : MauBinhAction

    /** Yêu cầu máy tự xếp bài theo phương án tốt nhất ("Xếp gợi ý" hoặc tự động). */
    @Serializable
    data object AutoArrange : MauBinhAction

    /** Hết 60 giây: hệ thống tự động chốt bài và nộp bài. */
    @Serializable
    data object Timeout : MauBinhAction

    /** Chuyển sang bước lật chi tiếp theo (hoặc kết thúc). */
    @Serializable
    data object NextRevealStep : MauBinhAction
}

/**
 * Góc nhìn của một người chơi khác (ẩn bài úp khi chưa lật).
 */
@Serializable
data class MauBinhOtherPlayerView(
    val playerId: PlayerId,
    val cardCount: Int,
    val isSubmitted: Boolean,
    val instantWinDeclared: MauBinhInstantWinType? = null,
    val visibleChi1: List<Card>? = null,
    val visibleChi2: List<Card>? = null,
    val visibleChi3: List<Card>? = null,
)

/**
 * Góc nhìn của một người chơi cụ thể (Client View).
 * Tuyệt đối không để lộ bài của đối thủ khi chưa lật!
 */
@Serializable
data class MauBinhView(
    val myPlayerId: PlayerId,
    val phase: MauBinhPhase,
    val revealStep: MauBinhRevealStep,
    val myCards: List<Card>,
    val myArrangement: MauBinhArrangement?,
    val myInstantWin: MauBinhInstantWinType?,
    val isMySubmitted: Boolean,
    val otherPlayers: List<MauBinhOtherPlayerView>,
    val pairwiseComparisons: List<MauBinhPairwiseComparison>,
    val settlement: Settlement?,
    val rules: MauBinhRules,
)
