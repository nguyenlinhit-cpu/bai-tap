package com.baiviet.game.xidach.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Deck
import com.baiviet.core.engine.GameEngine
import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.TableConfig
import com.baiviet.core.engine.Transition
import com.baiviet.game.xidach.rules.XiDachEvaluator
import com.baiviet.game.xidach.rules.XiDachHandType
import com.baiviet.game.xidach.rules.XiDachRules
import com.baiviet.game.xidach.rules.XiDachScoring

class XiDachEngine : GameEngine<XiDachState, XiDachAction, XiDachView, XiDachRules> {

    override fun start(table: TableConfig<XiDachRules>, seed: Long): XiDachState = startRound(table, seed, dealerSeat = 0, bets = null)

    /**
     * Bắt đầu ván với nhà cái và mức cược cho trước.
     *
     * @param dealerSeat ghế làm cái
     * @param bets mức cược từng ghế (null = mỗi người 1B)
     */
    fun startRound(table: TableConfig<XiDachRules>, seed: Long, dealerSeat: Int, bets: List<Long>?): XiDachState {
        val players = List(table.playerCount) { PlayerId(it) }
        val dealerId = players[dealerSeat]

        val (_, shuffledDeck) = Deck.shuffled(seed)
        val (dealtHands, remainder) = Deck.deal(
            deck = shuffledDeck,
            playerCount = table.playerCount,
            cardsPerPlayer = 2,
        )

        var playerStates = players.indices.associate { index ->
            val pid = players[index]
            pid to XiDachPlayerState(
                id = pid,
                cards = dealtHands[index],
                betAmount = bets?.get(index) ?: table.betUnit,
                status = if (pid == dealerId) XiDachPlayerStatus.WAITING else XiDachPlayerStatus.WAITING,
                isRevealed = false,
            )
        }

        // Đánh giá đầu ván: kiểm tra Xì bàng / Xì dách
        val dealerHand = XiDachEvaluator.evaluate(playerStates[dealerId]!!.cards, isDealer = true)

        if (dealerHand.type == XiDachHandType.XI_BANG || dealerHand.type == XiDachHandType.XI_DACH) {
            // Nhà cái có Xì bàng hoặc Xì dách -> lật ngay, xét cả bàn, kết thúc ván lập tức
            playerStates = playerStates.mapValues { (_, state) ->
                state.copy(isRevealed = true, status = XiDachPlayerStatus.RESOLVED)
            }
            val handsMap = playerStates.mapValues { (pid, state) ->
                XiDachEvaluator.evaluate(state.cards, isDealer = pid == dealerId)
            }
            val betsMap = playerStates.mapValues { (_, s) -> s.betAmount }
            val (_, results) = XiDachScoring.computeSettlement(dealerId, players, betsMap, handsMap, table.rules)

            return XiDachState(
                players = players,
                dealerId = dealerId,
                currentActor = null,
                phase = XiDachPhase.FINISHED,
                playerStates = playerStates,
                remainingDeck = remainder,
                rules = table.rules,
                results = results,
            )
        }

        // Nhà cái không có -> kiểm tra các con có Xì bàng hoặc Xì dách
        val updatedStates = playerStates.toMutableMap()
        for (pid in players) {
            if (pid == dealerId) continue
            val hand = XiDachEvaluator.evaluate(updatedStates[pid]!!.cards, isDealer = false)
            if (hand.type == XiDachHandType.XI_BANG || hand.type == XiDachHandType.XI_DACH) {
                // Thắng luôn đầu ván!
                updatedStates[pid] = updatedStates[pid]!!.copy(
                    isRevealed = true,
                    status = XiDachPlayerStatus.RESOLVED,
                )
            }
        }

        // Tìm người con đầu tiên chưa resolved để bắt đầu lượt rút
        val nonDealerPlayers = players.filter { it != dealerId }
        val firstActor = nonDealerPlayers.firstOrNull { updatedStates[it]!!.status != XiDachPlayerStatus.RESOLVED }

        return if (firstActor == null) {
            // Tất cả con đều có Xì bàng / Xì dách -> kết thúc ván
            val allRevealed = updatedStates.mapValues { (_, s) -> s.copy(isRevealed = true, status = XiDachPlayerStatus.RESOLVED) }
            val handsMap = allRevealed.mapValues { (pid, state) ->
                XiDachEvaluator.evaluate(state.cards, isDealer = pid == dealerId)
            }
            val betsMap = allRevealed.mapValues { (_, s) -> s.betAmount }
            val (_, results) = XiDachScoring.computeSettlement(dealerId, players, betsMap, handsMap, table.rules)

            XiDachState(
                players = players,
                dealerId = dealerId,
                currentActor = null,
                phase = XiDachPhase.FINISHED,
                playerStates = allRevealed,
                remainingDeck = remainder,
                rules = table.rules,
                results = results,
            )
        } else {
            updatedStates[firstActor] = updatedStates[firstActor]!!.copy(status = XiDachPlayerStatus.PLAYING)
            XiDachState(
                players = players,
                dealerId = dealerId,
                currentActor = firstActor,
                phase = XiDachPhase.PLAYER_TURNS,
                playerStates = updatedStates,
                remainingDeck = remainder,
                rules = table.rules,
            )
        }
    }

