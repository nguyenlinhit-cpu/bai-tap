package com.baiviet.game.poker.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Deck
import com.baiviet.core.engine.GameEngine
import com.baiviet.core.engine.GameEvent
import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.TableConfig
import com.baiviet.core.engine.Transition
import com.baiviet.game.poker.rules.PokerEvaluator
import com.baiviet.game.poker.rules.PokerHand
import com.baiviet.game.poker.rules.PokerRules
import com.baiviet.game.poker.rules.PokerScoring
import kotlin.random.Random

class PokerEngine : GameEngine<PokerState, PokerAction, PokerView, PokerRules> {

    override fun start(table: TableConfig<PokerRules>, seed: Long): PokerState =
        startHand(table, seed, List(table.playerCount) { table.rules.startingChipsBB * table.betUnit }, dealerIndex = 0)

    /**
     * Bắt đầu một ván với stack mang từ ván trước và vị trí nút dealer (xoay vòng mỗi ván).
     * Mọi stack phải > 0 (người hết stack phải nạp lại hoặc rời bàn trước khi chia).
     */
    fun startHand(table: TableConfig<PokerRules>, seed: Long, stacks: List<Long>, dealerIndex: Int): PokerState {
        require(stacks.size == table.playerCount && stacks.all { it > 0 }) { "Stack không hợp lệ: $stacks" }
        val deck = Deck.shuffled(seed).second.toMutableList()

        val players = (0 until table.playerCount).map { PlayerId(it) }
        val bigBlind = table.betUnit
        val smallBlind = maxOf(1L, bigBlind / 2)

        // Chia mỗi người 2 lá tẩy
        val playerStates = players.associateWith { id ->
            val cards = listOf(deck.removeAt(0), deck.removeAt(0))
            PokerPlayerState(
                id = id,
                holeCards = cards,
                stack = stacks[id.seat],
                investedThisHand = 0L,
                currentStreetBet = 0L,
            )
        }.toMutableMap()

        val sbIndex = if (players.size == 2) dealerIndex else (dealerIndex + 1) % players.size
        val bbIndex = if (players.size == 2) (dealerIndex + 1) % 2 else (dealerIndex + 2) % players.size
        val firstActorIndex = if (players.size == 2) sbIndex else (dealerIndex + 3) % players.size

        // Thu Small Blind
        val sbPlayer = players[sbIndex]
        val sbState = playerStates[sbPlayer]!!
        val actualSB = minOf(sbState.stack, smallBlind)
        playerStates[sbPlayer] = sbState.copy(
            stack = sbState.stack - actualSB,
            investedThisHand = actualSB,
            currentStreetBet = actualSB,
            isAllIn = (sbState.stack - actualSB == 0L),
        )

        // Thu Big Blind
        val bbPlayer = players[bbIndex]
        val bbState = playerStates[bbPlayer]!!
        val actualBB = minOf(bbState.stack, bigBlind)
        playerStates[bbPlayer] = bbState.copy(
            stack = bbState.stack - actualBB,
            investedThisHand = actualBB,
            currentStreetBet = actualBB,
            isAllIn = (bbState.stack - actualBB == 0L),
        )

        val base = PokerState(
            players = players,
            dealerIndex = dealerIndex,
            street = PokerStreet.PREFLOP,
            deckCards = deck,
            communityCards = emptyList(),
            burnedCards = emptyList(),
            playerStates = playerStates,
            currentActor = players[firstActorIndex],
            currentStreetHighBet = actualBB,
            minRaise = bigBlind,
            lastBettor = bbPlayer,
            actedThisStreet = emptySet(),
            bigBlind = bigBlind,
            smallBlind = smallBlind,
            rules = table.rules,
        )
        // Blind làm người đi đầu hết stack → chuyển lượt; nếu không ai còn cược được thì chia nốt bài
        val first = base.playerStates.getValue(players[firstActorIndex])
        if (!first.isAllIn) return base
        val prev = players[(firstActorIndex - 1 + players.size) % players.size]
        val next = findNextActor(players, prev, base.playerStates)
        return if (next == null || base.eligibleToAct().size <= 1 && base.eligibleToAct().all {
                base.playerStates.getValue(it).currentStreetBet >= actualBB
            }
        ) {
            advanceStreet(base, allInRunout = true).state
        } else {
            base.copy(currentActor = next)
        }
    }

