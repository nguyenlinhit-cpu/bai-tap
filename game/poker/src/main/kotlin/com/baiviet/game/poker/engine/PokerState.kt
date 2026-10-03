package com.baiviet.game.poker.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.game.poker.rules.PokerHand
import com.baiviet.game.poker.rules.PokerPlayerResult
import com.baiviet.game.poker.rules.PokerRules
import kotlinx.serialization.Serializable

@Serializable
enum class PokerStreet {
    PREFLOP,
    FLOP,
    TURN,
    RIVER,
    SHOWDOWN,
    FINISHED,
}

@Serializable
data class PokerPlayerState(
    val id: PlayerId,
    val holeCards: List<Card>,
    val stack: Long,
    val investedThisHand: Long = 0L,
    val currentStreetBet: Long = 0L,
    val isFolded: Boolean = false,
    val isAllIn: Boolean = false,
    val hasMucked: Boolean = false,
)

@Serializable
data class SidePotInfo(
    val amount: Long,
    val eligiblePlayers: List<PlayerId>,
)

@Serializable
data class PokerState(
    val tableId: String = "poker_table",
    val players: List<PlayerId>,
    val dealerIndex: Int = 0,
    val street: PokerStreet = PokerStreet.PREFLOP,
    val deckCards: List<Card> = emptyList(),
    val communityCards: List<Card> = emptyList(),
    val burnedCards: List<Card> = emptyList(),
    val playerStates: Map<PlayerId, PokerPlayerState>,
    val currentActor: PlayerId? = null,
    val currentStreetHighBet: Long = 0L,
    val minRaise: Long = 0L,
    val lastBettor: PlayerId? = null,
    val actedThisStreet: Set<PlayerId> = emptySet(),
    /** Người đã hành động và chỉ đối mặt với all-in thiếu mức raise → chỉ được Theo/Úp. */
    val raiseLocked: Set<PlayerId> = emptySet(),
    val bigBlind: Long = 100L,
    val smallBlind: Long = 50L,
    val rules: PokerRules = PokerRules(),
    val results: List<PokerPlayerResult> = emptyList(),
) {
    val pot: Long get() = playerStates.values.sumOf { it.investedThisHand }

    fun activePlayers(): List<PlayerId> =
        players.filter { playerStates[it]?.isFolded == false }

    fun eligibleToAct(): List<PlayerId> =
        players.filter {
            val s = playerStates[it] ?: return@filter false
            !s.isFolded && !s.isAllIn
        }
}

@Serializable
data class PokerPlayerInfo(
    val id: PlayerId,
    val stack: Long,
    val investedThisHand: Long,
    val currentStreetBet: Long,
    val isFolded: Boolean,
    val isAllIn: Boolean,
    val holeCards: List<Card>? = null, // null nếu đang che bài
    val hand: PokerHand? = null,
    val isDealer: Boolean = false,
    val isSB: Boolean = false,
    val isBB: Boolean = false,
)

@Serializable
data class PokerView(
    val myId: PlayerId,
    val street: PokerStreet,
    val communityCards: List<Card>,
    val myHoleCards: List<Card>,
    val myHand: PokerHand?,
    val currentActor: PlayerId?,
    val dealerIndex: Int,
    val pot: Long,
    val currentStreetHighBet: Long,
    val toCall: Long,
    val minRaiseTotal: Long,
    val maxRaiseTotal: Long,
    val players: List<PokerPlayerInfo>,
    val results: List<PokerPlayerResult>,
    val rules: PokerRules,
)
