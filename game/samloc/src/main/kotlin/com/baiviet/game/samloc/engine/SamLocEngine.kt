package com.baiviet.game.samloc.engine

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
import com.baiviet.game.samloc.rules.SamLocBeatRules
import com.baiviet.game.samloc.rules.SamLocCombo
import com.baiviet.game.samloc.rules.SamLocComboFinder
import com.baiviet.game.samloc.rules.SamLocComboType
import com.baiviet.game.samloc.rules.SamLocInstantWin
import com.baiviet.game.samloc.rules.SamLocPenalties
import com.baiviet.game.samloc.rules.SamLocRules
import com.baiviet.game.samloc.rules.SlOrder
import com.baiviet.game.samloc.rules.slRank
import com.baiviet.game.samloc.rules.sortedSl

/**
 * Engine luật Sâm Lốc — Kotlin thuần, không phụ thuộc Android.
 */
class SamLocEngine : GameEngine<SamLocState, SlAction, SamLocView, SamLocRules> {
    companion object {
        const val CARDS_PER_PLAYER = 10
    }

    override fun start(
        table: TableConfig<SamLocRules>,
        seed: Long,
    ): SamLocState = startGame(table, seed, previousWinner = null)

    fun startGame(
        table: TableConfig<SamLocRules>,
        seed: Long,
        previousWinner: PlayerId?,
    ): SamLocState {
        val (_, deck) = Deck.shuffled(seed)
        val (hands, _) = Deck.deal(deck, table.playerCount, CARDS_PER_PLAYER)
        return startWithHands(table.rules, table.playerCount, table.betUnit, hands, previousWinner, seed)
    }

    fun startWithHands(
        rules: SamLocRules,
        playerCount: Int,
        betUnit: Long,
        hands: List<List<Card>>,
        previousWinner: PlayerId? = null,
        seed: Long = 0L,
    ): SamLocState {
        require(hands.size == playerCount) { "Số tay bài phải bằng số người" }
        require(playerCount in 2..4) { "Sâm Lốc hỗ trợ từ 2 đến 4 người" }

        val isFirstGame = previousWinner == null
        val sortedHands = hands.map { it.sortedSl() }

        val nominalLeader =
            if (!isFirstGame && previousWinner!!.seat in 0 until playerCount) {
                previousWinner.seat
            } else {
                // Ván đầu tiên: chọn ngẫu nhiên dựa trên seed
                ((seed xor 0x5DEECE66DL).ushr(16) % playerCount).toInt().coerceIn(0, playerCount - 1)
            }

        val base =
            SamLocState(
                rules = rules,
                playerCount = playerCount,
                betUnit = betUnit,
                seed = seed,
                isFirstGame = isFirstGame,
                hands = sortedHands,
                phase = SlPhase.BAO_SAM,
                nominalLeader = nominalLeader,
                turn = nominalLeader,
                baoMot = (0 until playerCount).filter { sortedHands[it].size == 1 }.toSet(),
            )

        // Kiểm tra Ăn trắng
        val instantCandidates =
            (0 until playerCount)
                .mapNotNull { seat -> SamLocInstantWin.detect(sortedHands[seat], rules)?.let { seat to it } }
                .toMap()
        val instantWinner = SamLocInstantWin.resolve(instantCandidates, sortedHands)
        if (instantWinner != null) {
            return base.copy(
                finished = true,
                turn = instantWinner,
                winner = instantWinner,
                instantWin = SlInstantWinResult(instantWinner, instantCandidates.getValue(instantWinner)),
            )
        }

        return base
    }

    fun initialEvents(state: SamLocState): List<GameEvent> =
        buildList {
            add(GameEvent.Dealt((0 until state.playerCount).associate { PlayerId(it) to state.hands[it].size }))
            state.instantWin?.let {
                add(GameEvent.Announced(PlayerId(it.player), "Ăn trắng: ${it.type.viName}"))
            }
        }

    override fun currentActors(state: SamLocState): Set<PlayerId> = if (state.finished) emptySet() else setOf(PlayerId(state.turn))

