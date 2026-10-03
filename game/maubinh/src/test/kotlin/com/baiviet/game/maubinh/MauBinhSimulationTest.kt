package com.baiviet.game.maubinh

import com.baiviet.core.ai.BotLevel
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.TableConfig
import com.baiviet.game.maubinh.bot.MauBinhBot
import com.baiviet.game.maubinh.engine.MauBinhAction
import com.baiviet.game.maubinh.engine.MauBinhEngine
import com.baiviet.game.maubinh.engine.MauBinhState
import com.baiviet.game.maubinh.rules.MauBinhRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Mô phỏng bot đấu bot 10.000 ván ngẫu nhiên Mậu Binh (Binh Xập Xám):
 * - Không crash.
 * - Không deadlock.
 * - Zero-sum bảo toàn 100% ván (tổng delta tiền = 0 tuyệt đối).
 * - Bot AI không bao giờ binh lủng.
 */
class MauBinhSimulationTest {

    private val engine = MauBinhEngine()

    private fun playGame(
        players: Int,
        seed: Long,
        rules: MauBinhRules,
        levels: List<BotLevel>,
        bot: MauBinhBot,
    ): Pair<MauBinhState, Long> = runBlocking {
        val table = TableConfig(
            playerCount = players,
            rules = rules,
            betUnit = 100L,
            minBalanceMultiplier = rules.minBalanceMultiplier.toInt(),
        )
        var s = engine.start(table, seed)
        var steps = 0

        while (!engine.isFinished(s)) {
            val actors = engine.currentActors(s)
            assertTrue("Phải luôn có ít nhất 1 người chơi được quyền hành động", actors.isNotEmpty())

            val actor = actors.first()
            val legal = engine.legalActions(s, actor)
            assertTrue("Phải có hành động hợp lệ cho actor $actor", legal.isNotEmpty())

            val view = engine.viewOf(s, actor)
            val action = bot.decide(view, legal, levels[actor.seat])
            s = engine.apply(s, actor, action).state

            steps++
            assertTrue("Ván không được lặp vô tận (steps = $steps)", steps < 100)
        }

        // Kiểm tra sau khi kết thúc: bot không bao giờ binh lủng
        for (p in s.players) {
            if (p.instantWinDeclared == null && p.arrangement != null) {
                assertFalse("Bot không bao giờ được binh lủng: seat=${p.playerId.seat}", p.arrangement!!.isFoul(rules))
            }
        }

        val settlement = engine.settle(s)
        val sum = settlement.deltas.values.sum()
        s to sum
    }

    @Test
    fun `bots play 10000 random games without errors and zero-sum`() {
        val rnd = Random(2026)
        val bot = MauBinhBot(Random(42))

        repeat(10_000) { i ->
            val players = 2 + (i % 3) // 2, 3, hoặc 4 người chơi
            val rules = when (i % 4) {
                0 -> MauBinhRules.DEFAULT
                1 -> MauBinhRules(aceTwoThreeFourFiveIsLowest = true)
                2 -> MauBinhRules(sapThreeChiMultiplier = false, sapLangMultiplier = false)
                else -> MauBinhRules(foulPenaltyChi = 10)
            }
            val levels = List(players) { if (rnd.nextBoolean()) BotLevel.EASY else BotLevel.NORMAL }
            val (_, sum) = playGame(players, rnd.nextLong(), rules, levels, bot)
            assertEquals("Tổng tiền ván $i phải bằng 0", 0L, sum)
        }
    }

    @Test
    fun `hard bot plays cleanly in 4-player game`() {
        val rnd = Random(12345)
        val bot = MauBinhBot(Random(99))

        repeat(50) { i ->
            val players = 4
            val levels = List(players) { if (it == 0) BotLevel.HARD else BotLevel.NORMAL }
            val (_, sum) = playGame(players, rnd.nextLong(), MauBinhRules.DEFAULT, levels, bot)
            assertEquals("Tổng tiền ván Hard bot phải bằng 0", 0L, sum)
        }
    }
}
