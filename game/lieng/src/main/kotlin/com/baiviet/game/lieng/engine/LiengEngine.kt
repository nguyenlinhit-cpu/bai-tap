package com.baiviet.game.lieng.engine

import com.baiviet.core.cards.Deck
import com.baiviet.core.engine.GameEngine
import com.baiviet.core.engine.GameEvent
import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.TableConfig
import com.baiviet.core.engine.Transition
import com.baiviet.game.lieng.rules.LiengEvaluator
import com.baiviet.game.lieng.rules.LiengRules
import com.baiviet.game.lieng.rules.LiengScoring

/**
 * Engine Liêng.
 *
 * - Mỗi người bỏ cược sàn B rồi nhận 3 lá úp.
 * - Tối đa [LiengRules.maxBettingRounds] vòng tố. Mỗi lượt: Xem/Theo, Tố (thêm tối thiểu B, tối đa bằng pot),
 *   Úp, Tất tay. Mỗi vòng tối đa [LiengRules.maxRaisesPerRound] lần tố.
 * - Vòng kết thúc khi mọi người còn cược được đã hành động và cược bằng nhau.
 * - Hết vòng cuối hoặc chỉ còn ≤ 1 người còn cược được → lật bài; chỉ còn 1 người chưa úp → người đó ăn.
 * - Tiền thật sự bị giới hạn bởi stack (xu mang vào ván) → có side pot khi tất tay.
 */
class LiengEngine : GameEngine<LiengState, LiengAction, LiengView, LiengRules> {

    override fun start(table: TableConfig<LiengRules>, seed: Long): LiengState =
        startHand(table, seed, List(table.playerCount) { table.betUnit * table.rules.stackMultiplier }, firstSeat = 0)

    /**
     * Bắt đầu ván với stack cho trước.
     *
     * @param stacks số xu mỗi ghế mang vào ván (đã chặn theo số dư)
     * @param firstSeat ghế mở vòng tố
     */
    fun startHand(table: TableConfig<LiengRules>, seed: Long, stacks: List<Long>, firstSeat: Int): LiengState {
        require(stacks.size == table.playerCount)
        val players = List(table.playerCount) { PlayerId(it) }
        val (_, deck) = Deck.shuffled(seed)
        val (hands, _) = Deck.deal(deck, table.playerCount, 3)
        val playerStates = players.associateWith { pid ->
            val stack = stacks[pid.seat].coerceAtLeast(0L)
            val ante = minOf(stack, table.betUnit)
            LiengPlayerState(id = pid, cards = hands[pid.seat], invested = ante, stack = stack - ante)
        }
        val base = LiengState(
            players = players,
            phase = LiengPhase.BETTING,
            playerStates = playerStates,
            minRaise = table.betUnit,
            initialMinRaise = table.betUnit,
            firstSeat = firstSeat,
            rules = table.rules,
        )
        val first = firstEligible(base)
        return if (first == null || eligible(base).size <= 1) showdown(base) else base.copy(currentActor = first)
    }

    override fun currentActors(state: LiengState): Set<PlayerId> =
        if (state.phase == LiengPhase.FINISHED) emptySet() else setOfNotNull(state.currentActor)

    override fun legalActions(state: LiengState, player: PlayerId): List<LiengAction> {
        if (state.phase == LiengPhase.FINISHED || state.currentActor != player) return emptyList()
        val p = state.playerStates[player] ?: return emptyList()
        if (p.isFolded || p.stack == 0L) return emptyList()

        val toCall = state.currentHighBet - p.currentRoundBet
        val actions = mutableListOf<LiengAction>(LiengAction.Fold)
        if (toCall <= 0L) actions += LiengAction.Check
        if (toCall > 0L && p.stack > toCall) actions += LiengAction.Call
        raiseAmounts(state, p).forEach { actions += LiengAction.Raise(it) }
        if (state.raisesThisRound < state.rules.maxRaisesPerRound || p.stack <= toCall) actions += LiengAction.AllIn
        return actions
    }