    override fun currentActors(state: XiDachState): Set<PlayerId> =
        state.currentActor?.let { setOf(it) } ?: emptySet()

    override fun legalActions(state: XiDachState, player: PlayerId): List<XiDachAction> {
        if (state.phase == XiDachPhase.FINISHED || state.currentActor != player) return emptyList()

        val pState = state.playerStates[player] ?: return emptyList()
        val isDealer = player == state.dealerId
        val hand = XiDachEvaluator.evaluate(pState.cards, isDealer = isDealer)

        if (state.phase == XiDachPhase.PLAYER_TURNS) {
            val actions = mutableListOf<XiDachAction>()
            // Rút bài: tối đa 5 lá và chưa quắc
            if (pState.cards.size < 5 && !hand.isBust && hand.type != XiDachHandType.NGU_LINH) {
                actions.add(XiDachAction.Hit)
            }
            // Dằn bài: phải đủ tuổi (>= playerMinScore) hoặc có Ngũ linh
            if (hand.score >= state.rules.playerMinScore || hand.type == XiDachHandType.NGU_LINH) {
                actions.add(XiDachAction.Stand)
            }
            return actions
        }

        if (state.phase == XiDachPhase.DEALER_TURN && isDealer) {
            val actions = mutableListOf<XiDachAction>()
            // Nhà cái có thể rút nếu chưa đủ 5 lá và chưa quắc
            if (pState.cards.size < 5 && !hand.isBust && hand.type != XiDachHandType.NGU_LINH) {
                actions.add(XiDachAction.Hit)
            }

            // Nhà cái chỉ được xét khi đạt mốc dealerMinScore (15) hoặc Ngũ linh
            if (hand.score >= state.rules.dealerMinScore || hand.type == XiDachHandType.NGU_LINH) {
                val unresolvedPlayers = state.players.filter {
                    it != state.dealerId && state.playerStates[it]?.status != XiDachPlayerStatus.RESOLVED
                }
                for (target in unresolvedPlayers) {
                    actions.add(XiDachAction.Inspect(target))
                }
                if (unresolvedPlayers.isNotEmpty()) {
                    actions.add(XiDachAction.InspectAll)
                }
            }
            return actions
        }

        return emptyList()
    }

    override fun apply(state: XiDachState, player: PlayerId, action: XiDachAction): Transition<XiDachState> {
        val legal = legalActions(state, player)
        if (action !in legal) {
            throw IllegalActionException("Hành động $action không hợp lệ cho $player trong trạng thái hiện tại")
        }

        return when (state.phase) {
            XiDachPhase.PLAYER_TURNS -> handlePlayerTurn(state, player, action)
            XiDachPhase.DEALER_TURN -> handleDealerTurn(state, player, action)
            else -> throw IllegalActionException("Không thể thực hiện hành động ở giai đoạn ${state.phase}")
        }
    }

