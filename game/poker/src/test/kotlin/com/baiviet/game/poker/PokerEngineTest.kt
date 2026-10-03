package com.baiviet.game.poker

import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.poker.engine.PokerAction
import com.baiviet.game.poker.engine.PokerEngine
import com.baiviet.game.poker.engine.PokerStreet
import com.baiviet.game.poker.rules.PokerRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PokerEngineTest {

    private val engine = PokerEngine()
    private val config = TableConfig(playerCount = 4, rules = PokerRules(startingChipsBB = 100), betUnit = 100L)

    @Test
    fun testStartPostsBlindsCorrectly() {
        val state = engine.start(config, 12345L)

        assertEquals(4, state.players.size)
        assertEquals(PokerStreet.PREFLOP, state.street)
        assertEquals(0, state.dealerIndex)

        // SB is player 1, BB is player 2
        val sb = state.playerStates[PlayerId(1)]!!
        val bb = state.playerStates[PlayerId(2)]!!

        assertEquals(50L, sb.currentStreetBet)
        assertEquals(50L, sb.investedThisHand)
        assertEquals(9950L, sb.stack)

        assertEquals(100L, bb.currentStreetBet)
        assertEquals(100L, bb.investedThisHand)
        assertEquals(9900L, bb.stack)

        assertEquals(100L, state.currentStreetHighBet)
        // First to act is UTG (seat 3)
        assertEquals(PlayerId(3), state.currentActor)
    }

    @Test
    fun testPreflopAllFoldToBB() {
        var state = engine.start(config, 12345L)

        // Player 3 folds
        state = engine.apply(state, PlayerId(3), PokerAction.Fold).state
        assertEquals(PlayerId(0), state.currentActor)

        // Player 0 folds
        state = engine.apply(state, PlayerId(0), PokerAction.Fold).state
        assertEquals(PlayerId(1), state.currentActor)

        // Player 1 (SB) folds -> Only Player 2 (BB) remains!
        state = engine.apply(state, PlayerId(1), PokerAction.Fold).state

        assertTrue(engine.isFinished(state))
        assertEquals(PokerStreet.FINISHED, state.street)

        val bbResult = state.results.first { it.id == PlayerId(2) }
        assertTrue(bbResult.isWinner)
        assertEquals(150L, bbResult.payout)
        assertEquals(50L, bbResult.delta) // +50 net (won SB's 50)
    }

    @Test
    fun testPreflopCallFlopCheckProgression() {
        var state = engine.start(config, 12345L)

        // Player 3 calls 100
        state = engine.apply(state, PlayerId(3), PokerAction.Call).state
        // Player 0 calls 100
        state = engine.apply(state, PlayerId(0), PokerAction.Call).state
        // Player 1 (SB) calls 50 more (to 100)
        state = engine.apply(state, PlayerId(1), PokerAction.Call).state
        // Player 2 (BB) checks
        state = engine.apply(state, PlayerId(2), PokerAction.Check).state

        // Preflop should finish, advance to Flop!
        assertEquals(PokerStreet.FLOP, state.street)
        assertEquals(3, state.communityCards.size)
        assertEquals(1, state.burnedCards.size)
        // First actor on flop is first active left of dealer (seat 1)
        assertEquals(PlayerId(1), state.currentActor)
        assertEquals(0L, state.currentStreetHighBet)
    }

    @Test
    fun testAllInRunoutToShowdown() {
        var state = engine.start(config, 12345L)

        // Everyone goes All-in or folds preflop
        state = engine.apply(state, PlayerId(3), PokerAction.AllIn).state
        state = engine.apply(state, PlayerId(0), PokerAction.Fold).state
        state = engine.apply(state, PlayerId(1), PokerAction.Fold).state
        state = engine.apply(state, PlayerId(2), PokerAction.AllIn).state

        // Since all eligible players are all-in, engine should run out board to river and showdown
        assertTrue(engine.isFinished(state))
        assertEquals(5, state.communityCards.size)

        val settlement = engine.settle(state)
        assertNotNull(settlement)
        assertEquals(0L, settlement.deltas.values.sum())
    }
}
