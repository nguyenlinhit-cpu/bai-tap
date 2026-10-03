package com.baiviet.game.xidach.ui

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.xidach.engine.XiDachAction
import com.baiviet.game.xidach.engine.XiDachView
import com.baiviet.game.xidach.rules.XiDachRules

/** Vai trò nhà cái chọn ở màn Chọn bàn (thứ tự khớp tham số `option`). */
enum class XiDachDealerMode {
    PLAYER_DEALER,
    BOT_DEALER,
    ROTATING,
}

data class XiDachUiState(
    val tableConfig: TableConfig<XiDachRules>,
    val view: XiDachView? = null,
    val humanId: PlayerId = PlayerId(0),
    val dealerMode: XiDachDealerMode = XiDachDealerMode.BOT_DEALER,
    val legalActions: List<XiDachAction> = emptyList(),
    val isHumanTurn: Boolean = false,
    /** Thời điểm bắt đầu lượt hiện tại (vòng đếm giờ). */
    val turnStartedAt: Long = 0L,
    val showResultDialog: Boolean = false,
    val showExitDialog: Boolean = false,
    val showSqueezeDialog: Boolean = false,
    val squeezingCard: Card? = null,
    val settlement: Settlement? = null,
    val balances: Map<PlayerId, Long> = emptyMap(),
    val names: Map<PlayerId, String> = emptyMap(),
    val notes: List<String> = emptyList(),
    val canPlayAgain: Boolean = true,
    val loading: Boolean = true,
)

sealed interface XiDachIntent {
    data object Hit : XiDachIntent
    data object Stand : XiDachIntent
    data class Inspect(val target: PlayerId) : XiDachIntent
    data object InspectAll : XiDachIntent
    data object RequestSqueeze : XiDachIntent
    data object DismissSqueeze : XiDachIntent
    data object NextRound : XiDachIntent
    data object RequestExit : XiDachIntent
    data object ConfirmExit : XiDachIntent
    data object DismissExit : XiDachIntent
}

sealed interface XiDachEffect {
    data class ShowToast(val message: String) : XiDachEffect
    data object NavigateBack : XiDachEffect
}
