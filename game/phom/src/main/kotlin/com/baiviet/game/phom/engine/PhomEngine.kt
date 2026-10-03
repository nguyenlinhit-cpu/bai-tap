package com.baiviet.game.phom.engine

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Deck
import com.baiviet.core.engine.GameEngine
import com.baiviet.core.engine.GameEvent
import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.SettlementLine
import com.baiviet.core.engine.TableConfig
import com.baiviet.core.engine.Transition
import com.baiviet.game.phom.rules.PhomEatRules
import com.baiviet.game.phom.rules.PhomMeld
import com.baiviet.game.phom.rules.PhomMeldFinder
import com.baiviet.game.phom.rules.PhomPlayerResult
import com.baiviet.game.phom.rules.PhomRules
import com.baiviet.game.phom.rules.PhomScoring
import com.baiviet.game.phom.rules.phomPoint
import com.baiviet.game.phom.rules.sortedPhom

/**
 * Engine luật Phỏm (Tá Lả) — thuần Kotlin, không phụ thuộc Android.
 */
class PhomEngine : GameEngine<PhomState, PhomAction, PhomView, PhomRules> {

    override fun start(table: TableConfig<PhomRules>, seed: Long): PhomState =
        startGame(table, seed, previousWinner = null)

    fun startGame(
        table: TableConfig<PhomRules>,
        seed: Long,
        previousWinner: PlayerId?,
    ): PhomState {
        val (_, deck) = Deck.shuffled(seed)
        val leader = if (previousWinner != null && previousWinner.seat in 0 until table.playerCount) {
            previousWinner.seat
        } else {
            0
        }

        // Người đi đầu nhận 10 lá, các người khác nhận 9 lá
        var offset = 0
        val hands = (0 until table.playerCount).map { seat ->
            val count = if (seat == leader) 10 else 9
            val hand = deck.subList(offset, offset + count).sortedPhom()
            offset += count
            hand
        }
        val stock = deck.subList(offset, deck.size)

        return startWithHands(
            rules = table.rules,
            playerCount = table.playerCount,
            betUnit = table.betUnit,
            hands = hands,
            stock = stock,
            leader = leader,
            seed = seed,
        )
    }

    fun startWithHands(
        rules: PhomRules,
        playerCount: Int,
        betUnit: Long,
        hands: List<List<Card>>,
        stock: List<Card>,
        leader: Int = 0,
        seed: Long = 0L,
    ): PhomState {
        require(hands.size == playerCount) { "Số tay bài phải bằng số người" }
        require(playerCount in 2..4) { "Phỏm hỗ trợ từ 2 đến 4 người" }

        val sortedHands = hands.map { it.sortedPhom() }

        val base = PhomState(
            rules = rules,
            playerCount = playerCount,
            betUnit = betUnit,
            seed = seed,
            isFirstGame = true,
            leader = leader,
            turn = leader,
            phase = PhomPhase.DISCARD, // Leader có 10 lá nên đi đầu bằng cách đánh rác
            hands = sortedHands,
            stock = stock,
        )

        // Kiểm tra Ù khan ngay khi chia bài (nếu luật nhà bật)
        if (rules.uKhanEnabled) {
            val uKhanWinner = (0 until playerCount).firstOrNull { seat ->
                PhomMeldFinder.isUKhan(sortedHands[seat])
            }
            if (uKhanWinner != null) {
                return base.copy(
                    winner = uKhanWinner,
                    isUKhan = true,
                    finished = true,
                )
            }
        }

        return base
    }

    override fun currentActors(state: PhomState): Set<PlayerId> =
        if (state.finished) emptySet() else setOf(PlayerId(state.turn))