    private fun handlePlayerTurn(state: XiDachState, player: PlayerId, action: XiDachAction): Transition<XiDachState> {
        val pState = state.playerStates[player]!!
        val updatedStates = state.playerStates.toMutableMap()
        var remainingDeck = state.remainingDeck

        when (action) {
            XiDachAction.Hit -> {
                val drawnCard = remainingDeck.first()
                remainingDeck = remainingDeck.drop(1)
                val newCards = pState.cards + drawnCard
                val newHand = XiDachEvaluator.evaluate(newCards, isDealer = false)

                when {
                    newHand.isBust -> {
                        // Quắc -> chuyển lượt tiếp
                        updatedStates[player] = pState.copy(cards = newCards, status = XiDachPlayerStatus.BUST)
                        return Transition(moveToNextActor(state.copy(playerStates = updatedStates, remainingDeck = remainingDeck), player), emptyList())
                    }
                    newCards.size == 5 -> {
                        // Đạt 5 lá (Ngũ linh hoặc đủ tuổi 5 lá) -> tự động dằn
                        updatedStates[player] = pState.copy(cards = newCards, status = XiDachPlayerStatus.STOOD)
                        return Transition(moveToNextActor(state.copy(playerStates = updatedStates, remainingDeck = remainingDeck), player), emptyList())
                    }
                    else -> {
                        // Có thể rút tiếp hoặc dằn nếu đủ tuổi
                        updatedStates[player] = pState.copy(cards = newCards)
                        return Transition(state.copy(playerStates = updatedStates, remainingDeck = remainingDeck), emptyList())
                    }
                }
            }
            XiDachAction.Stand -> {
                updatedStates[player] = pState.copy(status = XiDachPlayerStatus.STOOD)
                return Transition(moveToNextActor(state.copy(playerStates = updatedStates), player), emptyList())
            }
            else -> throw IllegalActionException("Hành động $action không hợp lệ cho nhà con")
        }
    }

    private fun moveToNextActor(state: XiDachState, current: PlayerId): XiDachState {
        val nonDealerPlayers = state.players.filter { it != state.dealerId }
        val currentIndex = nonDealerPlayers.indexOf(current)
        val nextPlayer = nonDealerPlayers.drop(currentIndex + 1).firstOrNull {
            val s = state.playerStates[it]!!.status
            s != XiDachPlayerStatus.RESOLVED && s != XiDachPlayerStatus.STOOD && s != XiDachPlayerStatus.BUST
        }

        return if (nextPlayer != null) {
            val updatedStates = state.playerStates.toMutableMap()
            updatedStates[nextPlayer] = updatedStates[nextPlayer]!!.copy(status = XiDachPlayerStatus.PLAYING)
            state.copy(currentActor = nextPlayer, playerStates = updatedStates)
        } else {
            // Chuyển sang lượt nhà cái!
            val updatedStates = state.playerStates.toMutableMap()
            updatedStates[state.dealerId] = updatedStates[state.dealerId]!!.copy(status = XiDachPlayerStatus.PLAYING)
            state.copy(
                phase = XiDachPhase.DEALER_TURN,
                currentActor = state.dealerId,
                playerStates = updatedStates,
            )
        }
    }

