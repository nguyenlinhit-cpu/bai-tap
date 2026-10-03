package com.baiviet.game.maubinh.rules

import com.baiviet.core.engine.RuleConfig
import kotlinx.serialization.Serializable

/**
 * Cấu hình luật chơi Mậu Binh (Binh Xập Xám) — hỗ trợ tùy biến Luật Nhà.
 */
@Serializable
data class MauBinhRules(
    /** A-2-3-4-5 là sảnh nhỏ nhất (mặc định false: lớn thứ nhì, sau 10-J-Q-K-A). */
    val aceTwoThreeFourFiveIsLowest: Boolean = false,
    /** Thắng cả 3 chi với 1 người -> nhân đôi chi với người đó (mặc định bật). */
    val sapThreeChiMultiplier: Boolean = true,
    /** Sập cả làng (thắng cả 3 chi với tất cả người chơi) -> nhân đôi thêm một lần nữa. */
    val sapLangMultiplier: Boolean = true,
    /** Số chi bị phạt khi binh lủng (mặc định 6 chi/người). */
    val foulPenaltyChi: Int = 6,
    /** Thời gian xếp bài (giây), mặc định 60s theo đặc tả. */
    val turnSeconds: Int = 60,
    /** Chi thưởng: Sám cô ở Chi 3 (3 chi). */
    val bonusChiSamChi3: Int = 3,
    /** Chi thưởng: Cù lũ ở Chi 2 (2 chi). */
    val bonusChiCuLuChi2: Int = 2,
    /** Chi thưởng: Tứ quý ở Chi 1 (4 chi). */
    val bonusChiTuQuyChi1: Int = 4,
    /** Chi thưởng: Tứ quý ở Chi 2 (8 chi). */
    val bonusChiTuQuyChi2: Int = 8,
    /** Chi thưởng: Thùng phá sảnh ở Chi 1 (5 chi). */
    val bonusChiThungPhaSanhChi1: Int = 5,
    /** Chi thưởng: Thùng phá sảnh ở Chi 2 (10 chi). */
    val bonusChiThungPhaSanhChi2: Int = 10,
    /** Tới trắng: Rồng cuốn (13 lá cùng chất) -> 24 chi/người. */
    val instantWinDragonRollChi: Int = 24,
    /** Tới trắng: Sảnh rồng (13 lá từ 2 đến A) -> 12 chi/người. */
    val instantWinDragonStraightChi: Int = 12,
    /** Tới trắng: Năm đôi một sám -> 3 chi/người. */
    val instantWinFivePairsTripleChi: Int = 3,
    /** Tới trắng: Lục phé bôn (6 đôi) -> 3 chi/người. */
    val instantWinSixPairsChi: Int = 3,
    /** Tới trắng: Ba thùng -> 3 chi/người. */
    val instantWinThreeFlushesChi: Int = 3,
    /** Tới trắng: Ba sảnh -> 3 chi/người. */
    val instantWinThreeStraightsChi: Int = 3,
    /** Bội số xu tối thiểu để vào bàn (mặc định 30B). */
    val minBalanceMultiplier: Long = 30L,
) : RuleConfig {
    companion object {
        const val GAME_ID = "maubinh"
        val DEFAULT = MauBinhRules()
    }
}
