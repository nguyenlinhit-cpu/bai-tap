package com.baiviet.game.maubinh.bot

import com.baiviet.core.ai.Bot
import com.baiviet.core.ai.BotLevel
import com.baiviet.game.maubinh.engine.MauBinhAction
import com.baiviet.game.maubinh.engine.MauBinhPhase
import com.baiviet.game.maubinh.engine.MauBinhView
import com.baiviet.game.maubinh.rules.MauBinhHandOptimizer
import kotlin.random.Random

/**
 * AI Bot cho game Mậu Binh (Binh Xập Xám).
 * Tuân thủ nghiêm ngặt:
 * - Chỉ sử dụng thông tin trong [MauBinhView] (không nhìn bài úp đối thủ).
 * - Luôn tránh binh lủng trong mọi trường hợp.
 * - Tự nhận biết và báo Mậu binh tới trắng.
 */
class MauBinhBot(
    private val random: Random = Random.Default,
) : Bot<MauBinhView, MauBinhAction> {

    override suspend fun decide(
        view: MauBinhView,
        legal: List<MauBinhAction>,
        level: BotLevel,
    ): MauBinhAction {
        require(legal.isNotEmpty()) { "Danh sách hành động hợp lệ không được rỗng" }

        if (view.phase == MauBinhPhase.REVEALING) {
            return legal.firstOrNull { it is MauBinhAction.NextRevealStep } ?: legal.first()
        }

        // Giai đoạn ARRANGING
        // 1. Kiểm tra tới trắng
        val instantWinAction = legal.firstOrNull { it is MauBinhAction.DeclareInstantWin }
        if (instantWinAction != null) {
            return instantWinAction
        }

        // 2. Tìm phương án xếp bài tối ưu
        val optResult = MauBinhHandOptimizer.findBestArrangement(view.myCards, view.rules)

        // Nếu tối ưu tìm ra tới trắng
        if (optResult.instantWinType != null) {
            val iwAction = legal.firstOrNull {
                it is MauBinhAction.DeclareInstantWin && it.type == optResult.instantWinType
            }
            if (iwAction != null) return iwAction
        }

        return MauBinhAction.SubmitArrangement(optResult.arrangement)
    }
}
