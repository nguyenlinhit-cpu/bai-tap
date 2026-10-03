package com.baiviet.game.maubinh.engine

import com.baiviet.core.cards.Deck
import com.baiviet.core.engine.GameEngine
import com.baiviet.core.engine.GameEvent
import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.TableConfig
import com.baiviet.core.engine.Transition
import com.baiviet.game.maubinh.rules.MauBinhEvaluator
import com.baiviet.game.maubinh.rules.MauBinhHandOptimizer
import com.baiviet.game.maubinh.rules.MauBinhPairwiseComparison
import com.baiviet.game.maubinh.rules.MauBinhPlayerHand
import com.baiviet.game.maubinh.rules.MauBinhRules
import com.baiviet.game.maubinh.rules.MauBinhScoring

/**
 * Game Engine luật cho Mậu Binh (Binh Xập Xám).
 * Tuân thủ hợp đồng GameEngine thuần Kotlin, bất biến.
 */
class MauBinhEngine : GameEngine<MauBinhState, MauBinhAction, MauBinhView, MauBinhRules> {

    override fun start(table: TableConfig<MauBinhRules>, seed: Long): MauBinhState {
        val (_, shuffled) = Deck.shuffled(seed)
        val (hands, _) = Deck.deal(shuffled, playerCount = table.playerCount, cardsPerPlayer = 13)

        val players = hands.mapIndexed { idx, cards ->
            MauBinhPlayerState(
                playerId = PlayerId(idx),
                initialCards = cards,
                instantWinDeclared = null,
                arrangement = null,
                isSubmitted = false,
            )
        }

        return MauBinhState(
            tableConfig = table,
            seed = seed,
            phase = MauBinhPhase.ARRANGING,
            revealStep = MauBinhRevealStep.NOT_STARTED,
            players = players,
            pairwiseComparisons = emptyList(),
            finalSettlement = null,
        )
    }

    override fun currentActors(state: MauBinhState): Set<PlayerId> {
        return when (state.phase) {
            MauBinhPhase.ARRANGING -> {
                state.players.filter { !it.isSubmitted }.map { it.playerId }.toSet()
            }
            MauBinhPhase.REVEALING -> {
                setOf(state.players.first().playerId)
            }
            MauBinhPhase.FINISHED -> emptySet()
        }
    }

    override fun legalActions(state: MauBinhState, player: PlayerId): List<MauBinhAction> {
        if (player !in currentActors(state)) return emptyList()

        return when (state.phase) {
            MauBinhPhase.ARRANGING -> {
                val pState = state.players.firstOrNull { it.playerId == player } ?: return emptyList()
                val actions = mutableListOf<MauBinhAction>()
                actions.add(MauBinhAction.AutoArrange)
                actions.add(MauBinhAction.Timeout)

                val iw = MauBinhEvaluator.detectInstantWin(pState.initialCards)
                if (iw != null) {
                    actions.add(MauBinhAction.DeclareInstantWin(iw))
                }

                // Gợi ý phương án tối ưu làm ví dụ hợp lệ cho SubmitArrangement
                val opt = MauBinhHandOptimizer.findBestArrangement(pState.initialCards, state.tableConfig.rules)
                actions.add(MauBinhAction.SubmitArrangement(opt.arrangement))

                actions
            }
            MauBinhPhase.REVEALING -> listOf(MauBinhAction.NextRevealStep)
            MauBinhPhase.FINISHED -> emptyList()
        }
    }