    /** Các mức tố gợi ý (thêm so với mức cao nhất): B, 2B, ½ pot, pot — trong giới hạn stack. */
    private fun raiseAmounts(state: LiengState, p: LiengPlayerState): List<Long> {
        if (state.raisesThisRound >= state.rules.maxRaisesPerRound) return emptyList()
        val max = maxRaise(state, p)
        val min = state.initialMinRaise
        if (max < min) return emptyList()
        return listOf(min, min * 2, state.pot / 2, state.pot)
            .map { it.coerceIn(min, max) }
            .distinct()
            .sorted()
    }

    /** Tố tối đa: bằng pot hiện tại, và phải còn lại xu (tố hết xu là Tất tay). */
    private fun maxRaise(state: LiengState, p: LiengPlayerState): Long {
        val toCall = (state.currentHighBet - p.currentRoundBet).coerceAtLeast(0L)
        return minOf(state.pot, p.stack - toCall - 1)
    }

    override fun apply(state: LiengState, player: PlayerId, action: LiengAction): Transition<LiengState> {
        if (state.phase == LiengPhase.FINISHED) throw IllegalActionException("Ván đã kết thúc")
        if (state.currentActor != player) throw IllegalActionException("Chưa đến lượt $player")
        val legal = legalActions(state, player)
        val p = state.playerStates.getValue(player)
        val toCall = (state.currentHighBet - p.currentRoundBet).coerceAtLeast(0L)
        val ok = when (action) {
            is LiengAction.Raise -> legal.any { it is LiengAction.Raise } &&
                action.amount in state.initialMinRaise..maxRaise(state, p)
            else -> action in legal
        }
        if (!ok) throw IllegalActionException("Hành động $action không hợp lệ cho $player")

        var high = state.currentHighBet
        var acted = state.actedThisRound + player
        var raises = state.raisesThisRound
        var lastBettor = state.lastBettor
        val events = mutableListOf<GameEvent>()

        fun put(amount: Long): LiengPlayerState =
            p.copy(stack = p.stack - amount, invested = p.invested + amount, currentRoundBet = p.currentRoundBet + amount)

        val updated: LiengPlayerState = when (action) {
            LiengAction.Fold -> p.copy(isFolded = true).also { events += GameEvent.Bet(player, 0, "Úp") }
            LiengAction.Check -> p.also { events += GameEvent.Bet(player, 0, "Xem") }
            LiengAction.Call -> put(toCall).also { events += GameEvent.Bet(player, toCall, "Theo") }
            is LiengAction.Raise -> {
                high += action.amount
                raises++
                acted = setOf(player)
                lastBettor = player
                events += GameEvent.Bet(player, toCall + action.amount, "Tố")
                put(toCall + action.amount)
            }
            LiengAction.AllIn -> {
                val newTotal = p.currentRoundBet + p.stack
                if (newTotal > high) {
                    high = newTotal
                    raises++
                    acted = setOf(player)
                    lastBettor = player
                }
                events += GameEvent.Bet(player, p.stack, "Tất tay")
                put(p.stack)
            }
        }

        var s = state.copy(
            playerStates = state.playerStates + (player to updated),
            currentHighBet = high,
            actedThisRound = acted,
            raisesThisRound = raises,
            lastBettor = lastBettor,
        )

        // Chỉ còn 1 người chưa úp → thắng luôn
        if (s.players.count { !s.playerStates.getValue(it).isFolded } == 1) return Transition(showdown(s), events)

        val canBet = eligible(s)
        val roundDone = canBet.all { it in s.actedThisRound && s.playerStates.getValue(it).currentRoundBet >= s.currentHighBet }
        if (roundDone) {
            val stillContested = canBet.size >= 2
            if (s.currentRound >= s.rules.maxBettingRounds || !stillContested) return Transition(showdown(s), events)
            s = s.copy(
                currentRound = s.currentRound + 1,
                currentHighBet = 0L,
                actedThisRound = emptySet(),
                raisesThisRound = 0,
                lastBettor = null,
                playerStates = s.playerStates.mapValues { (_, ps) -> ps.copy(currentRoundBet = 0L) },
            )
            return Transition(s.copy(currentActor = firstEligible(s)), events)
        }

        // Người kế tiếp còn phải hành động
        val idx = s.players.indexOf(player)
        val next = (1..s.players.size).map { s.players[(idx + it) % s.players.size] }.first { pid ->
            pid in canBet && (pid !in s.actedThisRound || s.playerStates.getValue(pid).currentRoundBet < s.currentHighBet)
        }
        return Transition(s.copy(currentActor = next), events)
    }

