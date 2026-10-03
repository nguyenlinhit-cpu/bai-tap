package com.baiviet.game.poker.bot

import com.baiviet.core.ai.Bot
import com.baiviet.core.ai.BotLevel
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Deck
import com.baiviet.game.poker.engine.PokerAction
import com.baiviet.game.poker.engine.PokerStreet
import com.baiviet.game.poker.engine.PokerView
import com.baiviet.game.poker.rules.FastEvaluator
import com.baiviet.game.poker.rules.PokerHand
import com.baiviet.game.poker.rules.PokerHandType
import kotlin.random.Random

class PokerBot(private val random: Random = Random.Default) : Bot<PokerView, PokerAction> {

    override suspend fun decide(view: PokerView, legal: List<PokerAction>, level: BotLevel): PokerAction {
        require(legal.isNotEmpty()) { "Không có hành động hợp lệ cho Poker Bot" }
        if (legal.size == 1) return legal.first()

        val canCheck = legal.contains(PokerAction.Check)
        val canCall = legal.contains(PokerAction.Call)
        val betAction = legal.find { it is PokerAction.Bet }
        val raiseAction = legal.find { it is PokerAction.Raise }
        val allInAction = legal.find { it is PokerAction.AllIn }

        val equity = if (level == BotLevel.HARD) {
            calculateMonteCarloEquity(view.myHoleCards, view.communityCards, iterations = 600)
        } else {
            estimateHeuristicStrength(view.myHoleCards, view.communityCards, view.myHand)
        }

        // Tố láo (Bluff)
        val bluffProb = when (level) {
            BotLevel.EASY -> 0.0f
            BotLevel.NORMAL -> 0.08f
            BotLevel.HARD -> 0.15f
        }
        val isBluff = equity < 0.40f && random.nextFloat() < bluffProb
        if (isBluff) {
            if (betAction != null && random.nextFloat() < 0.6f) return betAction
            if (raiseAction != null && random.nextFloat() < 0.5f) return raiseAction
        }

        // Tỉ lệ chip cần theo trên tổng pot (Pot odds)
        val potOdds = if (view.toCall > 0) {
            view.toCall.toFloat() / (view.pot + view.toCall).coerceAtLeast(1L).toFloat()
        } else 0.0f

        return when {
            equity >= 0.80f -> {
                // Bài cực mạnh (Monster): Tố hoặc All-in
                if (raiseAction != null && random.nextFloat() < 0.75f) {
                    raiseAction
                } else if (betAction != null && random.nextFloat() < 0.85f) {
                    betAction
                } else if (allInAction != null && view.toCall > 0 && random.nextFloat() < 0.4f) {
                    allInAction
                } else if (canCall) {
                    PokerAction.Call
                } else if (canCheck) {
                    PokerAction.Check
                } else {
                    legal.first()
                }
            }
            equity >= 0.55f -> {
                // Bài mạnh (Top pair, Two pair, Good kicker): Cược hoặc Theo
                if (betAction != null && random.nextFloat() < 0.60f) {
                    betAction
                } else if (raiseAction != null && random.nextFloat() < 0.35f) {
                    raiseAction
                } else if (canCall) {
                    PokerAction.Call
                } else if (canCheck) {
                    PokerAction.Check
                } else {
                    legal.first()
                }
            }
            equity >= 0.35f || equity >= potOdds -> {
                // Bài trung bình hoặc có pot odds thuận lợi
                if (canCheck) {
                    PokerAction.Check
                } else if (canCall && view.toCall <= view.pot / 3) {
                    PokerAction.Call
                } else if (legal.contains(PokerAction.Fold)) {
                    PokerAction.Fold
                } else {
                    legal.first()
                }
            }
            else -> {
                // Bài yếu
                if (canCheck) {
                    PokerAction.Check
                } else if (legal.contains(PokerAction.Fold)) {
                    PokerAction.Fold
                } else {
                    legal.first()
                }
            }
        }
    }

    private fun estimateHeuristicStrength(
        holeCards: List<Card>,
        communityCards: List<Card>,
        hand: PokerHand?,
    ): Float {
        if (communityCards.isEmpty()) {
            // Đánh giá Preflop sơ bộ
            if (holeCards.size < 2) return 0.5f
            val r1 = PokerHand.rankToValue(holeCards[0].rank)
            val r2 = PokerHand.rankToValue(holeCards[1].rank)
            val isPair = r1 == r2
            val isSuited = holeCards[0].suit == holeCards[1].suit
            val high = maxOf(r1, r2)
            val low = minOf(r1, r2)

            return when {
                isPair && high >= 11 -> 0.88f // JJ+
                isPair && high >= 8 -> 0.72f // 88-TT
                isPair -> 0.60f // 22-77
                high == 14 && low >= 11 -> if (isSuited) 0.82f else 0.75f // AK, AQ, AJ
                high >= 12 && low >= 10 && isSuited -> 0.68f // KQs, QJs
                high == 14 -> if (isSuited) 0.65f else 0.55f // A-x
                isSuited && high - low <= 2 -> 0.52f // Suited connectors
                else -> 0.32f
            }
        }

        if (hand == null) return 0.3f
        return when (hand.type) {
            PokerHandType.ROYAL_FLUSH, PokerHandType.STRAIGHT_FLUSH -> 0.99f
            PokerHandType.FOUR_OF_A_KIND, PokerHandType.FULL_HOUSE -> 0.94f
            PokerHandType.FLUSH, PokerHandType.STRAIGHT -> 0.85f
            PokerHandType.THREE_OF_A_KIND -> 0.76f
            PokerHandType.TWO_PAIR -> 0.68f
            PokerHandType.ONE_PAIR -> {
                val pairRank = hand.tieBreakers.firstOrNull() ?: 0
                if (pairRank >= 11) 0.58f else 0.45f
            }
            PokerHandType.HIGH_CARD -> 0.25f
        }
    }

    fun calculateMonteCarloEquity(
        myHole: List<Card>,
        community: List<Card>,
        iterations: Int = 80,
    ): Float {
        if (myHole.size < 2) return 0.5f

        val knownCards = (myHole + community).toSet()
        val remainingDeck = Deck.FULL_DECK.filter { it !in knownCards }
        if (remainingDeck.size < 2) return 0.5f

        var wins = 0.0f
        val cardsToDraw = 5 - community.size

        for (i in 0 until iterations) {
            val shuffled = remainingDeck.shuffled(random)
            val oppHole = listOf(shuffled[0], shuffled[1])
            val runout = shuffled.subList(2, 2 + cardsToDraw)

            val fullBoard = community + runout
            val cmp = FastEvaluator.score(myHole + fullBoard).compareTo(FastEvaluator.score(oppHole + fullBoard))
            if (cmp > 0) {
                wins += 1.0f
            } else if (cmp == 0) {
                wins += 0.5f
            }
        }

        return wins / iterations
    }
}
