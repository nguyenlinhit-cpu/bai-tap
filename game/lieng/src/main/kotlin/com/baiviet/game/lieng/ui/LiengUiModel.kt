package com.baiviet.game.lieng.ui

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.lieng.engine.LiengAction
import com.baiviet.game.lieng.engine.LiengView
import com.baiviet.game.lieng.rules.LiengRules

data class LiengUiState(
    val tableConfig: TableConfig<LiengRules>,
    val view: LiengView? = null,
    val humanId: PlayerId = PlayerId(0),
    val legalActions: List<LiengAction> = emptyList(),
    val isHumanTurn: Boolean = false,
    val showResultDialog: Boolean = false,
    val showExitDialog: Boolean = false,
    val showSqueezeDialog: Boolean = false,
    val squeezingCard: Card? = null,
    val settlement: Settlement? = null,
    val balances: Map<PlayerId, Long> = emptyMap(),
    val names: Map<PlayerId, String> = emptyMap(),
    val turnStartedAt: Long = 0L,
    val notes: List<String> = emptyList(),
    val canPlayAgain: Boolean = true,
    val loading: Boolean = true,
)

sealed interface LiengIntent {
    data object Fold : LiengIntent
    data object Check : LiengIntent
    data object Call : LiengIntent
    data class Raise(val amount: Long) : LiengIntent
    data object AllIn : LiengIntent
    data object RequestSqueeze : LiengIntent
    data object DismissSqueeze : LiengIntent
    data object NextRound : LiengIntent
    data object RequestExit : LiengIntent
    data object ConfirmExit : LiengIntent
    data object DismissExit : LiengIntent
}

sealed interface LiengEffect {
    data class ShowToast(val message: String) : LiengEffect
    data class PlaySound(val soundName: String) : LiengEffect
    data object NavigateBack : LiengEffect
}