    private fun handleDealerTurn(state: XiDachState, player: PlayerId, action: XiDachAction): Transition<XiDachState> {
        val pState = state.playerStates[player]!!
        val updatedStates = state.playerStates.toMutableMap()
        var remainingDeck = state.remainingDeck

        when (action) {
            XiDachAction.Hit -> {
                val drawnCard = remainingDeck.first()
                remainingDeck = remainingDeck.drop(1)
                val newCards = pState.cards + drawnCard
                val newHand = XiDachEvaluator.evaluate(newCards, isDealer = true)

                if (newHand.isBust) {
                    // Nhà cái quắc -> lật bài tất cả các con còn lại, kết thúc ván!
                    for (pid in state.players) {
                        updatedStates[pid] = updatedStates[pid]!!.copy(
                            isRevealed = true,
                            status = XiDachPlayerStatus.RESOLVED,
                        )
                    }
                    val handsMap = updatedStates.mapValues { (pid, s) ->
                        XiDachEvaluator.evaluate(s.cards, isDealer = pid == state.dealerId)
                    }
                    val betsMap = updatedStates.mapValues { (_, s) -> s.betAmount }
                    val (_, results) = XiDachScoring.computeSettlement(state.dealerId, state.players, betsMap, handsMap, state.rules)

                    return Transition(
                        state.copy(
                            phase = XiDachPhase.FINISHED,
                            currentActor = null,
                            playerStates = updatedStates,
                            remainingDeck = remainingDeck,
                            results = results,
                        ),
                        emptyList(),
                    )
                } else {
                    updatedStates[player] = pState.copy(cards = newCards)
                    return Transition(state.copy(playerStates = updatedStates, remainingDeck = remainingDeck), emptyList())
                }
            }
            is XiDachAction.Inspect -> {
                // Xét 1 con cụ thể
                val targetState = updatedStates[action.target] ?: throw IllegalActionException("Không tìm thấy người chơi ${action.target}")
                updatedStates[action.target] = targetState.copy(isRevealed = true, status = XiDachPlayerStatus.RESOLVED)

                // Kiểm tra còn con nào chưa xét không?
                val remainingUnresolved = state.players.filter {
                    it != state.dealerId && updatedStates[it]!!.status != XiDachPlayerStatus.RESOLVED
                }

                if (remainingUnresolved.isEmpty()) {
                    // Đã xét hết -> kết thúc ván
                    updatedStates[state.dealerId] = updatedStates[state.dealerId]!!.copy(isRevealed = true, status = XiDachPlayerStatus.RESOLVED)
                    val handsMap = updatedStates.mapValues { (pid, s) ->
                        XiDachEvaluator.evaluate(s.cards, isDealer = pid == state.dealerId)
                    }
                    val betsMap = updatedStates.mapValues { (_, s) -> s.betAmount }
                    val (_, results) = XiDachScoring.computeSettlement(state.dealerId, state.players, betsMap, handsMap, state.rules)

                    return Transition(
                        state.copy(
                            phase = XiDachPhase.FINISHED,
                            currentActor = null,
                            playerStates = updatedStates,
                            results = results,
                        ),
                        emptyList(),
                    )
                } else {
                    return Transition(state.copy(playerStates = updatedStates), emptyList())
                }
            }
            XiDachAction.InspectAll -> {
                // Xét tất cả các con còn lại
                for (pid in state.players) {
                    updatedStates[pid] = updatedStates[pid]!!.copy(isRevealed = true, status = XiDachPlayerStatus.RESOLVED)
                }
                val handsMap = updatedStates.mapValues { (pid, s) ->
                    XiDachEvaluator.evaluate(s.cards, isDealer = pid == state.dealerId)
                }
                val betsMap = updatedStates.mapValues { (_, s) -> s.betAmount }
                val (_, results) = XiDachScoring.computeSettlement(state.dealerId, state.players, betsMap, handsMap, state.rules)

                return Transition(
                    state.copy(
                        phase = XiDachPhase.FINISHED,
                        currentActor = null,
                        playerStates = updatedStates,
                        results = results,
                    ),
                    emptyList(),
                )
            }
            else -> throw IllegalActionException("Hành động $action không hợp lệ cho nhà cái")
        }
    }

    override fun isFinished(state: XiDachState): Boolean = state.phase == XiDachPhase.FINISHED

    override fun settle(state: XiDachState): Settlement {
        check(isFinished(state)) { "Chỉ được settle khi ván đã kết thúc" }
        val handsMap = state.playerStates.mapValues { (pid, s) ->
            XiDachEvaluator.evaluate(s.cards, isDealer = pid == state.dealerId)
        }
        val betsMap = state.playerStates.mapValues { (_, s) -> s.betAmount }
        val (settlement, _) = XiDachScoring.computeSettlement(
            dealerId = state.dealerId,
            players = state.players,
            bets = betsMap,
            hands = handsMap,
            rules = state.rules,
        )
        return settlement
    }

    override fun viewOf(state: XiDachState, player: PlayerId): XiDachView {
        val myState = state.playerStates[player] ?: error("Người chơi $player không có trong bàn")
        val isFinished = state.phase == XiDachPhase.FINISHED
        val isDealer = player == state.dealerId
        val myHand = XiDachEvaluator.evaluate(myState.cards, isDealer = isDealer)

        val playerInfos = state.players.map { pid ->
            val pState = state.playerStates[pid]!!
            val canSee = isFinished || pid == player || pState.isRevealed
            XiDachPlayerInfo(
                id = pid,
                cardCount = pState.cards.size,
                cards = if (canSee) pState.cards else null,
                hand = if (canSee) XiDachEvaluator.evaluate(pState.cards, isDealer = pid == state.dealerId) else null,
                betAmount = pState.betAmount,
                status = pState.status,
                isRevealed = pState.isRevealed,
            )
        }

        return XiDachView(
            myId = player,
            dealerId = state.dealerId,
            currentActor = state.currentActor,
            phase = state.phase,
            myHand = myHand,
            players = playerInfos,
            rules = state.rules,
            results = state.results,
        )
    }
}
