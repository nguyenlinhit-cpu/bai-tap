package com.baiviet.game.poker.engine

import kotlinx.serialization.Serializable

@Serializable
sealed interface PokerAction {
    @Serializable
    data object Fold : PokerAction

    @Serializable
    data object Check : PokerAction

    @Serializable
    data object Call : PokerAction

    @Serializable
    data class Bet(val amount: Long) : PokerAction

    @Serializable
    data class Raise(val totalBet: Long) : PokerAction

    @Serializable
    data object AllIn : PokerAction
}