    override fun legalActions(
        state: SamLocState,
        player: PlayerId,
    ): List<SlAction> {
        if (state.finished || player.seat != state.turn) return emptyList()
        val seat = player.seat
        val hand = state.hands[seat]

        return when (state.phase) {
            SlPhase.BAO_SAM -> {
                listOf(SlAction.CallSam, SlAction.SkipSam)
            }

            SlPhase.PLAYING -> {
                if (state.samCaller != null) {
                    if (seat == state.samCaller) {
                        // Người báo Sâm đang đánh tự do
                        val combos = SamLocComboFinder.all(hand)
                        combos.map { SlAction.Play(it.cards) }
                    } else {
                        // Đối thủ cố gắng chặn nước đánh của người Báo Sâm
                        val top = state.top ?: return emptyList()
                        val beats = SamLocComboFinder.beating(hand, top, state.rules)
                        beats.map { SlAction.Play(it.cards) } + listOf(SlAction.Pass)
                    }
                } else {
                    // Đánh bình thường
                    val top = state.top
                    if (top == null) {
                        val combos = SamLocComboFinder.all(hand)
                        combos.map { SlAction.Play(it.cards) }
                    } else {
                        val beats = SamLocComboFinder.beating(hand, top, state.rules)
                        beats.map { SlAction.Play(it.cards) } + listOf(SlAction.Pass)
                    }
                }
            }
        }
    }

    override fun apply(
        state: SamLocState,
        player: PlayerId,
        action: SlAction,
    ): Transition<SamLocState> {
        val seat = player.seat
        if (state.finished) throw IllegalActionException("Ván đã kết thúc")
        if (seat != state.turn) throw IllegalActionException("Chưa đến lượt của ghế $seat (lượt của ${state.turn})")

        return when (state.phase) {
            SlPhase.BAO_SAM -> applyBaoSam(state, seat, action)
            SlPhase.PLAYING -> applyPlaying(state, seat, action)
        }
    }

    private fun applyBaoSam(
        state: SamLocState,
        seat: Int,
        action: SlAction,
    ): Transition<SamLocState> =
        when (action) {
            is SlAction.CallSam -> {
                val nextState =
                    state.copy(
                        phase = SlPhase.PLAYING,
                        samCaller = seat,
                        turn = seat,
                        moveCount = state.moveCount + 1,
                    )
                Transition(
                    nextState,
                    listOf(GameEvent.Announced(PlayerId(seat), "Báo Sâm!")),
                )
            }

            is SlAction.SkipSam -> {
                val nextSeat = (seat + 1) % state.playerCount
                if (nextSeat == state.nominalLeader) {
                    // Tất cả mọi người đều bỏ qua pha báo Sâm
                    val nextState =
                        state.copy(
                            phase = SlPhase.PLAYING,
                            samCaller = null,
                            turn = state.nominalLeader,
                            moveCount = state.moveCount + 1,
                        )
                    Transition(
                        nextState,
                        listOf(
                            GameEvent.Passed(PlayerId(seat)),
                            GameEvent.RoundStarted(PlayerId(state.nominalLeader)),
                        ),
                    )
                } else {
                    val nextState =
                        state.copy(
                            turn = nextSeat,
                            moveCount = state.moveCount + 1,
                        )
                    Transition(nextState, listOf(GameEvent.Passed(PlayerId(seat))))
                }
            }

            else -> {
                throw IllegalActionException("Hành động không hợp lệ trong pha Báo Sâm")
            }
        }

    private fun applyPlaying(
        state: SamLocState,
        seat: Int,
        action: SlAction,
    ): Transition<SamLocState> =
        when (action) {
            is SlAction.Play -> handlePlay(state, seat, action.cards)
            is SlAction.Pass -> handlePass(state, seat)
            else -> throw IllegalActionException("Hành động không hợp lệ trong pha Đánh bài")
        }