    override fun currentActors(state: PokerState): Set<PlayerId> =
        if (state.street == PokerStreet.FINISHED) emptySet()
        else state.currentActor?.let { setOf(it) } ?: emptySet()

    override fun legalActions(state: PokerState, player: PlayerId): List<PokerAction> {
        if (state.street == PokerStreet.FINISHED || state.currentActor != player) return emptyList()

        val pState = state.playerStates[player] ?: return emptyList()
        if (pState.isFolded || pState.isAllIn) return emptyList()

        val actions = mutableListOf<PokerAction>()
        val toCall = maxOf(0L, state.currentStreetHighBet - pState.currentStreetBet)
        val stack = pState.stack

        if (toCall == 0L) {
            // Có quyền Check
            actions.add(PokerAction.Check)
            if (stack > 0 && state.currentStreetHighBet == 0L) {
                val minBet = minOf(stack, state.bigBlind)
                actions.add(PokerAction.Bet(minBet))
                actions.add(PokerAction.AllIn)
            } else if (stack > 0) {
                // Quyền của BB ở preflop: đã có cược nhưng không ai tố thêm → được tố
                val minRaiseTotal = state.currentStreetHighBet + state.minRaise
                if (pState.currentStreetBet + stack > minRaiseTotal) actions.add(PokerAction.Raise(minRaiseTotal))
                actions.add(PokerAction.AllIn)
            }
        } else {
            // Đang bị cược/tố
            actions.add(PokerAction.Fold)
            if (stack <= toCall) {
                // Chỉ đủ All-in để call
                actions.add(PokerAction.AllIn)
            } else {
                actions.add(PokerAction.Call)
                // All-in thiếu mức raise tối thiểu không mở lại quyền raise cho người đã hành động
                if (player !in state.raiseLocked) {
                    val minRaiseTotal = state.currentStreetHighBet + state.minRaise
                    if (pState.currentStreetBet + stack > minRaiseTotal) {
                        actions.add(PokerAction.Raise(minRaiseTotal))
                    }
                    actions.add(PokerAction.AllIn)
                }
            }
        }

        return actions
    }

