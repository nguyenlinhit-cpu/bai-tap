package com.baiviet.game.bacay.engine

import com.baiviet.core.cards.Deck
import com.baiviet.core.engine.GameEngine
import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.TableConfig
import com.baiviet.core.engine.Transition
import com.baiviet.game.bacay.rules.BaCayEvaluator
import com.baiviet.game.bacay.rules.BaCayRules
import com.baiviet.game.bacay.rules.BaCayScoring

class BaCayEngine : GameEngine<BaCayState, BaCayAction, BaCayView, BaCayRules> {

    override fun start(table: TableConfig<BaCayRules>, seed: Long): BaCayState = startRound(table, seed, dealerSeat = 0, bets = null)

    /**
     * Bắt đầu ván với nhà cái và mức cược cho trước.
     *
     * @param dealerSeat ghế làm cái
     * @param bets mức cược từng ghế (null = mỗi người 1B)
     */
    fun startRound(table: TableConfig<BaCayRules>, seed: Long, dealerSeat: Int, bets: List<Long>?): BaCayState {
        val players = List(table.playerCount) { PlayerId(it) }
        val dealerId = players[dealerSeat]

        val (_, shuffledDeck) = Deck.shuffled(seed)
        val (dealtHands, _) = Deck.deal(
            deck = shuffledDeck,
            playerCount = table.playerCount,
            cardsPerPlayer = 3,
        )

        val playerStates = players.indices.associate { index ->
            val pid = players[index]
            pid to BaCayPlayerState(
                id = pid,
                cards = dealtHands[index],
                betAmount = bets?.get(index) ?: table.betUnit,
                isRevealed = false,
            )
        }

        return BaCayState(
            players = players,
            dealerId = dealerId,
            currentActor = null,
            phase = BaCayPhase.SQUEEZING,
            playerStates = playerStates,
            rules = table.rules,
        )
    }

    override fun currentActors(state: BaCayState): Set<PlayerId> {
        if (state.phase == BaCayPhase.FINISHED) return emptySet()
        return state.players.filter { state.playerStates[it]?.isRevealed == false }.toSet()
    }

    override fun legalActions(state: BaCayState, player: PlayerId): List<BaCayAction> {
        if (state.phase == BaCayPhase.FINISHED) return emptyList()
        val pState = state.playerStates[player] ?: return emptyList()
        if (pState.isRevealed) return emptyList()

        return listOf(BaCayAction.Reveal, BaCayAction.RevealAll)
    }

    override fun apply(state: BaCayState, player: PlayerId, action: BaCayAction): Transition<BaCayState> {
        val legal = legalActions(state, player)
        if (action !in legal) {
            throw IllegalActionException("Hành động $action không hợp lệ cho người chơi $player")
        }

        val updatedStates = state.playerStates.toMutableMap()

        when (action) {
            BaCayAction.Reveal -> {
                updatedStates[player] = updatedStates[player]!!.copy(isRevealed = true)
                val allRevealed = state.players.all { updatedStates[it]!!.isRevealed }

                return if (allRevealed) {
                    val handsMap = updatedStates.mapValues { (_, s) ->
                        BaCayEvaluator.evaluate(s.cards, state.rules.sapBeatsBaTien)
                    }
                    val betsMap = updatedStates.mapValues { (_, s) -> s.betAmount }
                    val (_, results) = BaCayScoring.computeSettlement(
                        state.dealerId,
                        state.players,
                        betsMap,
                        handsMap,
                        state.rules,
                    )
                    Transition(
                        state.copy(
                            phase = BaCayPhase.FINISHED,
                            playerStates = updatedStates,
                            results = results,
                        ),
                        emptyList(),
                    )
                } else {
                    Transition(state.copy(playerStates = updatedStates), emptyList())
                }
            }
            BaCayAction.RevealAll -> {
                for (pid in state.players) {
                    updatedStates[pid] = updatedStates[pid]!!.copy(isRevealed = true)
                }
                val handsMap = updatedStates.mapValues { (_, s) ->
                    BaCayEvaluator.evaluate(s.cards, state.rules.sapBeatsBaTien)
                }
                val betsMap = updatedStates.mapValues { (_, s) -> s.betAmount }
                val (_, results) = BaCayScoring.computeSettlement(
                    state.dealerId,
                    state.players,
                    betsMap,
                    handsMap,
                    state.rules,
                )
                return Transition(
                    state.copy(
                        phase = BaCayPhase.FINISHED,
                        playerStates = updatedStates,
                        results = results,
                    ),
                    emptyList(),
                )
            }
        }
    }

    override fun isFinished(state: BaCayState): Boolean = state.phase == BaCayPhase.FINISHED

    override fun settle(state: BaCayState): Settlement {
        check(isFinished(state)) { "Chỉ được settle khi ván đã kết thúc" }
        val handsMap = state.playerStates.mapValues { (_, s) ->
            BaCayEvaluator.evaluate(s.cards, state.rules.sapBeatsBaTien)
        }
        val betsMap = state.playerStates.mapValues { (_, s) -> s.betAmount }
        val (settlement, _) = BaCayScoring.computeSettlement(
            dealerId = state.dealerId,
            players = state.players,
            bets = betsMap,
            hands = handsMap,
            rules = state.rules,
        )
        return settlement
    }

    override fun viewOf(state: BaCayState, player: PlayerId): BaCayView {
        val myState = state.playerStates[player] ?: error("Người chơi $player không có trong bàn")
        val isFinished = state.phase == BaCayPhase.FINISHED
        val myHand = BaCayEvaluator.evaluate(myState.cards, state.rules.sapBeatsBaTien)

        val playerInfos = state.players.map { pid ->
            val pState = state.playerStates[pid]!!
            val canSee = isFinished || pid == player || pState.isRevealed
            BaCayPlayerInfo(
                id = pid,
                cardCount = pState.cards.size,
                cards = if (canSee) pState.cards else null,
                hand = if (canSee) BaCayEvaluator.evaluate(pState.cards, state.rules.sapBeatsBaTien) else null,
                betAmount = pState.betAmount,
                isRevealed = pState.isRevealed,
            )
        }

        return BaCayView(
            myId = player,
            dealerId = state.dealerId,
            phase = state.phase,
            myHand = myHand,
            players = playerInfos,
            rules = state.rules,
            results = state.results,
        )
    }
}
