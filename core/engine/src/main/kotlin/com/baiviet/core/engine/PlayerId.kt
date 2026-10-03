package com.baiviet.core.engine

import kotlinx.serialization.Serializable

/** Định danh người chơi theo vị trí ngồi (0-based). */
@JvmInline
@Serializable
value class PlayerId(val seat: Int) {
    override fun toString(): String = "P$seat"
}
