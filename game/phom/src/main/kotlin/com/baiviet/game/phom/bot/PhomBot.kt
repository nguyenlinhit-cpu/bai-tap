package com.baiviet.game.phom.bot

import com.baiviet.core.ai.Bot
import com.baiviet.core.ai.BotLevel
import com.baiviet.game.phom.engine.PhomAction
import com.baiviet.game.phom.engine.PhomPhase
import com.baiviet.game.phom.engine.PhomView
import kotlin.random.Random

/**
 * Bot Phỏm (Tá Lả) — 3 cấp độ thông minh.
 * Chỉ sử dụng thông tin trong [PhomView], không bao giờ nhìn thấy bài úp trên tay người khác.
 */
class PhomBot(
    private val random: Random = Random.Default,
    private val timeBudgetMs: Long = 600L,
) : Bot<PhomView, PhomAction> {

    override suspend fun decide(
        view: PhomView,
        legal: List<PhomAction>,
        level: BotLevel,
    ): PhomAction {
        require(legal.isNotEmpty()) { "Không có hành động hợp lệ" }
        if (legal.size == 1) return legal.first()

        // 1. Nếu có thể Ù / Ù khan: luôn luôn Ù
        legal.firstOrNull { it is PhomAction.DeclareU || it is PhomAction.DeclareUKhan }?.let {
            return it
        }

        return when (view.phase) {
            PhomPhase.DRAW_OR_EAT -> decideDrawOrEat(view, legal, level)
            PhomPhase.MELD_AND_LAYOFF -> decideMeldAndLayOff(view, legal, level)
            PhomPhase.DISCARD -> decideDiscard(view, legal, level)
        }
    }

    private fun decideDrawOrEat(
        view: PhomView,
        legal: List<PhomAction>,
        level: BotLevel,
    ): PhomAction {
        val eatActions = legal.filterIsInstance<PhomAction.Eat>()
        if (eatActions.isEmpty()) {
            return legal.firstOrNull { it is PhomAction.Draw } ?: legal.first()
        }

        return when (level) {
            BotLevel.EASY -> {
                // 50% ăn, 50% bốc
                if (random.nextBoolean()) eatActions.first() else (legal.firstOrNull { it is PhomAction.Draw } ?: eatActions.first())
            }
            BotLevel.NORMAL, BotLevel.HARD -> {
                // Thường & Khó: ưu tiên ăn bài để chắc chắn tạo phỏm
                eatActions.first()
            }
        }
    }

    private fun decideMeldAndLayOff(
        view: PhomView,
        legal: List<PhomAction>,
        level: BotLevel,
    ): PhomAction {
        // Nếu có hành động gửi bài, luôn luôn gửi để giảm điểm rác
        legal.firstOrNull { it is PhomAction.LayOff }?.let { return it }

        // Nếu là hạ phỏm: chọn phương án hạ
        legal.firstOrNull { it is PhomAction.Meld }?.let { return it }

        return legal.first()
    }

    private fun decideDiscard(
        view: PhomView,
        legal: List<PhomAction>,
        level: BotLevel,
    ): PhomAction {
        val discardActions = legal.filterIsInstance<PhomAction.Discard>()
        if (discardActions.isEmpty()) return legal.first()

        val cards = discardActions.map { it.card }

        return when (level) {
            BotLevel.EASY -> {
                // Dễ: chọn ngẫu nhiên trong các lá có thể đánh
                val chosen = cards.random(random)
                PhomAction.Discard(chosen)
            }
            BotLevel.NORMAL, BotLevel.HARD -> {
                val allTableDiscards = view.discardPiles.flatten()
                val ranked = PhomHandAnalyzer.rankDiscardCandidates(view.myHand, cards, allTableDiscards)
                val bestCard = ranked.firstOrNull() ?: cards.first()
                PhomAction.Discard(bestCard)
            }
        }
    }
}