    override fun legalActions(state: PhomState, player: PlayerId): List<PhomAction> {
        if (state.finished || player.seat != state.turn) return emptyList()
        val seat = player.seat
        val hand = state.hands[seat]

        return when (state.phase) {
            PhomPhase.DRAW_OR_EAT -> {
                buildList {
                    // 1. Có thể bốc từ nọc nếu nọc còn bài
                    if (state.stock.isNotEmpty()) {
                        add(PhomAction.Draw)
                    }

                    // 2. Có thể ăn bài nếu người liền trước vừa đánh ra lá hợp lệ
                    val lastDiscard = state.lastDiscard
                    val lastDiscarder = state.lastDiscarder
                    val prevSeat = (seat - 1 + state.playerCount) % state.playerCount
                    if (lastDiscard != null && lastDiscarder == prevSeat) {
                        val eatMelds = PhomEatRules.findPossibleEatMelds(hand, state.eatenCards[seat], lastDiscard)
                        for (meld in eatMelds) {
                            add(PhomAction.Eat(meld.cards))
                        }
                    }
                }
            }

            PhomPhase.MELD_AND_LAYOFF -> {
                buildList {
                    if (!state.hasMelded(seat)) {
                        // Người chơi chưa hạ: tìm các cách hạ phỏm hợp lệ
                        val best = PhomMeldFinder.findBestPartition(hand, state.eatenCards[seat])
                        add(PhomAction.Meld(best.melds))
                    } else {
                        // Đã hạ phỏm: có thể gửi bài hoặc bỏ qua gửi bài
                        add(PhomAction.PassLayOff)
                        val layOffs = findPossibleLayOffs(state, seat)
                        if (layOffs.isNotEmpty()) {
                            add(PhomAction.LayOff(layOffs))
                        }
                    }
                }
            }

            PhomPhase.DISCARD -> {
                buildList {
                    // Kiểm tra xem có thể báo Ù không
                    if (PhomMeldFinder.checkU(hand, state.eatenCards[seat])) {
                        add(PhomAction.DeclareU)
                    }

                    // Các lá rác có thể đánh đi (không bị trói)
                    val legals = PhomEatRules.legalDiscards(hand, state.eatenCards[seat], state.exposedMelds[seat])
                    for (card in legals) {
                        add(PhomAction.Discard(card))
                    }
                }
            }
        }
    }

    override fun apply(state: PhomState, player: PlayerId, action: PhomAction): Transition<PhomState> {
        val seat = player.seat
        if (state.finished) throw IllegalActionException("Ván đã kết thúc")
        if (seat != state.turn) throw IllegalActionException("Chưa đến lượt của ghế $seat (lượt của ${state.turn})")

        return when (action) {
            is PhomAction.Draw -> handleDraw(state, seat)
            is PhomAction.Eat -> handleEat(state, seat, action.meldCards)
            is PhomAction.Meld -> handleMeld(state, seat, action.melds)
            is PhomAction.LayOff -> handleLayOff(state, seat, action.layOffs)
            is PhomAction.PassLayOff -> handlePassLayOff(state, seat)
            is PhomAction.Discard -> handleDiscard(state, seat, action.card)
            is PhomAction.DeclareU -> handleDeclareU(state, seat)
            is PhomAction.DeclareUKhan -> handleDeclareUKhan(state, seat)
        }
    }

    private fun handleDraw(state: PhomState, seat: Int): Transition<PhomState> {
        if (state.phase != PhomPhase.DRAW_OR_EAT) throw IllegalActionException("Không trong pha bốc/ăn")
        if (state.stock.isEmpty()) throw IllegalActionException("Nọc đã hết bài")

        val drawnCard = state.stock.first()
        val newStock = state.stock.drop(1)
        val newHand = (state.hands[seat] + drawnCard).sortedPhom()
        val newHands = state.hands.toMutableList()
        newHands[seat] = newHand

        // Nếu là vòng cuối của người này (đã đánh 3 lá rác, chuẩn bị đánh lá thứ 4) -> chuyển sang MELD_AND_LAYOFF
        val isFinal = state.isFinalRound(seat)
        val nextPhase = if (isFinal) PhomPhase.MELD_AND_LAYOFF else PhomPhase.DISCARD

        val nextState = state.copy(
            hands = newHands,
            stock = newStock,
            phase = nextPhase,
            moveCount = state.moveCount + 1,
        )

        return Transition(
            nextState,
            listOf(GameEvent.Drew(PlayerId(seat))),
        )
    }

