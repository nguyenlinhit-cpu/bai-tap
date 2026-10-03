package com.baiviet.game.bacay.ui

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.bacay.engine.BaCayAction
import com.baiviet.game.bacay.engine.BaCayView
import com.baiviet.game.bacay.rules.BaCayRules

/** Vai trò nhà cái chọn ở màn Chọn bàn (thứ tự khớp tham số `option`). */
enum class BaCayDealerMode {
    PLAYER_DEALER,
    BOT_DEALER,
    ROTATING,
}

data class BaCayUiState(
    val tableConfig: TableConfig<BaCayRules>,
    val view: BaCayView? = null,
    val humanId: PlayerId = PlayerId(0),
    val dealerMode: BaCayDealerMode = BaCayDealerMode.BOT_DEALER,
    val legalActions: List<BaCayAction> = emptyList(),
    val isHumanTurn: Boolean = false,
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

sealed interface BaCayIntent {
    data object Reveal : BaCayIntent
    data object RequestSqueeze : BaCayIntent
    data object DismissSqueeze : BaCayIntent
    data object NextRound : BaCayIntent
    data object RequestExit : BaCayIntent
    data object ConfirmExit : BaCayIntent
    data object DismissExit : BaCayIntent
}

sealed interface BaCayEffect {
    data class ShowToast(val message: String) : BaCayEffect
    data object NavigateBack : BaCayEffect
}