    override fun apply(state: MauBinhState, player: PlayerId, action: MauBinhAction): Transition<MauBinhState> {
        val actors = currentActors(state)
        if (player !in actors) {
            throw IllegalActionException("Người chơi $player không có quyền hành động ở thời điểm hiện tại")
        }

        val events = mutableListOf<GameEvent>()

        return when (state.phase) {
            MauBinhPhase.ARRANGING -> {
                val pIdx = state.players.indexOfFirst { it.playerId == player }
                val pState = state.players[pIdx]
                val updatedPlayer: MauBinhPlayerState

                when (action) {
                    is MauBinhAction.DeclareInstantWin -> {
                        val iw = MauBinhEvaluator.detectInstantWin(pState.initialCards)
                        if (iw == null || iw != action.type) {
                            throw IllegalActionException("Bài của người chơi không đủ điều kiện tới trắng ${action.type.viName}")
                        }
                        updatedPlayer = pState.copy(
                            instantWinDeclared = action.type,
                            isSubmitted = true,
                        )
                        events.add(GameEvent.Announced(player, "Báo Mậu binh: ${action.type.viName}"))
                    }
                    is MauBinhAction.SubmitArrangement -> {
                        val allSubmitted = action.arrangement.chi1 + action.arrangement.chi2 + action.arrangement.chi3
                        if (allSubmitted.size != 13 || allSubmitted.toSet() != pState.initialCards.toSet()) {
                            throw IllegalActionException("Bài nộp không khớp với 13 lá được chia")
                        }
                        updatedPlayer = pState.copy(
                            arrangement = action.arrangement,
                            isSubmitted = true,
                        )
                        events.add(GameEvent.Melded(player, "Đã xếp xong bài"))
                    }
                    is MauBinhAction.AutoArrange, is MauBinhAction.Timeout -> {
                        val opt = MauBinhHandOptimizer.findBestArrangement(pState.initialCards, state.tableConfig.rules)
                        if (opt.instantWinType != null) {
                            updatedPlayer = pState.copy(
                                instantWinDeclared = opt.instantWinType,
                                arrangement = opt.arrangement,
                                isSubmitted = true,
                            )
                            events.add(GameEvent.Announced(player, "Báo Mậu binh: ${opt.instantWinType.viName}"))
                        } else {
                            updatedPlayer = pState.copy(
                                arrangement = opt.arrangement,
                                isSubmitted = true,
                            )
                            val desc = if (action is MauBinhAction.Timeout) "Hết giờ, tự động xếp bài" else "Tự động xếp bài tối ưu"
                            events.add(GameEvent.Melded(player, desc))
                        }
                    }
                    else -> throw IllegalActionException("Hành động không hợp lệ trong giai đoạn xếp bài")
                }

                val newPlayers = state.players.toMutableList()
                newPlayers[pIdx] = updatedPlayer

                val allSubmitted = newPlayers.all { it.isSubmitted }
                if (allSubmitted) {
                    // Tất cả người chơi đã nộp bài -> Tính điểm toàn bộ & chuyển sang REVEALING
                    val settlement = computeSettlement(newPlayers, state.tableConfig.rules, state.tableConfig.betUnit)
                    val comparisons = computePairwiseComparisons(newPlayers, state.tableConfig.rules)

                    events.add(GameEvent.Revealed(player, "Tất cả đã xếp xong. Bắt đầu so Chi 1!"))

                    Transition(
                        state = state.copy(
                            players = newPlayers,
                            phase = MauBinhPhase.REVEALING,
                            revealStep = MauBinhRevealStep.CHI_1,
                            pairwiseComparisons = comparisons,
                            finalSettlement = settlement,
                        ),
                        events = events,
                    )
                } else {
                    Transition(
                        state = state.copy(players = newPlayers),
                        events = events,
                    )
                }
            }
            MauBinhPhase.REVEALING -> {
                if (action !is MauBinhAction.NextRevealStep) {
                    throw IllegalActionException("Chỉ chấp nhận NextRevealStep trong giai đoạn so bài")
                }

                when (state.revealStep) {
                    MauBinhRevealStep.NOT_STARTED, MauBinhRevealStep.CHI_1 -> {
                        events.add(GameEvent.Revealed(player, "So Chi 2"))
                        Transition(
                            state = state.copy(revealStep = MauBinhRevealStep.CHI_2),
                            events = events,
                        )
                    }
                    MauBinhRevealStep.CHI_2 -> {
                        events.add(GameEvent.Revealed(player, "So Chi 3"))
                        Transition(
                            state = state.copy(revealStep = MauBinhRevealStep.CHI_3),
                            events = events,
                        )
                    }
                    MauBinhRevealStep.CHI_3 -> {
                        events.add(GameEvent.Revealed(player, "Tổng kết các chi"))
                        Transition(
                            state = state.copy(revealStep = MauBinhRevealStep.SUMMARY),
                            events = events,
                        )
                    }
                    MauBinhRevealStep.SUMMARY -> {
                        val settlement = state.finalSettlement ?: computeSettlement(
                            state.players,
                            state.tableConfig.rules,
                            state.tableConfig.betUnit,
                        )
                        events.add(GameEvent.Settled(settlement))
                        Transition(
                            state = state.copy(
                                phase = MauBinhPhase.FINISHED,
                                finalSettlement = settlement,
                            ),
                            events = events,
                        )
                    }
                }
            }
            MauBinhPhase.FINISHED -> throw IllegalActionException("Ván bài đã kết thúc")
        }
    }

    override fun isFinished(state: MauBinhState): Boolean {
        return state.phase == MauBinhPhase.FINISHED
    }

    override fun settle(state: MauBinhState): Settlement {
        return state.finalSettlement ?: computeSettlement(
            state.players,
            state.tableConfig.rules,
            state.tableConfig.betUnit,
        )
    }