    private fun handleEat(state: PhomState, seat: Int, meldCards: List<Card>): Transition<PhomState> {
        if (state.phase != PhomPhase.DRAW_OR_EAT) throw IllegalActionException("Không trong pha bốc/ăn")
        val incoming = state.lastDiscard ?: throw IllegalActionException("Không có lá bài để ăn")
        val victim = state.lastDiscarder ?: throw IllegalActionException("Không xác định được người bị ăn")
        if (incoming !in meldCards) throw IllegalActionException("Phỏm ăn phải chứa lá bài vừa đánh")

        val hand = state.hands[seat]
        val otherCards = meldCards - incoming
        if (!hand.containsAll(otherCards)) throw IllegalActionException("Bài trên tay không chứa các lá tạo phỏm")

        val validMelds = PhomEatRules.findPossibleEatMelds(hand, state.eatenCards[seat], incoming)
        val matchedMeld = validMelds.firstOrNull { it.cards.toSet() == meldCards.toSet() }
            ?: throw IllegalActionException("Tổ hợp ăn bài không hợp lệ theo luật")

        // Ăn bài thành công: thêm lá incoming vào tay, ghi nhận eatenCards
        val newHand = (hand + incoming).sortedPhom()
        val newHands = state.hands.toMutableList()
        newHands[seat] = newHand

        val newEaten = state.eatenCards.toMutableList()
        newEaten[seat] = state.eatenCards[seat] + incoming

        // Tính tiền phạt ăn bài
        val isChot = state.discardPiles[victim].size == 4
        val eatCount = state.eatCountMatrix[victim][seat] + 1
        val newMatrix = state.eatCountMatrix.mapIndexed { vIdx, row ->
            if (vIdx == victim) {
                row.mapIndexed { eIdx, cnt -> if (eIdx == seat) cnt + 1 else cnt }
            } else {
                row
            }
        }

        val penaltyCards = when {
            isChot -> state.rules.chotEatCards
            state.rules.progressiveEatPenalty -> when (eatCount) {
                1 -> state.rules.normalEatCards
                2 -> state.rules.secondEatCards
                else -> state.rules.thirdEatCards
            }
            else -> state.rules.normalEatCards
        }

        val eatDesc = if (isChot) "Bị ăn chốt (−${penaltyCards}B)" else "Bị ăn bài (−${penaltyCards}B)"
        val newEatLines = state.eatLines + SettlementLine(
            from = PlayerId(victim),
            to = PlayerId(seat),
            amount = penaltyCards * state.betUnit,
            reason = eatDesc,
        )

        val newLastChotEater = if (isChot) seat else state.lastChotEater

        val isFinal = state.isFinalRound(seat)
        val nextPhase = if (isFinal) PhomPhase.MELD_AND_LAYOFF else PhomPhase.DISCARD

        val nextState = state.copy(
            hands = newHands,
            eatenCards = newEaten,
            phase = nextPhase,
            eatCountMatrix = newMatrix,
            lastChotEater = newLastChotEater,
            eatLines = newEatLines,
            moveCount = state.moveCount + 1,
        )

        val event = if (isChot) {
            GameEvent.Announced(PlayerId(seat), "Ăn chốt!")
        } else {
            GameEvent.Announced(PlayerId(seat), "Ăn bài: ${matchedMeld.description}")
        }

        return Transition(nextState, listOf(event))
    }