    override fun apply(state: PokerState, player: PlayerId, action: PokerAction): Transition<PokerState> {
        val legal = legalActions(state, player)
        val isActionValid = legal.any { it::class == action::class }
        if (!isActionValid) {
            throw IllegalActionException("Hành động $action không hợp lệ cho người chơi $player")
        }
        val me = state.playerStates.getValue(player)
        val maxTotal = me.currentStreetBet + me.stack
        when (action) {
            is PokerAction.Bet -> if (action.amount < minOf(state.bigBlind, me.stack) || action.amount > me.stack) {
                throw IllegalActionException("Cược phải từ ${state.bigBlind} đến ${me.stack}")
            }
            is PokerAction.Raise -> if (action.totalBet < state.currentStreetHighBet + state.minRaise || action.totalBet > maxTotal) {
                throw IllegalActionException("Tố phải từ ${state.currentStreetHighBet + state.minRaise} đến $maxTotal")
            }
            else -> Unit
        }
        var raiseLocked = state.raiseLocked - player

        val pState = state.playerStates[player]!!
        val updatedStates = state.playerStates.toMutableMap()
        var currentHighBet = state.currentStreetHighBet
        var minRaise = state.minRaise
        var lastBettor = state.lastBettor
        var actedThisStreet = state.actedThisStreet + player

        when (action) {
            PokerAction.Fold -> {
                updatedStates[player] = pState.copy(isFolded = true)
            }
            PokerAction.Check -> {
                // Không thay đổi chip
            }
            PokerAction.Call -> {
                val toCall = currentHighBet - pState.currentStreetBet
                val actual = minOf(pState.stack, toCall)
                updatedStates[player] = pState.copy(
                    stack = pState.stack - actual,
                    investedThisHand = pState.investedThisHand + actual,
                    currentStreetBet = pState.currentStreetBet + actual,
                    isAllIn = (pState.stack - actual == 0L),
                )
            }
            is PokerAction.Bet -> {
                val actual = minOf(pState.stack, maxOf(state.bigBlind, action.amount))
                updatedStates[player] = pState.copy(
                    stack = pState.stack - actual,
                    investedThisHand = pState.investedThisHand + actual,
                    currentStreetBet = actual,
                    isAllIn = (pState.stack - actual == 0L),
                )
                currentHighBet = actual
                minRaise = actual
                lastBettor = player
                actedThisStreet = setOf(player)
                raiseLocked = emptySet()
            }
            is PokerAction.Raise -> {
                val desiredTotal = action.totalBet
                val diff = desiredTotal - pState.currentStreetBet
                val actualDiff = minOf(pState.stack, diff)
                val newTotal = pState.currentStreetBet + actualDiff
                val raiseInc = newTotal - currentHighBet

                updatedStates[player] = pState.copy(
                    stack = pState.stack - actualDiff,
                    investedThisHand = pState.investedThisHand + actualDiff,
                    currentStreetBet = newTotal,
                    isAllIn = (pState.stack - actualDiff == 0L),
                )
                minRaise = raiseInc
                currentHighBet = newTotal
                lastBettor = player
                actedThisStreet = setOf(player)
                raiseLocked = emptySet()
            }
            PokerAction.AllIn -> {
                val actualDiff = pState.stack
                val newTotal = pState.currentStreetBet + actualDiff
                val raiseInc = newTotal - currentHighBet

                updatedStates[player] = pState.copy(
                    stack = 0L,
                    investedThisHand = pState.investedThisHand + actualDiff,
                    currentStreetBet = newTotal,
                    isAllIn = true,
                )
                if (newTotal > currentHighBet) {
                    if (raiseInc >= minRaise) {
                        // Raise đủ mức: mở lại vòng cho mọi người
                        minRaise = raiseInc
                        actedThisStreet = setOf(player)
                        raiseLocked = emptySet()
                    } else {
                        // All-in thiếu: người đã hành động chỉ được Theo hoặc Úp
                        raiseLocked = raiseLocked + (state.actedThisStreet - player)
                    }
                    currentHighBet = newTotal
                    lastBettor = player
                }
            }
        }

        // 1. Kiểm tra nếu chỉ còn 1 người duy nhất chưa Fold -> Kết thúc ván
        val activePlayers = state.players.filter { updatedStates[it]?.isFolded == false }
        if (activePlayers.size == 1) {
            val investedMap = updatedStates.mapValues { (_, s) -> s.investedThisHand }
            val foldedSet = updatedStates.filter { it.value.isFolded }.keys
            val handsMap = updatedStates.mapValues { (_, s) ->
                PokerEvaluator.evaluate(s.holeCards + state.communityCards)
            }

            val (_, results) = PokerScoring.computeSettlement(
                players = state.players,
                invested = investedMap,
                folded = foldedSet,
                hands = handsMap,
                dealerIndex = state.dealerIndex,
            )

            // Cập nhật lại stack sau khi chia pot
            val finalStates = updatedStates.mapValues { (id, s) ->
                val payout = results.firstOrNull { it.id == id }?.payout ?: 0L
                s.copy(stack = s.stack + payout)
            }

            return Transition(
                state.copy(
                    street = PokerStreet.FINISHED,
                    currentActor = null,
                    playerStates = finalStates,
                    results = results,
                ),
                emptyList(),
            )
        }

        // 2. Tìm người chơi kế tiếp
        val nextActor = findNextActor(state.players, player, updatedStates)

        // Kiểm tra vòng cược hiện tại đã xong chưa
        val eligibleToAct = activePlayers.filter { updatedStates[it]?.isAllIn == false }
        val allMatched = eligibleToAct.all { updatedStates[it]?.currentStreetBet == currentHighBet }
        val allEligibleActed = eligibleToAct.all { it in actedThisStreet }

        val streetFinished = (allMatched && allEligibleActed) || eligibleToAct.isEmpty()

        if (streetFinished) {
            return advanceStreet(
                state.copy(
                    playerStates = updatedStates,
                    currentStreetHighBet = currentHighBet,
                    minRaise = minRaise,
                ),
                eligibleToAct.size <= 1,
            )
        }

        return Transition(
            state.copy(
                currentActor = nextActor,
                currentStreetHighBet = currentHighBet,
                minRaise = minRaise,
                lastBettor = lastBettor,
                actedThisStreet = actedThisStreet,
                raiseLocked = raiseLocked,
                playerStates = updatedStates,
            ),
            emptyList(),
        )
    }

