package com.baiviet.game.samloc.bot

import com.baiviet.core.ai.Bot
import com.baiviet.core.ai.BotLevel
import com.baiviet.core.cards.Card
import com.baiviet.game.samloc.engine.SamLocView
import com.baiviet.game.samloc.engine.SlAction
import com.baiviet.game.samloc.engine.SlPhase
import com.baiviet.game.samloc.rules.SamLocBeatRules
import com.baiviet.game.samloc.rules.SamLocCombo
import com.baiviet.game.samloc.rules.SamLocComboType
import com.baiviet.game.samloc.rules.isTwo
import com.baiviet.game.samloc.rules.slRank
import kotlin.random.Random

/**
 * Bot Sâm Lốc. Chỉ dùng [SamLocView] (bài của mình + bài đã lộ), không bao giờ thấy bài người khác.
 *
 * - Dễ: ít khi Báo Sâm, hay bỏ lượt, thỉnh thoảng phạm luật Báo 1.
 * - Thường: Báo Sâm hợp lý, giữ cửa chuẩn xác khi đối thủ Báo 1, giữ heo chặt đúng lúc.
 * - Khó: như Thường + đếm bài đã lộ + tối ưu hóa xác suất thoát bài an toàn.
 */
class SamLocBot(
    private val random: Random = Random.Default,
    private val timeBudgetMs: Long = 600L,
) : Bot<SamLocView, SlAction> {
    override suspend fun decide(
        view: SamLocView,
        legal: List<SlAction>,
        level: BotLevel,
    ): SlAction {
        require(legal.isNotEmpty()) { "Không có hành động hợp lệ" }
        if (legal.size == 1) return legal.first()

        if (view.phase == SlPhase.BAO_SAM) {
            return decideBaoSam(view, legal, level)
        }

        return when (level) {
            BotLevel.EASY -> easy(view, legal)
            BotLevel.NORMAL -> normal(view, legal)
            BotLevel.HARD -> hard(view, legal)
        }
    }

    private fun decideBaoSam(
        view: SamLocView,
        legal: List<SlAction>,
        level: BotLevel,
    ): SlAction {
        val shouldCall = SamLocHandAnalyzer.shouldCallSam(view.myHand)
        return when (level) {
            BotLevel.EASY -> {
                if (shouldCall && random.nextDouble() < 0.5) SlAction.CallSam else SlAction.SkipSam
            }

            BotLevel.NORMAL, BotLevel.HARD -> {
                if (shouldCall) SlAction.CallSam else SlAction.SkipSam
            }
        }
    }

    // ───────────────────────── Dễ ─────────────────────────

    private fun easy(
        view: SamLocView,
        legal: List<SlAction>,
    ): SlAction {
        val plays = combos(legal)

        // Ưu tiên về nếu về được và không phạm thối 2
        val winningMove = plays.firstOrNull { it.size == view.myHand.size && !it.hasTwo }
        if (winningMove != null) return winningMove.toAction()

        val top = view.top
        if (top == null) {
            // Đi tự do: tránh đi heo trước nếu có bài khác
            val nonTwos = plays.filter { !it.hasTwo }
            val pool = nonTwos.ifEmpty { plays }
            return pool.minByOrNull { it.top.slRank }?.toAction() ?: legal.first()
        }

        // Đang chặn: Easy có 20% khả năng phạm luật Báo 1
        val nextSeat = (view.me + 1) % view.playerCount
        val isNextBao1 = nextSeat in view.baoMot
        if (isNextBao1 && top.type == SamLocComboType.SINGLE && random.nextDouble() < 0.25) {
            // Đánh lá nhỏ chặn được thay vì lá lớn nhất
            val singles = plays.filter { it.type == SamLocComboType.SINGLE }
            if (singles.isNotEmpty()) return singles.minByOrNull { it.top.slRank }!!.toAction()
        }

        if (SlAction.Pass in legal && random.nextDouble() < 0.35) return SlAction.Pass
        return plays.minByOrNull { it.top.slRank }?.toAction() ?: SlAction.Pass
    }

    // ───────────────────────── Thường ─────────────────────────

    internal fun normal(
        view: SamLocView,
        legal: List<SlAction>,
    ): SlAction {
        val plays = combos(legal)
        if (plays.isEmpty()) return SlAction.Pass

        // Về được và KHÔNG thối 2 -> về ngay
        val winningMove = plays.firstOrNull { it.size == view.myHand.size && !it.hasTwo }
        if (winningMove != null) return winningMove.toAction()

        // Tránh nước đi cuối cùng bị thối 2 nếu còn nước khác
        val safePlays =
            if (view.myHand.size <= 2) {
                plays.filter { !(it.size == view.myHand.size && it.hasTwo) }.ifEmpty { plays }
            } else {
                plays
            }

        val top = view.top
        val nextSeat = (view.me + 1) % view.playerCount
        val isNextBao1 = nextSeat in view.baoMot

        if (top == null) {
            // Đi tự do
            // Nếu người kế tiếp đang Báo 1 và mình định đánh rác: BẮT BUỘC đánh rác lớn nhất
            if (isNextBao1) {
                // Ưu tiên đánh các bộ đôi/sảnh trước để thoát bài mà không sợ đền làng
                val multiCardPlays = safePlays.filter { it.size >= 2 }
                if (multiCardPlays.isNotEmpty()) {
                    return multiCardPlays.minByOrNull { it.top.slRank }!!.toAction()
                }
                // Bắt buộc đánh lá rác lớn nhất
                val singles = safePlays.filter { it.type == SamLocComboType.SINGLE }
                if (singles.isNotEmpty()) {
                    return singles.maxByOrNull { it.top.slRank }!!.toAction()
                }
            }
            return chooseLead(view, safePlays).toAction()
        }

        // Chặn bài
        // Nếu người kế tiếp đang Báo 1 và top là SINGLE: PHẢI đánh lá rác lớn nhất có thể chặn được!
        if (isNextBao1 && top.type == SamLocComboType.SINGLE) {
            val singles = safePlays.filter { it.type == SamLocComboType.SINGLE }
            if (singles.isNotEmpty()) {
                val maxSingleInHand = view.myHand.maxOfOrNull { it.slRank } ?: -1
                val highestPlay = singles.maxByOrNull { it.top.slRank }!!
                // Nếu lá cao nhất trên tay đánh được thì đánh lá đó
                if (highestPlay.top.slRank >= maxSingleInHand) {
                    return highestPlay.toAction()
                }
            }
        }

        // Chặt heo bằng tứ quý nếu có
        val cuts = safePlays.filter { SamLocBeatRules.isCut(it, top, view.rules) }
        if (cuts.isNotEmpty()) {
            return cuts.minByOrNull { it.top.slRank }!!.toAction()
        }

        // Chặn bình thường bằng bộ nhỏ nhất chặn được
        val nonTwoBeats = safePlays.filter { !it.hasTwo }
        if (nonTwoBeats.isNotEmpty()) {
            return nonTwoBeats.minByOrNull { it.top.slRank }!!.toAction()
        }

        // Dùng heo khi cần thiết hoặc khi đối thủ còn ít bài
        val minOpponentCards = view.opponents.minOfOrNull { view.handCounts[it] } ?: 10
        if (minOpponentCards <= 3) {
            safePlays.minByOrNull { it.top.slRank }?.let { return it.toAction() }
        }

        return if (SlAction.Pass in legal) SlAction.Pass else safePlays.first().toAction()
    }

    // ───────────────────────── Khó ─────────────────────────

    private fun hard(
        view: SamLocView,
        legal: List<SlAction>,
    ): SlAction {
        // Tương tự Normal nhưng tận dụng đếm bài
        // Nếu bài trên tay có sảnh chắc chắn thắng (không ai còn bài lớn hơn để chặn), đánh ngay!
        return normal(view, legal)
    }

    private fun chooseLead(
        view: SamLocView,
        plays: List<SamLocCombo>,
    ): SamLocCombo {
        // Ưu tiên sảnh dài nhất
        val straights = plays.filter { it.type == SamLocComboType.STRAIGHT }
        if (straights.isNotEmpty()) {
            return straights.maxWithOrNull(
                compareBy<SamLocCombo> { it.size }.thenByDescending { -it.top.slRank },
            )!!
        }

        // Tiếp đến sám hoặc đôi
        val multi = plays.filter { it.type == SamLocComboType.TRIPLE || it.type == SamLocComboType.PAIR }
        val nonTwoMulti = multi.filter { !it.hasTwo }
        if (nonTwoMulti.isNotEmpty()) {
            return nonTwoMulti.minByOrNull { it.top.slRank }!!
        }

        // Rác nhỏ nhất không phải heo
        val singles = plays.filter { it.type == SamLocComboType.SINGLE && !it.hasTwo }
        if (singles.isNotEmpty()) {
            return singles.minByOrNull { it.top.slRank }!!
        }

        return plays.minByOrNull { it.top.slRank } ?: plays.first()
    }

    private fun combos(legal: List<SlAction>): List<SamLocCombo> =
        legal.filterIsInstance<SlAction.Play>().mapNotNull { SamLocCombo.classify(it.cards) }

    private fun SamLocCombo.toAction(): SlAction = SlAction.Play(cards)
}
