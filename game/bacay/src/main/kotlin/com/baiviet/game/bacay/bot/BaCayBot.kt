package com.baiviet.game.bacay.bot

import com.baiviet.core.ai.Bot
import com.baiviet.core.ai.BotLevel
import com.baiviet.game.bacay.engine.BaCayAction
import com.baiviet.game.bacay.engine.BaCayView
import kotlin.random.Random

/**
 * AI Bot chơi Ba Cây (Cào).
 */
class BaCayBot(private val random: Random = Random.Default) : Bot<BaCayView, BaCayAction> {

    override suspend fun decide(view: BaCayView, legal: List<BaCayAction>, level: BotLevel): BaCayAction {
        require(legal.isNotEmpty()) { "Không có hành động hợp lệ để bot lựa chọn" }
        // Bot ưu tiên tự lật bài của mình
        return legal.find { it is BaCayAction.Reveal } ?: legal.first()
    }
}