    private fun advanceStreet(state: PokerState, allInRunout: Boolean): Transition<PokerState> {
        val resetStates = state.playerStates.mapValues { (_, s) -> s.copy(currentStreetBet = 0L) }
        val remainingDeck = state.deckCards.toMutableList()
        val community = state.communityCards.toMutableList()
        val burned = state.burnedCards.toMutableList()

        if (allInRunout) {
            // Khi không còn ai có thể hành động cược nữa, chia tất cả các lá chung còn lại
            while (community.size < 5 && remainingDeck.size >= 2) {
                burned.add(remainingDeck.removeAt(0))
                if (community.isEmpty()) {
                    // Flop 3 lá
                    repeat(3) { community.add(remainingDeck.removeAt(0)) }
                } else {
                    community.add(remainingDeck.removeAt(0))
                }
            }
            return showdown(state.copy(
                deckCards = remainingDeck,
                communityCards = community,
                burnedCards = burned,
                playerStates = resetStates,
            ))
        }

        when (state.street) {
            PokerStreet.PREFLOP -> {
                // Sang Flop: Đốt 1 lá, chia 3 lá chung
                burned.add(remainingDeck.removeAt(0))
                repeat(3) { community.add(remainingDeck.removeAt(0)) }
                val firstActor = findFirstActorPostflop(state.players, state.dealerIndex, resetStates)

                return Transition(
                    state.copy(
                        street = PokerStreet.FLOP,
                        deckCards = remainingDeck,
                        communityCards = community,
                        burnedCards = burned,
                        currentActor = firstActor,
                        currentStreetHighBet = 0L,
                        minRaise = state.bigBlind,
                        lastBettor = null,
                        actedThisStreet = emptySet(),
                        raiseLocked = emptySet(),
                        playerStates = resetStates,
                    ),
                    emptyList(),
                )
            }
            PokerStreet.FLOP -> {
                // Sang Turn: Đốt 1 lá, chia 1 lá chung
                burned.add(remainingDeck.removeAt(0))
                community.add(remainingDeck.removeAt(0))
                val firstActor = findFirstActorPostflop(state.players, state.dealerIndex, resetStates)

                return Transition(
                    state.copy(
                        street = PokerStreet.TURN,
                        deckCards = remainingDeck,
                        communityCards = community,
                        burnedCards = burned,
                        currentActor = firstActor,
                        currentStreetHighBet = 0L,
                        minRaise = state.bigBlind,
                        lastBettor = null,
                        actedThisStreet = emptySet(),
                        raiseLocked = emptySet(),
                        playerStates = resetStates,
                    ),
                    emptyList(),
                )
            }
            PokerStreet.TURN -> {
                // Sang River: Đốt 1 lá, chia 1 lá chung
                burned.add(remainingDeck.removeAt(0))
                community.add(remainingDeck.removeAt(0))
                val firstActor = findFirstActorPostflop(state.players, state.dealerIndex, resetStates)

                return Transition(
                    state.copy(
                        street = PokerStreet.RIVER,
                        deckCards = remainingDeck,
                        communityCards = community,
                        burnedCards = burned,
                        currentActor = firstActor,
                        currentStreetHighBet = 0L,
                        minRaise = state.bigBlind,
                        lastBettor = null,
                        actedThisStreet = emptySet(),
                        raiseLocked = emptySet(),
                        playerStates = resetStates,
                    ),
                    emptyList(),
                )
            }
            PokerStreet.RIVER -> {
                return showdown(state.copy(
                    playerStates = resetStates,
                ))
            }
            PokerStreet.SHOWDOWN, PokerStreet.FINISHED -> {
                return Transition(state, emptyList())
            }
        }
    }