    private fun handlePlay(
        state: SamLocState,
        seat: Int,
        cards: List<Card>,
    ): Transition<SamLocState> {
        val hand = state.hands[seat]
        if (!hand.containsAll(cards)) {
            throw IllegalActionException("Bài trên tay không chứa đủ các lá đã chọn")
        }
        val combo =
            SamLocCombo.classify(cards)
                ?: throw IllegalActionException("Bộ bài không hợp lệ theo luật Sâm Lốc")

        val top = state.top
        if (top != null && !SamLocBeatRules.canBeat(combo, top, state.rules)) {
            throw IllegalActionException("Bộ bài không chặn được bài trên bàn")
        }

        // Kiểm tra xem có phạm quy Báo 1 không:
        // Người ngồi ngay trước người Báo 1 khi đánh rác (1 lá) phải đánh lá rác lớn nhất có thể.
        var violator: Int? = state.violatorOfBao1
        val nextActive = nextActiveSeat(seat, state)
        if (state.samCaller == null && nextActive in state.baoMot && combo.type == SamLocComboType.SINGLE) {
            val singlesInHand = hand.map { it.slRank }
            val maxSingleRank = singlesInHand.maxOrNull() ?: -1
            if (combo.top.slRank < maxSingleRank) {
                // Vi phạm luật giữ cửa Báo 1!
                violator = seat
            }
        }

        val newHand = hand - cards.toSet()
        val newHands = state.hands.toMutableList()
        newHands[seat] = newHand

        val newPlayedCards = state.playedCards + cards
        val newHasPlayed = state.hasPlayed + seat
        val newBaoMot = (0 until state.playerCount).filter { newHands[it].size == 1 }.toSet()

        // Xử lý Chặt
        val isCut = top != null && SamLocBeatRules.isCut(combo, top, state.rules)
        val cutValue = state.rules.cutCards * state.betUnit
        val (newCutChain, cutEvent) =
            if (isCut) {
                val victim = state.topOwner!!
                val prevAmount = state.cutChain?.amount ?: 0L
                val chain =
                    SlCutChain(
                        cutter = seat,
                        victim = victim,
                        amount = prevAmount + cutValue,
                        description = if (prevAmount > 0) "Chặt chồng" else "Chặt",
                    )
                val event = GameEvent.Cut(PlayerId(seat), PlayerId(victim), chain.amount, chain.description)
                chain to event
            } else {
                state.cutChain to null
            }

        // Trường hợp 1: Chặn được người Báo Sâm -> Đền Sâm!
        if (state.samCaller != null && seat != state.samCaller) {
            val finalCutLines = buildCutLines(state.cutLines, newCutChain, state.betUnit)
            val nextState =
                state.copy(
                    hands = newHands,
                    top = combo,
                    topOwner = seat,
                    samBeaten = true,
                    winner = seat,
                    finished = true,
                    cutLines = finalCutLines,
                    cutChain = null,
                    playedCards = newPlayedCards,
                    hasPlayed = newHasPlayed,
                    moveCount = state.moveCount + 1,
                )
            val events =
                buildList {
                    add(GameEvent.Played(PlayerId(seat), cards, combo.description))
                    cutEvent?.let { add(it) }
                    add(GameEvent.Announced(PlayerId(state.samCaller), "Đền Sâm!"))
                    add(GameEvent.Settled(settle(nextState)))
                }
            return Transition(nextState, events)
        }

        // Trường hợp 2: Hết bài
        if (newHand.isEmpty()) {
            val finalCutLines = buildCutLines(state.cutLines, newCutChain, state.betUnit)

            // Kiểm tra Về bằng heo (thối 2 khi về)
            if (combo.hasTwo) {
                val nextState =
                    state.copy(
                        hands = newHands,
                        top = combo,
                        topOwner = seat,
                        finishWithTwoPlayer = seat,
                        finished = true,
                        cutLines = finalCutLines,
                        cutChain = null,
                        playedCards = newPlayedCards,
                        hasPlayed = newHasPlayed,
                        moveCount = state.moveCount + 1,
                    )
                val events =
                    buildList {
                        add(GameEvent.Played(PlayerId(seat), cards, combo.description))
                        cutEvent?.let { add(it) }
                        add(GameEvent.Announced(PlayerId(seat), "Về bằng heo → Thối 2!"))
                        add(GameEvent.Settled(settle(nextState)))
                    }
                return Transition(nextState, events)
            }

            // Về hợp lệ!
            val nextState =
                state.copy(
                    hands = newHands,
                    top = combo,
                    topOwner = seat,
                    winner = seat,
                    finished = true,
                    cutLines = finalCutLines,
                    cutChain = null,
                    violatorOfBao1 = violator,
                    playedCards = newPlayedCards,
                    hasPlayed = newHasPlayed,
                    moveCount = state.moveCount + 1,
                )
            val events =
                buildList {
                    add(GameEvent.Played(PlayerId(seat), cards, combo.description))
                    cutEvent?.let { add(it) }
                    if (state.samCaller == seat) {
                        add(GameEvent.Announced(PlayerId(seat), "Ăn Sâm!"))
                    } else {
                        add(GameEvent.Finished(PlayerId(seat), 1))
                    }
                    add(GameEvent.Settled(settle(nextState)))
                }
            return Transition(nextState, events)
        }

        // Ván chưa kết thúc: chuyển lượt
        val nextTurn =
            if (state.samCaller != null) {
                // Khi người Báo Sâm đánh, lần lượt các người khác có quyền chặn
                (seat + 1) % state.playerCount
            } else {
                nextActiveSeat(seat, state)
            }

        val nextState =
            state.copy(
                hands = newHands,
                top = combo,
                topOwner = seat,
                turn = nextTurn,
                passed = emptySet(),
                cutChain = newCutChain,
                baoMot = newBaoMot,
                violatorOfBao1 = violator,
                playedCards = newPlayedCards,
                hasPlayed = newHasPlayed,
                moveCount = state.moveCount + 1,
            )

        val events =
            buildList {
                add(GameEvent.Played(PlayerId(seat), cards, combo.description))
                cutEvent?.let { add(it) }
            }
        return Transition(nextState, events)
    }

