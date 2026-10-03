package com.baiviet.game.phom

import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.SettlementLine
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.phom.engine.PhomAction
import com.baiviet.game.phom.engine.PhomEngine
import com.baiviet.game.phom.engine.PhomPhase
import com.baiviet.game.phom.engine.PhomState
import com.baiviet.game.phom.rules.PhomMeld
import com.baiviet.game.phom.rules.PhomPlayerResult
import com.baiviet.game.phom.rules.PhomRules
import com.baiviet.game.phom.rules.PhomScoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhomEngineTest {

    private val rules = PhomRules()
    private val engine = PhomEngine()
    private val table = TableConfig(
        playerCount = 4,
        rules = rules,
        betUnit = 1000L,
        minBalanceMultiplier = rules.minBalanceMultiplier,
    )

    @Test
    fun `initial state deals 10 cards to leader and 9 cards to others`() {
        val state = engine.startGame(table, seed = 12345L, previousWinner = null)
        assertFalse(engine.isFinished(state))
        assertEquals(0, state.turn)
        assertEquals(PhomPhase.DISCARD, state.phase)
        assertEquals(10, state.hands[0].size)
        assertEquals(9, state.hands[1].size)
        assertEquals(9, state.hands[2].size)
        assertEquals(9, state.hands[3].size)
        assertEquals(15, state.stock.size) // 52 - 10 - 9*3 = 15
    }

    @Test
    fun `draw and discard flow advances players and turns`() {
        var state = engine.startGame(table, seed = 42L, previousWinner = null)
        val p0 = PlayerId(0)
        val p1 = PlayerId(1)

        val legalDiscardsP0 = engine.legalActions(state, p0)
        assertTrue(legalDiscardsP0.isNotEmpty())
        val firstDiscard = legalDiscardsP0.filterIsInstance<PhomAction.Discard>().first()

        state = engine.apply(state, p0, firstDiscard).state
        assertEquals(1, state.turn)
        assertEquals(PhomPhase.DRAW_OR_EAT, state.phase)
        assertEquals(firstDiscard.card, state.lastDiscard)

        // P1 draws
        val drawAction = PhomAction.Draw
        state = engine.apply(state, p1, drawAction).state
        assertEquals(10, state.hands[1].size)
        assertEquals(PhomPhase.DISCARD, state.phase)

        // P1 discards
        val legalDiscardsP1 = engine.legalActions(state, p1).filterIsInstance<PhomAction.Discard>()
        val p1Discard = legalDiscardsP1.first()
        state = engine.apply(state, p1, p1Discard).state

        assertEquals(2, state.turn)
        assertEquals(PhomPhase.DRAW_OR_EAT, state.phase)
    }

    @Test
    fun `eating advances player, tracks penalty and card`() {
        // Construct custom hands where P0 discards 8s and P1 has 8c 8d
        val handP0 = cards("8s 2c 3c 4c 5c 6c 7c 9c 10c Jc")
        val handP1 = cards("8c 8d 2d 3d 4d 5d 6d 7d 9d")
        val handP2 = cards("As 2s 3s 4s 5s 6s 7s 9s 10s")
        val handP3 = cards("Ah 2h 3h 4h 5h 6h 7h 9h 10h")

        var state = engine.startWithHands(
            rules = rules,
            playerCount = 4,
            betUnit = 1000L,
            hands = listOf(handP0, handP1, handP2, handP3),
            stock = listOf(c("Ks")),
            leader = 0,
        )

        // P0 discards 8s
        state = engine.apply(state, PlayerId(0), PhomAction.Discard(c("8s"))).state

        // P1 should be able to eat 8s
        val legalP1 = engine.legalActions(state, PlayerId(1))
        val eatAction = legalP1.filterIsInstance<PhomAction.Eat>().firstOrNull()
        assertNotNull(eatAction)
        assertTrue(eatAction!!.meldCards.contains(c("8s")))

        state = engine.apply(state, PlayerId(1), eatAction).state
        assertEquals(10, state.hands[1].size)
        assertTrue(state.hands[1].contains(c("8s")))
        assertTrue(state.eatenCards[1].contains(c("8s")))
        assertEquals(PhomPhase.DISCARD, state.phase)

        // Eat penalty should be recorded
        assertEquals(1, state.eatLines.size)
        assertEquals(PlayerId(0), state.eatLines[0].from)
        assertEquals(PlayerId(1), state.eatLines[0].to)
        assertEquals(1000L, state.eatLines[0].amount) // First eat is 1B = 1000
    }

    @Test
    fun `U ends the game instantly and calculates zero-sum payouts`() {
        // P1 has 2 melds and 2 cards of a 3rd meld, draws 3rd card -> U!
        val handP1 = cards("8s 8c 8d 4c 5c 6c Jd Qd 2h") // needs Kd to U with 2h as trash
        val stock = listOf(c("Kd"))

        var state = engine.startWithHands(
            rules = rules.copy(uKhanEnabled = false),
            playerCount = 4,
            betUnit = 1000L,
            hands = listOf(
                cards("2s 3s 4s 5s 6s 7s 9s 10s 4h"),
                handP1,
                cards("2c 3c 4c 5c 6c 7c 9c 10c 4d"),
                cards("2d 3d 4d 5d 6d 7d 9d 10d 5s"),
            ),
            stock = stock,
            leader = 0,
        )

        // P0 discards 4h
        state = engine.apply(state, PlayerId(0), PhomAction.Discard(c("4h"))).state

        // P1 draws Kd
        state = engine.apply(state, PlayerId(1), PhomAction.Draw).state

        // Hand is now 8s 8c 8d, 4c 5c 6c, Jd Qd Kd, plus 2h -> 3 melds = U!
        // Should be in DISCARD phase
        val actions = engine.legalActions(state, PlayerId(1))
        val declareU = actions.filterIsInstance<PhomAction.DeclareU>().firstOrNull()
        assertNotNull(declareU)

        state = engine.apply(state, PlayerId(1), declareU!!).state

        assertTrue(state.finished)
        assertEquals(1, state.winner)
        assertTrue(state.isU)

        val settlement = engine.settle(state)
        val totalDelta = settlement.deltas.values.sum()
        assertEquals(0L, totalDelta)
        // P1 wins 5B from each of the other 3 players: 5 * 1000 * 3 = 15000
        assertEquals(15000L, settlement.deltas[PlayerId(1)])
        assertEquals(-5000L, settlement.deltas[PlayerId(0)])
        assertEquals(-5000L, settlement.deltas[PlayerId(2)])
        assertEquals(-5000L, settlement.deltas[PlayerId(3)])
    }

    @Test
    fun `U Den when player is eaten 3 times`() {
        // Player 0 gets eaten 3 times by Player 1, then Player 1 achieves U
        val eatLines = listOf(
            SettlementLine(PlayerId(0), PlayerId(1), 1000L, "Ăn bài 1"),
            SettlementLine(PlayerId(0), PlayerId(1), 2000L, "Ăn bài 2"),
            SettlementLine(PlayerId(0), PlayerId(1), 3000L, "Ăn bài 3"),
        )

        val handP1 = cards("8s 9s 10s 4c 5c 6c Jd Qd Kd")
        val matrix = listOf(
            listOf(0, 3, 0, 0), // P0 was eaten 3 times by P1
            listOf(0, 0, 0, 0),
            listOf(0, 0, 0, 0),
            listOf(0, 0, 0, 0),
        )

        var state = PhomState(
            rules = rules,
            playerCount = 4,
            betUnit = 1000L,
            seed = 0L,
            isFirstGame = true,
            leader = 0,
            turn = 1,
            phase = PhomPhase.DISCARD,
            hands = listOf(
                cards("2s 3s 4s 5s 6s 7s 9s 10s"),
                handP1,
                cards("2c 3c 4c 5c 6c 7c 9c 10c"),
                cards("2d 3d 4d 5d 6d 7d 9d 10d"),
            ),
            stock = listOf(c("As")),
            eatLines = eatLines,
            eatCountMatrix = matrix,
        )

        // P1 declares U
        val declareU = PhomAction.DeclareU
        state = engine.apply(state, PlayerId(1), declareU).state
        assertTrue(state.finished)
        assertTrue(state.isU)

        val settlement = engine.settle(state)
        // Zero-sum check
        val totalDelta = settlement.deltas.values.sum()
        assertEquals(0L, totalDelta)

        // P0 pays Ù đền for whole table: 3 * 5000 = 15000, plus eat penalties (1000+2000+3000 = 6000)
        // Total P0 = -15000 - 6000 = -21000
        // P1 = +15000 + 6000 = +21000
        // P2 and P3 pay 0 because P0 compensated for them!
        assertEquals(-21000L, settlement.deltas[PlayerId(0)])
        assertEquals(21000L, settlement.deltas[PlayerId(1)])
        assertEquals(0L, settlement.deltas[PlayerId(2)])
        assertEquals(0L, settlement.deltas[PlayerId(3)])
    }

    @Test
    fun `Normal end game calculates ranking, tie-breaking and zero-sum payouts`() {
        // Melds: P0 has meld, P1 has meld, P2 has meld, P3 is Mom
        val meldP0 = PhomMeld.classify(cards("8s 8c 8d"))!!
        val meldP1 = PhomMeld.classify(cards("4c 5c 6c"))!!
        val meldP2 = PhomMeld.classify(cards("Jd Qd Kd"))!!

        // Trash:
        // P0: 2h + 3h = 5 pts
        // P1: 2s + 3s = 5 pts (same pts as P0, but melded after P0)
        // P2: 9s + 10s = 19 pts
        // P3: Mom (chay) -> always 4th
        val hands = listOf(
            cards("2h 3h"),
            cards("2s 3s"),
            cards("9s 10s"),
            cards("2d 3d 4s 5h 6h 7h 8h 9h 10h"),
        )

        val exposedMelds: List<List<PhomMeld>> = listOf(
            listOf(meldP0),
            listOf(meldP1),
            listOf(meldP2),
            emptyList<PhomMeld>(),
        )

        val state = PhomState(
            rules = rules,
            playerCount = 4,
            betUnit = 1000L,
            seed = 0L,
            isFirstGame = true,
            leader = 0,
            turn = 3,
            phase = PhomPhase.DISCARD,
            hands = hands,
            stock = emptyList(),
            exposedMelds = exposedMelds,
            playerMeldOrder = listOf(0, 1, 2, null),
            finished = true,
        )

        val ranked = engine.rankEndOfGame(state)
        // P0 (5 pts, meldOrder 0) should be Rank 1 (Nhất)
        // P1 (5 pts, meldOrder 1) should be Rank 2 (Nhì)
        // P2 (19 pts) should be Rank 3 (Ba)
        // P3 (Móm) should be Rank 4 (Bét)
        assertEquals(0, ranked[0])
        assertEquals(1, ranked[1])
        assertEquals(2, ranked[2])
        assertEquals(3, ranked[3])

        // Settlement and Zero-sum verification
        val settlement = engine.settle(state.copy(winner = ranked.first()))
        assertEquals(0L, settlement.deltas.values.sum())
    }
}