    private fun handleMeld(state: PhomState, seat: Int, melds: List<PhomMeld>): Transition<PhomState> {
        if (state.phase != PhomPhase.MELD_AND_LAYOFF) throw IllegalActionException("Không trong pha hạ phỏm")
        if (state.hasMelded(seat)) throw IllegalActionException("Đã hạ phỏm rồi")

        val hand = state.hands[seat]
        val meldedCards = melds.flatMap { it.cards }
        if (!hand.containsAll(meldedCards)) throw IllegalActionException("Phỏm hạ chứa các lá không có trên tay")

        // Kiểm tra mọi lá ăn phải nằm trong các phỏm
        val eaten = state.eatenCards[seat]
        if (!meldedCards.containsAll(eaten)) throw IllegalActionException("Mọi lá bài đã ăn phải nằm trong phỏm khi hạ")

        // Cập nhật exposedMelds và playerMeldOrder
        val newExposed = state.exposedMelds.toMutableList()
        newExposed[seat] = melds

        val newOrder = state.playerMeldOrder.toMutableList()
        newOrder[seat] = state.meldOrderCount

        val nextState = state.copy(
            exposedMelds = newExposed,
            playerMeldOrder = newOrder,
            meldOrderCount = state.meldOrderCount + 1,
            moveCount = state.moveCount + 1,
        )

        val desc = if (melds.isEmpty()) "Móm (không có phỏm)" else melds.joinToString(", ") { it.description }
        val events = listOf(GameEvent.Announced(PlayerId(seat), "Hạ: $desc"))

        return Transition(nextState, events)
    }

    private fun handleLayOff(
        state: PhomState,
        seat: Int,
        layOffs: List<Pair<Card, Pair<Int, Int>>>,
    ): Transition<PhomState> {
        if (state.phase != PhomPhase.MELD_AND_LAYOFF) throw IllegalActionException("Không trong pha gửi bài")
        if (!state.hasMelded(seat)) throw IllegalActionException("Phải hạ phỏm trước khi gửi bài")

        var curHand = state.hands[seat]
        val newExposed = state.exposedMelds.map { it.toMutableList() }.toMutableList()
        var actualCount = 0

        for ((card, target) in layOffs) {
            val (targetSeat, meldIdx) = target
            if (card !in curHand) continue
            val targetMeld = newExposed[targetSeat].getOrNull(meldIdx) ?: continue
            if (!targetMeld.canLayOff(card)) continue

            newExposed[targetSeat][meldIdx] = targetMeld.withLayOff(card)
            curHand = curHand - card
            actualCount++
        }

        val newHands = state.hands.toMutableList()
        newHands[seat] = curHand

        val nextState = state.copy(
            hands = newHands,
            exposedMelds = newExposed,
            phase = PhomPhase.DISCARD,
            moveCount = state.moveCount + 1,
        )

        return Transition(
            nextState,
            listOf(GameEvent.Announced(PlayerId(seat), "Đã gửi ${layOffs.size} lá bài")),
        )
    }

    private fun handlePassLayOff(state: PhomState, seat: Int): Transition<PhomState> {
        if (state.phase != PhomPhase.MELD_AND_LAYOFF) throw IllegalActionException("Không trong pha gửi bài")
        if (!state.hasMelded(seat)) throw IllegalActionException("Phải hạ phỏm trước khi qua lượt gửi")

        val nextState = state.copy(
            phase = PhomPhase.DISCARD,
            moveCount = state.moveCount + 1,
        )
        return Transition(nextState, emptyList())
    }

