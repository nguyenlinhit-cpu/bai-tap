package com.baiviet.game.bacay

import com.baiviet.core.engine.TableConfig
import com.baiviet.game.bacay.engine.BaCayEngine
import com.baiviet.game.bacay.engine.BaCayState
import com.baiviet.game.bacay.rules.BaCayRules
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Lưu/khôi phục ván giữa chừng (JSON như SavedGameRepository) phải cho ra state y hệt,
 * và chơi tiếp từ state khôi phục cho kết quả giống hệt chơi tiếp từ state gốc.
 */
class BaCaySaveRestoreTest {
    private val json = Json { ignoreUnknownKeys = true; allowStructuredMapKeys = true }
    private val engine = BaCayEngine()

    private fun roundTrip(s: BaCayState): BaCayState =
        json.decodeFromString(BaCayState.serializer(), json.encodeToString(BaCayState.serializer(), s))

    @Test
    fun midGameStateRoundTripsAtEveryStep() {
        for (players in 2..4) {
            for (seed in 1L..30L) {
                var s = engine.start(TableConfig(players, BaCayRules.DEFAULT, 100L), seed)
                var steps = 0
                while (!engine.isFinished(s) && steps < 400) {
                    assertEquals(s, roundTrip(s))
                    val actor = engine.currentActors(s).minByOrNull { it.seat } ?: break
                    val legal = engine.legalActions(s, actor)
                    if (legal.isEmpty()) break
                    val action = legal[(seed.toInt() + steps) % legal.size]
                    val fromOriginal = engine.apply(s, actor, action).state
                    val fromRestored = engine.apply(roundTrip(s), actor, action).state
                    assertEquals(fromOriginal, fromRestored)
                    s = fromOriginal
                    steps++
                }
                assertEquals(s, roundTrip(s))
            }
        }
    }
}