    private fun showdown(state: PokerState): Transition<PokerState> {
        val investedMap = state.playerStates.mapValues { (_, s) -> s.investedThisHand }
        val foldedSet = state.playerStates.filter { it.value.isFolded }.keys
        val handsMap = state.playerStates.mapValues { (_, s) ->
            PokerEvaluator.evaluate(s.holeCards + state.communityCards)
        }

        val (_, results) = PokerScoring.computeSettlement(
            players = state.players,
            invested = investedMap,
            folded = foldedSet,
            hands = handsMap,
            dealerIndex = state.dealerIndex,
        )

        val finalStates = state.playerStates.mapValues { (id, s) ->
            val payout = results.firstOrNull { it.id == id }?.payout ?: 0L
            s.copy(stack = s.stack + payout)
        }

        return Transition(
            state.copy(
                street = PokerStreet.FINISHED,
                currentActor = null,
                playerStates = finalStates,
                results = results,
            ),
            emptyList(),
        )
    }

    private fun findNextActor(
        players: List<PlayerId>,
        current: PlayerId,
        states: Map<PlayerId, PokerPlayerState>,
    ): PlayerId? {
        val currentIndex = players.indexOf(current)
        for (i in 1..players.size) {
            val idx = (currentIndex + i) % players.size
            val candidate = players[idx]
            val s = states[candidate]!!
            if (!s.isFolded && !s.isAllIn) {
                return candidate
            }
        }
        return null
    }

    private fun findFirstActorPostflop(
        players: List<PlayerId>,
        dealerIndex: Int,
        states: Map<PlayerId, PokerPlayerState>,
    ): PlayerId? {
        for (i in 1..players.size) {
            val idx = (dealerIndex + i) % players.size
            val candidate = players[idx]
            val s = states[candidate]!!
            if (!s.isFolded && !s.isAllIn) {
                return candidate
            }
        }
        return null
    }

    override fun isFinished(state: PokerState): Boolean = state.street == PokerStreet.FINISHED

    override fun settle(state: PokerState): Settlement {
        check(isFinished(state)) { "Chỉ được settle khi ván Poker đã kết thúc" }
        val deltas = state.results.associate { it.id to it.delta }
        return Settlement(deltas, emptyList())
    }

    override fun viewOf(state: PokerState, player: PlayerId): PokerView {
        val pState = state.playerStates[player]
        val myHole = pState?.holeCards ?: emptyList()
        val allCards = myHole + state.communityCards
        val myHand = if (allCards.isNotEmpty()) PokerEvaluator.evaluate(allCards) else null

        val toCall = maxOf(0L, state.currentStreetHighBet - (pState?.currentStreetBet ?: 0L))
        val minRaiseTotal = state.currentStreetHighBet + state.minRaise
        val maxRaiseTotal = (pState?.currentStreetBet ?: 0L) + (pState?.stack ?: 0L)

        val playerInfos = state.players.map { id ->
            val s = state.playerStates[id]!!
            val isMe = (id == player)
            val showCards = isMe || state.street == PokerStreet.FINISHED || state.street == PokerStreet.SHOWDOWN
            val cards = if (showCards && !s.isFolded) s.holeCards else null
            val hand = if (cards != null && state.communityCards.isNotEmpty()) {
                PokerEvaluator.evaluate(cards + state.communityCards)
            } else null

            val seatIndex = state.players.indexOf(id)
            val isDealer = seatIndex == state.dealerIndex
            val isSB = if (state.players.size == 2) isDealer else seatIndex == (state.dealerIndex + 1) % state.players.size
            val isBB = if (state.players.size == 2) !isDealer else seatIndex == (state.dealerIndex + 2) % state.players.size

            PokerPlayerInfo(
                id = id,
                stack = s.stack,
                investedThisHand = s.investedThisHand,
                currentStreetBet = s.currentStreetBet,
                isFolded = s.isFolded,
                isAllIn = s.isAllIn,
                holeCards = cards,
                hand = hand,
                isDealer = isDealer,
                isSB = isSB,
                isBB = isBB,
            )
        }

        return PokerView(
            myId = player,
            street = state.street,
            communityCards = state.communityCards,
            myHoleCards = myHole,
            myHand = myHand,
            currentActor = state.currentActor,
            dealerIndex = state.dealerIndex,
            pot = state.pot,
            currentStreetHighBet = state.currentStreetHighBet,
            toCall = toCall,
            minRaiseTotal = minRaiseTotal,
            maxRaiseTotal = maxRaiseTotal,
            players = playerInfos,
            results = state.results,
            rules = state.rules,
        )
    }
}
