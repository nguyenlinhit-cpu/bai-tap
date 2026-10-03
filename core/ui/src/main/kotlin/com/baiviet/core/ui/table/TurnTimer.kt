package com.baiviet.core.ui.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.baiviet.core.ui.audio.GameAudio
import com.baiviet.core.ui.audio.Sfx
import kotlinx.coroutines.delay

/**
 * Phần thời gian lượt còn lại (1 → 0), cập nhật ~10 lần/giây.
 * Trả về null khi không có lượt đang đếm. Kêu "tích" ở 5 giây cuối nếu [tickSound].
 */
@Composable
fun rememberTurnProgress(startedAt: Long, seconds: Int, active: Boolean, tickSound: Boolean = false): Float? {
    var progress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(startedAt, active, seconds) {
        if (!active || startedAt == 0L) return@LaunchedEffect
        var lastWhole = Int.MAX_VALUE
        while (true) {
            val leftMs = seconds * 1000L - (System.currentTimeMillis() - startedAt)
            progress = (leftMs.toFloat() / (seconds * 1000f)).coerceIn(0f, 1f)
            val whole = ((leftMs + 999) / 1000).toInt()
            if (tickSound && whole in 1..5 && whole < lastWhole) GameAudio.play(Sfx.TICK)
            lastWhole = whole
            if (leftMs <= 0) break
            delay(100)
        }
    }
    return if (active && startedAt != 0L) progress else null
}

/**
 * Vị trí ghế quanh bàn (không gồm bạn, ngồi dưới) theo chiều ngược kim đồng hồ:
 * 2 người: trên; 3 người: trên-phải, trên-trái; 4 người: phải, trên, trái.
 */
fun opponentAlignments(opponents: Int): List<Alignment> = when (opponents) {
    1 -> listOf(Alignment.TopCenter)
    2 -> listOf(Alignment.TopEnd, Alignment.TopStart)
    else -> listOf(Alignment.CenterEnd, Alignment.TopCenter, Alignment.CenterStart)
}
