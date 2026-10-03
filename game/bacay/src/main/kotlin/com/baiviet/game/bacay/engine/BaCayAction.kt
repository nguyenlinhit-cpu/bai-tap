package com.baiviet.game.bacay.engine

import kotlinx.serialization.Serializable

@Serializable
sealed interface BaCayAction {
    /** Người chơi lật bài của mình */
    @Serializable
    data object Reveal : BaCayAction

    /** Lật bài tất cả người chơi và kết thúc ván */
    @Serializable
    data object RevealAll : BaCayAction
}