    private fun handlePass(
        state: SamLocState,
        seat: Int,
    ): Transition<SamLocState> {
        val newPassed = state.passed + seat

        if (state.samCaller != null) {
            // Trong chế độ Báo Sâm: kiểm tra xem tất cả đối thủ khác đã bỏ qua chưa
            val otherSeats = (0 until state.playerCount).filter { it != state.samCaller && state.hands[it].isNotEmpty() }
            if (otherSeats.all { it in newPassed }) {
                // Không ai chặn được nước này của người Báo Sâm -> Người Báo Sâm được đi tiếp vòng mới
                val nextState =
                    state.copy(
                        turn = state.samCaller,
                        top = null,
                        topOwner = null,
                        passed = emptySet(),
                        moveCount = state.moveCount + 1,
                    )
                return Transition(
                    nextState,
                    listOf(
                        GameEvent.Passed(PlayerId(seat)),
                        GameEvent.RoundStarted(PlayerId(state.samCaller)),
                    ),
                )
            } else {
                val nextTurn = nextTurnExcluding(seat, newPassed, state)
                val nextState =
                    state.copy(
                        turn = nextTurn,
                        passed = newPassed,
                        moveCount = state.moveCount + 1,
                    )
                return Transition(nextState, listOf(GameEvent.Passed(PlayerId(seat))))
            }
        } else {
            // Chế độ chơi bình thường: kiểm tra vòng kết thúc
            val otherActive = state.activeSeats.filter { it != state.topOwner }
            if (otherActive.all { it in newPassed }) {
                // Hết vòng: chốt chuỗi chặt nếu có
                val finalCutLines = buildCutLines(state.cutLines, state.cutChain, state.betUnit)
                val leader = state.topOwner!!
                val nextState =
                    state.copy(
                        turn = leader,
                        top = null,
                        topOwner = null,
                        passed = emptySet(),
                        cutChain = null,
                        cutLines = finalCutLines,
                        moveCount = state.moveCount + 1,
                    )
                return Transition(
                    nextState,
                    listOf(
                        GameEvent.Passed(PlayerId(seat)),
                        GameEvent.RoundStarted(PlayerId(leader)),
                    ),
                )
            } else {
                val nextTurn = nextTurnExcluding(seat, newPassed, state)
                val nextState =
                    state.copy(
                        turn = nextTurn,
                        passed = newPassed,
                        moveCount = state.moveCount + 1,
                    )
                return Transition(nextState, listOf(GameEvent.Passed(PlayerId(seat))))
            }
        }
    }

