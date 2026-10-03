package com.baiviet.game.lieng.bot

import com.baiviet.core.ai.Bot
import com.baiviet.core.ai.BotLevel
import com.baiviet.game.lieng.engine.LiengAction
import com.baiviet.game.lieng.engine.LiengView
import com.baiviet.game.lieng.rules.LiengHandType
import kotlin.random.Random

/**
 * Bot Liêng: ước lượng xác suất thắng của tay bài so với số đối thủ còn lại,
 * so với tỉ lệ pot để Theo/Tố/Úp; có tố láo (bluff) ở Thường/Khó.
 * Chỉ dùng [LiengView] — không thấy bài người khác.
 */
class LiengBot(private val random: Random = Random.Default) : Bot<LiengView, LiengAction> {

    override suspend fun decide(view: LiengView, legal: List<LiengAction>, level: BotLevel): LiengAction {
        require(legal.isNotEmpty()) { "Không có hành động hợp lệ để bot lựa chọn" }
        if (legal.size == 1) return legal.first()

        val opponents = view.players.count { it.id != view.myId && !it.isFolded }.coerceAtLeast(1)
        // Xác suất thắng 1 đối thủ ≈ mức mạnh; nhiều đối thủ thì nhân lũy thừa
        val single = strength(view.myHand.type, view.myHand.points)
        val noise = when (level) {
            BotLevel.EASY -> random.nextDouble(-0.2, 0.2)
            BotLevel.NORMAL -> random.nextDouble(-0.06, 0.06)
            BotLevel.HARD -> 0.0
        }
        val win = (Math.pow(single, opponents.toDouble()) + noise).coerceIn(0.0, 1.0)

        val canCheck = LiengAction.Check in legal
        val canCall = LiengAction.Call in legal
        val raises = legal.filterIsInstance<LiengAction.Raise>().sortedBy { it.amount }
        val potOdds = if (view.toCall > 0) view.toCall.toDouble() / (view.pot + view.toCall) else 0.0

        val bluff = when (level) {
            BotLevel.EASY -> 0.0
            BotLevel.NORMAL -> 0.07
            BotLevel.HARD -> 0.12
        }
        if (raises.isNotEmpty() && win < 0.35 && view.currentRound < view.rules.maxBettingRounds && random.nextDouble() < bluff) {
            return raises.first()
        }

        return when {
            win > 0.8 && raises.isNotEmpty() -> if (random.nextDouble() < 0.6) raises.last() else raises.first()
            win > 0.8 && LiengAction.AllIn in legal && !canCall && !canCheck -> LiengAction.AllIn
            win > 0.6 && raises.isNotEmpty() && random.nextDouble() < 0.4 -> raises.first()
            canCheck -> LiengAction.Check
            canCall && win >= potOdds * 1.1 -> LiengAction.Call
            // Chỉ còn cách tất tay để theo
            !canCall && view.toCall > 0 && LiengAction.AllIn in legal && win > 0.55 -> LiengAction.AllIn
            else -> LiengAction.Fold
        }
    }

    /** Xác suất xấp xỉ thắng một đối thủ ngẫu nhiên. */
    private fun strength(type: LiengHandType, points: Int): Double = when (type) {
        LiengHandType.SAP -> 0.995
        LiengHandType.LIENG -> 0.96
        LiengHandType.ANH -> 0.9
        LiengHandType.DIEM -> 0.08 + points * 0.09
    }
}