    /** Người còn cược được: chưa úp và còn xu. */
    private fun eligible(s: LiengState): List<PlayerId> =
        s.players.filter { val ps = s.playerStates.getValue(it); !ps.isFolded && ps.stack > 0 }

    private fun firstEligible(s: LiengState): PlayerId? {
        val canBet = eligible(s)
        return (0 until s.players.size).map { s.players[(s.firstSeat + it) % s.players.size] }.firstOrNull { it in canBet }
    }

    private fun showdown(s: LiengState): LiengState {
        val (_, results) = computeSettlement(s)
        return s.copy(phase = LiengPhase.FINISHED, currentActor = null, results = results)
    }

    private fun computeSettlement(s: LiengState) = LiengScoring.computeSettlement(
        players = s.players,
        invested = s.playerStates.mapValues { it.value.invested },
        folded = s.playerStates.filter { it.value.isFolded }.keys,
        hands = s.playerStates.mapValues { LiengEvaluator.evaluate(it.value.cards, s.rules) },
        rules = s.rules,
        firstSeat = s.firstSeat,
    )

    override fun isFinished(state: LiengState): Boolean = state.phase == LiengPhase.FINISHED

    override fun settle(state: LiengState): Settlement {
        check(isFinished(state)) { "Chỉ được settle khi ván đã kết thúc" }
        return computeSettlement(state).first
    }

    /** Thoát giữa ván = úp bài: mất số xu đã bỏ vào pot. */
    fun forfeit(state: LiengState, player: PlayerId): Settlement {
        if (isFinished(state)) return settle(state)
        val folded = state.copy(playerStates = state.playerStates + (player to state.playerStates.getValue(player).copy(isFolded = true)))
        val others = folded.players.filter { !folded.playerStates.getValue(it).isFolded }
        // Phần còn lại tiếp tục coi như lật bài ngay giữa những người chưa úp
        return computeSettlement(if (others.isEmpty()) state else folded).first
    }

    override fun viewOf(state: LiengState, player: PlayerId): LiengView {
        val me = state.playerStates[player] ?: error("Người chơi $player không có trong bàn")
        val finished = state.phase == LiengPhase.FINISHED
        val infos = state.players.map { pid ->
            val ps = state.playerStates.getValue(pid)
            val canSee = pid == player || (finished && !ps.isFolded)
            LiengPlayerInfo(
                id = pid,
                cardCount = ps.cards.size,
                cards = if (canSee) ps.cards else null,
                hand = if (canSee) LiengEvaluator.evaluate(ps.cards, state.rules) else null,
                invested = ps.invested,
                currentRoundBet = ps.currentRoundBet,
                isFolded = ps.isFolded,
                isAllIn = ps.isAllIn,
                stack = ps.stack,
            )
        }
        return LiengView(
            myId = player,
            currentActor = state.currentActor,
            phase = state.phase,
            myHand = LiengEvaluator.evaluate(me.cards, state.rules),
            players = infos,
            pot = state.pot,
            currentHighBet = state.currentHighBet,
            toCall = (state.currentHighBet - me.currentRoundBet).coerceAtLeast(0L),
            minRaise = state.initialMinRaise,
            currentRound = state.currentRound,
            rules = state.rules,
            results = state.results,
            myStack = me.stack,
            raisesThisRound = state.raisesThisRound,
        )
    }
}