    private fun buildCutLines(
        currentLines: List<SettlementLine>,
        chain: SlCutChain?,
        betUnit: Long,
    ): List<SettlementLine> {
        if (chain == null) return currentLines
        val lines = currentLines.toMutableList()
        lines +=
            SettlementLine(
                from = PlayerId(chain.victim),
                to = PlayerId(chain.cutter),
                amount = chain.amount,
                reason = "${chain.description}: −${chain.amount / betUnit} lá",
            )
        return lines
    }

    private fun nextActiveSeat(
        from: Int,
        state: SamLocState,
    ): Int {
        var s = (from + 1) % state.playerCount
        while (s != from) {
            if (state.hands[s].isNotEmpty()) return s
            s = (s + 1) % state.playerCount
        }
        return from
    }

    private fun nextTurnExcluding(
        from: Int,
        excluded: Set<Int>,
        state: SamLocState,
    ): Int {
        var s = (from + 1) % state.playerCount
        while (s != from) {
            if (state.hands[s].isNotEmpty() && s !in excluded) return s
            s = (s + 1) % state.playerCount
        }
        return from
    }

    override fun isFinished(state: SamLocState): Boolean = state.finished

    override fun settle(state: SamLocState): Settlement {
        require(state.finished) { "Ván chưa kết thúc, không thể thanh toán" }
        val allPlayers = (0 until state.playerCount).map { PlayerId(it) }
        val lines = mutableListOf<SettlementLine>()

        // 1. Kết thúc do Ăn trắng
        if (state.instantWin != null) {
            val winner = PlayerId(state.instantWin.player)
            val payout = state.rules.instantWinCards * state.betUnit
            for (p in allPlayers) {
                if (p != winner) {
                    lines +=
                        SettlementLine(
                            from = p,
                            to = winner,
                            amount = payout,
                            reason = "Thua Ăn trắng (${state.instantWin.type.viName}): −${state.rules.instantWinCards} lá",
                        )
                }
            }
            return Settlement.fromLines(allPlayers, lines)
        }

        // 2. Kết thúc do Đền Sâm
        if (state.samBeaten && state.samCaller != null) {
            val caller = PlayerId(state.samCaller)
            val penalty = state.rules.samFailPenaltyCards * state.betUnit
            for (p in allPlayers) {
                if (p != caller) {
                    lines +=
                        SettlementLine(
                            from = caller,
                            to = p,
                            amount = penalty,
                            reason = "Đền Sâm: −${state.rules.samFailPenaltyCards} lá",
                        )
                }
            }
            return Settlement.fromLines(allPlayers, lines)
        }

        // 3. Kết thúc do Ăn Sâm
        if (state.samCaller != null && state.winner == state.samCaller) {
            val winner = PlayerId(state.samCaller)
            val reward = state.rules.samWinCards * state.betUnit
            for (p in allPlayers) {
                if (p != winner) {
                    lines +=
                        SettlementLine(
                            from = p,
                            to = winner,
                            amount = reward,
                            reason = "Thua Ăn Sâm: −${state.rules.samWinCards} lá",
                        )
                }
            }
            return Settlement.fromLines(allPlayers, lines)
        }

        // 4. Kết thúc do Thối 2 khi về
        if (state.finishWithTwoPlayer != null) {
            val violator = PlayerId(state.finishWithTwoPlayer)
            val penalty = state.rules.finishWithTwoPenaltyCards * state.betUnit
            for (p in allPlayers) {
                if (p != violator) {
                    lines +=
                        SettlementLine(
                            from = violator,
                            to = p,
                            amount = penalty,
                            reason = "Thối 2 khi về: −${state.rules.finishWithTwoPenaltyCards} lá",
                        )
                }
            }
            return Settlement.fromLines(allPlayers, lines)
        }

        // 5. Kết thúc thông thường: có người về Nhất
        lines.addAll(state.cutLines)
        val winner = PlayerId(state.winner!!)

        // Tính tiền thua của từng người chơi
        data class LoserCalc(
            val seat: Int,
            val cards: Int,
            val reason: String,
        )
        val loserCalcs = mutableListOf<LoserCalc>()

        for (i in 0 until state.playerCount) {
            if (i == state.winner) continue
            val hand = state.hands[i]
            val hasPlayed = i in state.hasPlayed

            if (!hasPlayed) {
                // Cóng
                val penalty = SamLocPenalties.analyze(hand, state.rules)
                val penaltyCards = penalty.items.sumOf { SamLocPenalties.cardsForPenalty(it.kind, state.rules) }
                val totalCards = state.rules.congCards + penaltyCards
                val reasonDesc =
                    if (penaltyCards > 0) {
                        "Cóng (${state.rules.congCards} lá) + thối ($penaltyCards lá)"
                    } else {
                        "Cóng: −${state.rules.congCards} lá"
                    }
                loserCalcs += LoserCalc(i, totalCards, reasonDesc)
            } else {
                val penalty = SamLocPenalties.analyze(hand, state.rules)
                val penaltyCards = penalty.items.sumOf { SamLocPenalties.cardsForPenalty(it.kind, state.rules) }
                val normalCards = penalty.normalCards.size
                val totalCards = normalCards + penaltyCards
                val reasonDesc =
                    if (penaltyCards > 0) {
                        "$normalCards lá còn lại + thối ($penaltyCards lá)"
                    } else {
                        "$normalCards lá còn lại"
                    }
                loserCalcs += LoserCalc(i, totalCards, reasonDesc)
            }
        }

        // Kiểm tra xem có người phạm luật Đền Báo 1 không:
        // Nếu có người đền làng, người đó gánh toàn bộ tiền thua của các người khác trả cho người Nhất
        val violatorSeat = state.violatorOfBao1
        if (violatorSeat != null && violatorSeat != state.winner) {
            val violatorPlayer = PlayerId(violatorSeat)
            for (calc in loserCalcs) {
                val amount = calc.cards * state.betUnit
                if (calc.seat == violatorSeat) {
                    lines +=
                        SettlementLine(
                            from = violatorPlayer,
                            to = winner,
                            amount = amount,
                            reason = "Thua: −${calc.cards} lá (${calc.reason})",
                        )
                } else {
                    lines +=
                        SettlementLine(
                            from = violatorPlayer,
                            to = winner,
                            amount = amount,
                            reason = "Đền làng thay ghế ${calc.seat}: −${calc.cards} lá",
                        )
                }
            }
        } else {
            // Không có đền làng: ai thua người đó trả
            for (calc in loserCalcs) {
                val amount = calc.cards * state.betUnit
                lines +=
                    SettlementLine(
                        from = PlayerId(calc.seat),
                        to = winner,
                        amount = amount,
                        reason = "Thua: −${calc.cards} lá (${calc.reason})",
                    )
            }
        }

        return Settlement.fromLines(allPlayers, lines)
    }

    override fun viewOf(
        state: SamLocState,
        player: PlayerId,
    ): SamLocView {
        val seat = player.seat
        return SamLocView(
            me = seat,
            playerCount = state.playerCount,
            rules = state.rules,
            betUnit = state.betUnit,
            myHand = state.hands[seat],
            handCounts = state.hands.map { it.size },
            phase = state.phase,
            turn = state.turn,
            samCaller = state.samCaller,
            top = state.top,
            topOwner = state.topOwner,
            passed = state.passed,
            baoMot = state.baoMot,
            instantWin = state.instantWin,
            finishWithTwoPlayer = state.finishWithTwoPlayer,
            winner = state.winner,
            finished = state.finished,
            playedCards = state.playedCards,
        )
    }
}
