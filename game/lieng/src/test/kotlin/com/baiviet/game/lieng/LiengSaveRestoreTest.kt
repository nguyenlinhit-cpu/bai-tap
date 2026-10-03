package com.baiviet.game.lieng

import com.baiviet.core.engine.TableConfig
import com.baiviet.game.lieng.engine.LiengEngine
import com.baiviet.game.lieng.engine.LiengState
import com.baiviet.game.lieng.rules.LiengRules
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Lưu/khôi phục ván giữa chừng (JSON như SavedGameRepository) phải cho ra state y hệt,
 * và chơi tiếp từ state khôi phục cho kết quả giống hệt chơi tiếp từ state gốc.
 */
class LiengSaveRestoreTest {
    private val json = Json { ignoreUnknownKeys = true; allowStructuredMapKeys = true }
    private val engine = LiengEngine()

    private fun roundTrip(s: LiengState): LiengState =
        json.decodeFromString(LiengState.serializer(), json.encodeToString(LiengState.serializer(), s))

    @Test
    fun midGameStateRoundTripsAtEveryStep() {
        for (players in 2..4) {
            for (seed in 1L..30L) {
                var s = engine.start(TableConfig(players, LiengRules.DEFAULT, 100L), seed)
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