    private fun handleDiscard(state: PhomState, seat: Int, card: Card): Transition<PhomState> {
        if (state.phase != PhomPhase.DISCARD) throw IllegalActionException("Không trong pha đánh bài")
        val hand = state.hands[seat]
        if (card !in hand) throw IllegalActionException("Bài trên tay không chứa lá $card")
        val legals = PhomEatRules.legalDiscards(hand, state.eatenCards[seat], state.exposedMelds[seat])
        if (card !in legals) {
            throw IllegalActionException("Lá $card bị trói hoặc là lá đã ăn, không thể đánh")
        }

        val newHand = (hand - card).sortedPhom()
        val newHands = state.hands.toMutableList()
        newHands[seat] = newHand

        val newDiscardPiles = state.discardPiles.toMutableList()
        newDiscardPiles[seat] = state.discardPiles[seat] + card

        // Kiểm tra xem ván đã kết thúc chưa:
        // Ván kết thúc khi tất cả mọi người đã đánh đủ 4 lá rác (discardCount == 4 cho tất cả)
        val allFinishedDiscards = (0 until state.playerCount).all { newDiscardPiles[it].size >= 4 }

        if (allFinishedDiscards) {
            val nextState = state.copy(
                hands = newHands,
                discardPiles = newDiscardPiles,
                lastDiscard = card,
                lastDiscarder = seat,
                finished = true,
                moveCount = state.moveCount + 1,
            )
            val ranked = rankEndOfGame(nextState)
            val winnerSeat = ranked.first()
            val finalState = nextState.copy(winner = winnerSeat)
            val settlement = settle(finalState)

            val events = listOf(
                GameEvent.Played(PlayerId(seat), listOf(card), "Đánh ${card.rank.label}${card.suit.symbol}"),
                GameEvent.Settled(settlement),
            )
            return Transition(finalState, events)
        }

        // Chuyển lượt sang người kế tiếp còn cần đánh bài
        var nextSeat = (seat + 1) % state.playerCount
        while (newDiscardPiles[nextSeat].size >= 4) {
            nextSeat = (nextSeat + 1) % state.playerCount
        }

        val nextState = state.copy(
            hands = newHands,
            discardPiles = newDiscardPiles,
            lastDiscard = card,
            lastDiscarder = seat,
            turn = nextSeat,
            phase = PhomPhase.DRAW_OR_EAT,
            moveCount = state.moveCount + 1,
        )

        return Transition(
            nextState,
            listOf(GameEvent.Played(PlayerId(seat), listOf(card), "Đánh ${card.rank.label}${card.suit.symbol}")),
        )
    }

    private fun handleDeclareU(state: PhomState, seat: Int): Transition<PhomState> {
        val hand = state.hands[seat]
        if (!PhomMeldFinder.checkU(hand, state.eatenCards[seat])) {
            throw IllegalActionException("Bài trên tay không đủ điều kiện Ù")
        }

        // Xác định xem có người đền làng không:
        // (a) Để cùng 1 người ăn 3 lá -> người bị ăn đền làng
        val victimEat3 = (0 until state.playerCount).firstOrNull { v -> state.eatCountMatrix[v][seat] >= 3 }

        // (b) Đánh cây chốt để người này ăn và Ù ngay
        val victimChot = if (state.lastChotEater == seat) state.lastDiscarder else null

        // (c) Ăn chốt đền: người ăn chốt sau cùng đền nếu có người Ù trong vòng cuối
        val victimChotDen = if (state.rules.eatChotDenEnabled && state.lastChotEater != null && state.lastChotEater != seat) {
            state.lastChotEater
        } else {
            null
        }

        val uDenSeat = victimEat3 ?: victimChot ?: victimChotDen

        val nextState = state.copy(
            winner = seat,
            isU = true,
            finished = true,
            uDenSeat = uDenSeat,
            moveCount = state.moveCount + 1,
        )

        val settlement = settle(nextState)
        val uDesc = when {
            uDenSeat != null -> "Ù đền làng (ghế $uDenSeat đền)!"
            else -> "Ù!"
        }

        return Transition(
            nextState,
            listOf(
                GameEvent.Announced(PlayerId(seat), uDesc),
                GameEvent.Settled(settlement),
            ),
        )
    }

    private fun handleDeclareUKhan(state: PhomState, seat: Int): Transition<PhomState> {
        if (!state.rules.uKhanEnabled) throw IllegalActionException("Luật Ù khan không được bật")
        if (!PhomMeldFinder.isUKhan(state.hands[seat])) throw IllegalActionException("Bài không phải Ù khan")

        val nextState = state.copy(
            winner = seat,
            isUKhan = true,
            finished = true,
            moveCount = state.moveCount + 1,
        )
        val settlement = settle(nextState)

        return Transition(
            nextState,
            listOf(
                GameEvent.Announced(PlayerId(seat), "Ù khan!"),
                GameEvent.Settled(settlement),
            ),
        )
    }

