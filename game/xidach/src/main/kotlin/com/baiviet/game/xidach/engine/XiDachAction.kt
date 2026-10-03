package com.baiviet.game.xidach.engine

import com.baiviet.core.engine.PlayerId
import kotlinx.serialization.Serializable

@Serializable
sealed interface XiDachAction {
    /** Rút thêm 1 lá bài từ nọc (tối đa 5 lá) */
    @Serializable
    data object Hit : XiDachAction

    /** Dằn bài (kết thúc lượt của mình) */
    @Serializable
    data object Stand : XiDachAction

    /** Nhà cái xét riêng một người chơi */
    @Serializable
    data class Inspect(val target: PlayerId) : XiDachAction

    /** Nhà cái xét tất cả người chơi còn lại */
    @Serializable
    data object InspectAll : XiDachAction
}
