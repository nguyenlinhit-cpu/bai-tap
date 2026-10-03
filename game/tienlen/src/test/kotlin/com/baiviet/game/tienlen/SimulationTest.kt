package com.baiviet.game.tienlen

import com.baiviet.core.ai.BotLevel
import com.baiviet.core.cards.Deck
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.tienlen.bot.HandAnalyzer
import com.baiviet.game.tienlen.bot.TienLenBot
import com.baiviet.game.tienlen.engine.TienLenEngine
import com.baiviet.game.tienlen.engine.TienLenState
import com.baiviet.game.tienlen.engine.TienLenView
import com.baiviet.game.tienlen.engine.TlAction
import com.baiviet.game.tienlen.rules.TienLenRules
import com.baiviet.game.tienlen.rules.TlScoring
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Bot đấu bot nhiều ván ngẫu nhiên: không crash, không trạng thái bất hợp lệ, tổng tiền = 0.
 */
class SimulationTest {

    private val engine = TienLenEngine()

    private fun checkInvariants(s: TienLenState) {
        val all = s.hands.flatten() + s.playedCards
        assertEquals("Lá trùng", all.size, all.toSet().size)
        assertEquals(s.playerCount * 13, all.size)
        if (!s.finished) assertTrue("Lượt ở ghế không còn chơi", s.isActive(s.turn))
    }

    private fun playGame(
        players: Int,
        seed: Long,
        rules: TienLenRules,
        levels: List<BotLevel>,
        previousWinner: PlayerId?,
        bot: TienLenBot,
    ): Pair<TienLenState, Long> = runBlocking {
        val table = TableConfig(players, rules, betUnit = 100L, minBalanceMultiplier = rules.minBalanceMultiplier)
        var s = engine.startGame(table, seed, previousWinner)
        var steps = 0
        while (!engine.isFinished(s)) {
            val seat = s.turn
            val legal = engine.legalActions(s, PlayerId(seat))
            assertTrue("Không có nước hợp lệ", legal.isNotEmpty())
            val action = bot.decide(engine.viewOf(s, PlayerId(seat)), legal, levels[seat])
            s = engine.apply(s, PlayerId(seat), action).state
            checkInvariants(s)
            steps++
            assertTrue("Ván quá dài", steps < 1_000)
        }
        val settlement = engine.settle(s)
        s to settlement.deltas.values.sum()
    }

    @Test
    fun `bots play 10000 random games without errors and zero-sum`() {
        val rnd = Random(2024)
        val bot = TienLenBot(Random(1))
        repeat(10_000) { i ->
            val players = 2 + i % 3
            val rules = if (i % 4 == 3) TienLenRules(scoring = TlScoring.RANKING) else TienLenRules()
            val levels = List(players) { if (rnd.nextBoolean()) BotLevel.EASY else BotLevel.NORMAL }
            val prev = if (i % 2 == 0) null else PlayerId(rnd.nextInt(players))
            val (_, sum) = playGame(players, rnd.nextLong(), rules, levels, prev, bot)
            assertEquals(0L, sum)
        }
    }

    @Test
    fun `house rule variants also run cleanly`() {
        val rnd = Random(7)
        val bot = TienLenBot(Random(3))
        val variants = listOf(
            TienLenRules(forbidFinishWithTwo = true),
            TienLenRules(fourPairsCutWithoutTurn = false, quadCutsPairOfTwos = false),
            TienLenRules(require3SpadesOnFirstMove = false, sameColorInstantWinCount = 12),
            TienLenRules(scoring = TlScoring.RANKING, instantWinCountsPenalties = true),
        )
        repeat(800) { i ->
            val rules = variants[i % variants.size]
            val players = 2 + i % 3
            val (_, sum) = playGame(players, rnd.nextLong(), rules, List(players) { BotLevel.NORMAL }, null, bot)
            assertEquals(0L, sum)
        }
    }

    @Test
    fun `hard bot plays full games`() {
        val rnd = Random(11)
        val bot = TienLenBot(Random(5), timeBudgetMs = 15L)
        repeat(6) { i ->
            val players = 2 + i % 3
            val levels = List(players) { if (it == 0) BotLevel.HARD else BotLevel.NORMAL }
            val (_, sum) = playGame(players, rnd.nextLong(), TienLenRules(), levels, null, bot)
            assertEquals(0L, sum)
        }
    }

    @Test
    fun `normal bot beats easy bot over many games`() {
        val rnd = Random(99)
        val bot = TienLenBot(Random(9))
        var normalTotal = 0L
        repeat(600) {
            val (s, _) = playGame(2, rnd.nextLong(), TienLenRules(), listOf(BotLevel.NORMAL, BotLevel.EASY), null, bot)
            normalTotal += engine.settle(s).deltas[PlayerId(0)]!!
        }
        assertTrue("Bot Thường phải thắng bot Dễ (tổng = $normalTotal)", normalTotal > 0)
    }

    @Test
    fun `bot decision depends only on its view`(): Unit = runBlocking {
        val deck = Deck.shuffled(42L).second
        val hands = deck.chunked(13)
        val s1 = engine.startWithHands(TienLenRules(), 4, 100L, hands, previousWinner = PlayerId(0))
        if (s1.finished) return@runBlocking
        // Đổi bài ẩn giữa các đối thủ — view của ghế 0 không đổi
        val swapped = listOf(hands[0], hands[2], hands[3], hands[1])
        val s2 = engine.startWithHands(TienLenRules(), 4, 100L, swapped, previousWinner = PlayerId(0))
        val v1: TienLenView = engine.viewOf(s1, PlayerId(0))
        val v2: TienLenView = engine.viewOf(s2, PlayerId(0))
        assertEquals(v1, v2)
        for (level in BotLevel.entries) {
            val a1 = TienLenBot(Random(1), 20L).decide(v1, engine.legalActions(s1, PlayerId(0)), level)
            val a2 = TienLenBot(Random(1), 20L).decide(v2, engine.legalActions(s2, PlayerId(0)), level)
            if (level != BotLevel.HARD) assertEquals(a1, a2)
            assertTrue(a1 is TlAction.Play)
        }
    }

    @Test
    fun `hand analyzer keeps straights and pairs intact`() {
        val groups = HandAnalyzer.decompose(cards("3s 4h 5d 6c 7s 9h 9d Jc Qs Kh 2d 2s Ac"))
        assertTrue(groups.any { it.cards.size == 5 && it.cards.first() == c("3s") })
        assertTrue(groups.any { it.cards.toSet() == setOf(c("9h"), c("9d")) })
        assertTrue(groups.any { it.cards.toSet() == setOf(c("2d"), c("2s")) })
        assertEquals(13, groups.sumOf { it.size })
    }
}