    fun rankEndOfGame(state: PhomState): List<Int> {
        val results = (0 until state.playerCount).map { seat ->
            val melds = state.exposedMelds[seat]
            val hand = state.hands[seat]
            val isMom = melds.isEmpty()
            val trash = if (isMom) hand else hand.filter { card -> melds.none { card in it.cards } }
            val points = trash.sumOf { it.phomPoint }
            val meldOrder = state.playerMeldOrder[seat] ?: Int.MAX_VALUE

            PhomPlayerResult(
                seat = seat,
                isU = state.isU && state.winner == seat,
                isUKhan = state.isUKhan && state.winner == seat,
                isMom = isMom,
                points = points,
                melds = melds,
                remainingTrash = trash,
                meldOrder = meldOrder,
            )
        }
        return PhomScoring.rankPlayers(results)
    }

    override fun settle(state: PhomState): Settlement {
        val allPlayers = (0 until state.playerCount).map { PlayerId(it) }
        val results = (0 until state.playerCount).map { seat ->
            val melds = state.exposedMelds[seat]
            val hand = state.hands[seat]
            val isMom = melds.isEmpty()
            val trash = if (isMom) hand else hand.filter { card -> melds.none { card in it.cards } }
            val points = trash.sumOf { it.phomPoint }
            val meldOrder = state.playerMeldOrder[seat] ?: Int.MAX_VALUE

            PhomPlayerResult(
                seat = seat,
                isU = state.isU && state.winner == seat,
                isUKhan = state.isUKhan && state.winner == seat,
                isMom = isMom,
                points = points,
                melds = melds,
                remainingTrash = trash,
                meldOrder = meldOrder,
            )
        }

        val rankedSeats = if (state.isU || state.isUKhan) {
            listOf(state.winner!!) + (0 until state.playerCount).filter { it != state.winner }
        } else {
            PhomScoring.rankPlayers(results)
        }

        val lines = PhomScoring.createSettlementLines(
            results = results,
            rankedSeats = rankedSeats,
            rules = state.rules,
            betUnit = state.betUnit,
            eatLines = state.eatLines,
            uDenSeat = state.uDenSeat,
        )

        return Settlement.fromLines(allPlayers, lines)
    }

    override fun isFinished(state: PhomState): Boolean = state.finished

    override fun viewOf(state: PhomState, player: PlayerId): PhomView {
        val seat = player.seat
        return PhomView(
            me = seat,
            playerCount = state.playerCount,
            rules = state.rules,
            betUnit = state.betUnit,
            myHand = state.hands[seat],
            handCounts = state.hands.map { it.size },
            turn = state.turn,
            phase = state.phase,
            eatenCards = state.eatenCards,
            discardPiles = state.discardPiles,
            stockCount = state.stock.size,
            lastDiscard = state.lastDiscard,
            lastDiscarder = state.lastDiscarder,
            exposedMelds = state.exposedMelds,
            winner = state.winner,
            isU = state.isU,
            isUKhan = state.isUKhan,
            finished = state.finished,
        )
    }

    private fun findPossibleLayOffs(state: PhomState, seat: Int): List<Pair<Card, Pair<Int, Int>>> {
        val meldedCards = state.exposedMelds[seat].flatMap { it.cards }.toSet()
        val trashCards = state.hands[seat].filter { it !in meldedCards }
        val list = mutableListOf<Pair<Card, Pair<Int, Int>>>()
        val usedCards = mutableSetOf<Card>()
        val currentExposed = state.exposedMelds.map { it.toMutableList() }

        for (card in trashCards) {
            if (card in usedCards) continue
            var found = false
            for (otherSeat in 0 until state.playerCount) {
                if (otherSeat == seat) continue
                val melds = currentExposed[otherSeat]
                for (mIdx in melds.indices) {
                    if (melds[mIdx].canLayOff(card)) {
                        list.add(card to (otherSeat to mIdx))
                        currentExposed[otherSeat][mIdx] = melds[mIdx].withLayOff(card)
                        usedCards.add(card)
                        found = true
                        break
                    }
                }
                if (found) break
            }
        }
        return list
    }
}
