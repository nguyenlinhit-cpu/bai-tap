package com.baiviet.game.xidach.bot

import com.baiviet.core.ai.Bot
import com.baiviet.core.ai.BotLevel
import com.baiviet.game.xidach.engine.XiDachAction
import com.baiviet.game.xidach.engine.XiDachPlayerStatus
import com.baiviet.game.xidach.engine.XiDachView
import com.baiviet.game.xidach.rules.XiDachHandType
import kotlin.random.Random

/**
 * AI Bot chơi Xì Dách với 3 cấp độ: Dễ, Thường, Khó.
 *
 * Chỉ sử dụng thông tin trong [XiDachView], tuyệt đối không gian lận bài úp.
 */
class XiDachBot(private val random: Random = Random.Default) : Bot<XiDachView, XiDachAction> {

    override suspend fun decide(view: XiDachView, legal: List<XiDachAction>, level: BotLevel): XiDachAction {
        require(legal.isNotEmpty()) { "Không có hành động hợp lệ để bot lựa chọn" }
        if (legal.size == 1) return legal.first()

        val isDealer = view.myId == view.dealerId
        val myHand = view.myHand

        return if (isDealer) {
            decideDealer(view, legal, level)
        } else {
            decidePlayer(view, legal, level)
        }
    }

    private fun decidePlayer(view: XiDachView, legal: List<XiDachAction>, level: BotLevel): XiDachAction {
        val myHand = view.myHand
        val score = myHand.score
        val cardCount = myHand.cards.size

        // Non (< 16): bắt buộc rút
        if (score < 16) {
            return legal.find { it is XiDachAction.Hit } ?: legal.first()
        }

        // Đã đạt Ngũ linh hoặc điểm tuyệt đối 21 -> dằn ngay
        if (myHand.type == XiDachHandType.NGU_LINH || score == 21) {
            return legal.find { it is XiDachAction.Stand } ?: legal.first()
        }

        // Đang có 4 lá tổng thấp (<= 14): cơ hội săn Ngũ linh rất lớn
        if (cardCount == 4 && score <= 14 && legal.contains(XiDachAction.Hit)) {
            val huntProb = when (level) {
                BotLevel.EASY -> 0.5f
                BotLevel.NORMAL -> 0.8f
                BotLevel.HARD -> 0.95f
            }
            if (random.nextFloat() < huntProb) return XiDachAction.Hit
        }

        // Quyết định khi điểm từ 16 đến 17
        if (score in 16..17 && legal.contains(XiDachAction.Hit) && legal.contains(XiDachAction.Stand)) {
            val hitProb = when (level) {
                BotLevel.EASY -> 0.5f
                BotLevel.NORMAL -> if (score == 16) 0.35f else 0.15f
                BotLevel.HARD -> if (score == 16 && cardCount == 2) 0.25f else 0.05f
            }
            return if (random.nextFloat() < hitProb) XiDachAction.Hit else XiDachAction.Stand
        }

        // Điểm >= 18: an toàn dằn bài
        return legal.find { it is XiDachAction.Stand } ?: legal.first()
    }

    private fun decideDealer(view: XiDachView, legal: List<XiDachAction>, level: BotLevel): XiDachAction {
        val myHand = view.myHand
        val score = myHand.score
        val canHit = legal.contains(XiDachAction.Hit)

        // Non (< 15): chỉ có thể rút
        if (score < 15 && canHit) {
            return XiDachAction.Hit
        }

        // Điểm cao (>= 18) hoặc có Ngũ linh: xét tất cả
        if (score >= 18 || myHand.type == XiDachHandType.NGU_LINH) {
            return legal.find { it is XiDachAction.InspectAll }
                ?: legal.filterIsInstance<XiDachAction.Inspect>().firstOrNull()
                ?: legal.first()
        }

        // Điểm từ 15 đến 17:
        // Tìm những con rút nhiều lá (3-4 lá, khả năng quắc cao) để xét trước
        val unresolvedPlayers = view.players.filter {
            it.id != view.dealerId && it.status != XiDachPlayerStatus.RESOLVED
        }

        val highCardCountCon = unresolvedPlayers.filter { it.cardCount >= 3 }
        if (highCardCountCon.isNotEmpty()) {
            val target = highCardCountCon.first().id
            val inspectAction = legal.find { it is XiDachAction.Inspect && it.target == target }
            if (inspectAction != null) return inspectAction
        }

        // Nếu điểm thấp (15) và ở mức Hard/Normal, cân nhắc rút thêm nếu các con còn lại chỉ có 2 lá (dằn sớm thường >= 18)
        if (score == 15 && canHit) {
            val hitProb = when (level) {
                BotLevel.EASY -> 0.4f
                BotLevel.NORMAL -> 0.6f
                BotLevel.HARD -> 0.75f
            }
            if (random.nextFloat() < hitProb) return XiDachAction.Hit
        }

        // Xét tất cả
        return legal.find { it is XiDachAction.InspectAll }
            ?: legal.filterIsInstance<XiDachAction.Inspect>().firstOrNull()
            ?: legal.first()
    }
}