    override fun viewOf(state: MauBinhState, player: PlayerId): MauBinhView {
        val me = state.players.first { it.playerId == player }
        val otherPlayers = state.players.filter { it.playerId != player }.map { other ->
            when (state.phase) {
                MauBinhPhase.ARRANGING -> {
                    MauBinhOtherPlayerView(
                        playerId = other.playerId,
                        cardCount = other.initialCards.size,
                        isSubmitted = other.isSubmitted,
                        instantWinDeclared = other.instantWinDeclared,
                        visibleChi1 = null,
                        visibleChi2 = null,
                        visibleChi3 = null,
                    )
                }
                MauBinhPhase.REVEALING -> {
                    MauBinhOtherPlayerView(
                        playerId = other.playerId,
                        cardCount = other.initialCards.size,
                        isSubmitted = other.isSubmitted,
                        instantWinDeclared = other.instantWinDeclared,
                        visibleChi1 = if (state.revealStep >= MauBinhRevealStep.CHI_1) other.arrangement?.chi1 else null,
                        visibleChi2 = if (state.revealStep >= MauBinhRevealStep.CHI_2) other.arrangement?.chi2 else null,
                        visibleChi3 = if (state.revealStep >= MauBinhRevealStep.CHI_3) other.arrangement?.chi3 else null,
                    )
                }
                MauBinhPhase.FINISHED -> {
                    MauBinhOtherPlayerView(
                        playerId = other.playerId,
                        cardCount = other.initialCards.size,
                        isSubmitted = other.isSubmitted,
                        instantWinDeclared = other.instantWinDeclared,
                        visibleChi1 = other.arrangement?.chi1,
                        visibleChi2 = other.arrangement?.chi2,
                        visibleChi3 = other.arrangement?.chi3,
                    )
                }
            }
        }

        return MauBinhView(
            myPlayerId = player,
            phase = state.phase,
            revealStep = state.revealStep,
            myCards = me.initialCards,
            myArrangement = me.arrangement,
            myInstantWin = me.instantWinDeclared,
            isMySubmitted = me.isSubmitted,
            otherPlayers = otherPlayers,
            pairwiseComparisons = state.pairwiseComparisons,
            settlement = state.finalSettlement,
            rules = state.tableConfig.rules,
        )
    }

    private fun computeSettlement(
        players: List<MauBinhPlayerState>,
        rules: MauBinhRules,
        betUnit: Long,
    ): Settlement {
        val hands = players.map { p ->
            MauBinhPlayerHand(
                playerId = p.playerId,
                cards = p.initialCards,
                instantWinType = p.instantWinDeclared,
                arrangement = p.arrangement,
            )
        }
        return MauBinhScoring.settle(hands, rules, betUnit)
    }

    private fun computePairwiseComparisons(
        players: List<MauBinhPlayerState>,
        rules: MauBinhRules,
    ): List<MauBinhPairwiseComparison> {
        val hands = players.map { p ->
            MauBinhPlayerHand(
                playerId = p.playerId,
                cards = p.initialCards,
                instantWinType = p.instantWinDeclared,
                arrangement = p.arrangement,
            )
        }
        val evaluatedHands = hands.associateWith { hand ->
            if (hand.instantWinType != null) false
            else hand.arrangement?.isFoul(rules) ?: true
        }

        val potentialSap3ChiCounts = mutableMapOf<PlayerId, Int>()
        for (i in hands.indices) {
            val p1 = hands[i]
            if (p1.instantWinType != null || evaluatedHands[p1] == true) continue
            var sweepCount = 0
            for (j in hands.indices) {
                if (i == j) continue
                val p2 = hands[j]
                if (p2.instantWinType != null) continue
                if (evaluatedHands[p2] == true) {
                    sweepCount++
                } else {
                    val p1Arr = p1.arrangement!!
                    val p2Arr = p2.arrangement!!
                    val h1_1 = MauBinhEvaluator.evaluateChi1OrChi2(p1Arr.chi1, rules)
                    val h1_2 = MauBinhEvaluator.evaluateChi1OrChi2(p2Arr.chi1, rules)
                    val h2_1 = MauBinhEvaluator.evaluateChi1OrChi2(p1Arr.chi2, rules)
                    val h2_2 = MauBinhEvaluator.evaluateChi1OrChi2(p2Arr.chi2, rules)
                    val h3_1 = MauBinhEvaluator.evaluateChi3(p1Arr.chi3)
                    val h3_2 = MauBinhEvaluator.evaluateChi3(p2Arr.chi3)
                    if (h1_1 > h1_2 && h2_1 > h2_2 && h3_1 > h3_2) {
                        sweepCount++
                    }
                }
            }
            potentialSap3ChiCounts[p1.playerId] = sweepCount
        }

        val totalOpponents = hands.size - 1
        val sapLangPlayers = potentialSap3ChiCounts.filter { (_, count) ->
            hands.size >= 3 && count == totalOpponents && rules.sapLangMultiplier
        }.keys

        val pairs = mutableListOf<MauBinhPairwiseComparison>()
        for (i in hands.indices) {
            for (j in (i + 1) until hands.size) {
                val h1 = hands[i]
                val h2 = hands[j]
                pairs.add(
                    MauBinhScoring.comparePair(
                        hand1 = h1,
                        hand2 = h2,
                        isP1Foul = evaluatedHands[h1] == true,
                        isP2Foul = evaluatedHands[h2] == true,
                        isP1SapLang = sapLangPlayers.contains(h1.playerId),
                        isP2SapLang = sapLangPlayers.contains(h2.playerId),
                        rules = rules,
                    ),
                )
            }
        }
        return pairs
    }
}
