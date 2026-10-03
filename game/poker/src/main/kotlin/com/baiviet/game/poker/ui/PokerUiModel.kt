package com.baiviet.game.poker.ui

import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.poker.engine.PokerAction
import com.baiviet.game.poker.engine.PokerView
import com.baiviet.game.poker.rules.PokerRules

data class PokerUiState(
    val tableConfig: TableConfig<PokerRules>,
    val view: PokerView? = null,
    val humanId: PlayerId = PlayerId(0),
    val legalActions: List<PokerAction> = emptyList(),
    val isHumanTurn: Boolean = false,
    val betSliderValue: Long = 0L,
    val showResultDialog: Boolean = false,
    val showExitDialog: Boolean = false,
    val showRebuyDialog: Boolean = false,
    val practiceMode: Boolean = false,
    val practiceEquity: Float = 0f,
    val settlement: Settlement? = null,
    val balances: Map<PlayerId, Long> = emptyMap(),
    val names: Map<PlayerId, String> = emptyMap(),
    /** Thời điểm bắt đầu lượt hiện tại (thanh đếm giờ). */
    val turnStartedAt: Long = 0L,
    val notes: List<String> = emptyList(),
    val canPlayAgain: Boolean = true,
    val loading: Boolean = true,
)

sealed interface PokerIntent {
    data object Fold : PokerIntent
    data object Check : PokerIntent
    data object Call : PokerIntent
    data class Bet(val amount: Long) : PokerIntent
    data class Raise(val totalBet: Long) : PokerIntent
    data object AllIn : PokerIntent
    data class SetBetSlider(val value: Long) : PokerIntent
    data object TogglePracticeMode : PokerIntent
    data object RequestRebuy : PokerIntent
    data object ConfirmRebuy : PokerIntent
    data object DismissRebuy : PokerIntent
    data object NextHand : PokerIntent
    data object RequestExit : PokerIntent
    data object ConfirmExit : PokerIntent
    data object DismissExit : PokerIntent
}

sealed interface PokerEffect {
    data class ShowToast(val message: String) : PokerEffect
    data class PlaySound(val soundName: String) : PokerEffect
    data object NavigateBack : PokerEffect
}
