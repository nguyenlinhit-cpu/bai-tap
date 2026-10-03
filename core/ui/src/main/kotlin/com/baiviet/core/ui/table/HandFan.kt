package com.baiviet.core.ui.table

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.baiviet.core.ui.audio.GameAudio
import com.baiviet.core.ui.audio.Sfx
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.baiviet.core.cards.Card
import com.baiviet.core.ui.card.CARD_ASPECT
import com.baiviet.core.ui.card.PlayingCard
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Bài trên tay xếp hình quạt.
 *
 * Thao tác:
 * - Chạm một lá để nhấc (chọn) / hạ.
 * - Kéo ngang để quét chọn nhiều lá liên tiếp.
 * - Vuốt lên để đánh các lá đang chọn.
 *
 * Lá mới xuất hiện (khi chia) bay từ tâm bàn xuống.
 */
@Composable
fun HandFan(
    cards: List<Card>,
    selected: Set<Card>,
    cardWidth: Dp,
    onToggle: (Card) -> Unit,
    onSweep: (List<Card>) -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dimmed: Set<Card> = emptySet(),
    highlighted: Set<Card> = emptySet(),
) {
    val density = LocalDensity.current
    val liftDp = 22.dp
    val cardHeight = cardWidth * CARD_ASPECT
    val currentCards by rememberUpdatedState(cards)
    val toggle by rememberUpdatedState(onToggle)
    val sweep by rememberUpdatedState(onSweep)
    val swipe by rememberUpdatedState(onSwipeUp)
    val currentSelected by rememberUpdatedState(selected)
    var lastCount by remember { mutableStateOf(cards.size) }
    LaunchedEffect(cards.size) {
        if (cards.size > lastCount) GameAudio.play(Sfx.DEAL)
        lastCount = cards.size
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(cardHeight + liftDp + 8.dp)) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val n = cards.size
        val spacingPx = if (n <= 1) 0f else minOf(cardWidthPx * 0.62f, (totalWidthPx - cardWidthPx) / (n - 1))
        val usedPx = cardWidthPx + spacingPx * (n - 1).coerceAtLeast(0)
        val startPx = (totalWidthPx - usedPx) / 2f
        val mid = (n - 1) / 2f

        fun indexAt(x: Float): Int? {
            if (n == 0 || x < startPx || x > startPx + usedPx) return null
            if (spacingPx <= 0f) return 0
            return ((x - startPx) / spacingPx).toInt().coerceIn(0, n - 1)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight + liftDp + 8.dp)
                .pointerInput(enabled, n, spacingPx, startPx) {
                    if (!enabled) return@pointerInput
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val firstIndex = indexAt(down.position.x)
                        var totalX = 0f
                        var totalY = 0f
                        var mode = 0 // 0 = chạm, 1 = quét ngang, 2 = vuốt lên
                        val swept = linkedSetOf<Int>()
                        firstIndex?.let { swept += it }
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val delta = change.positionChange()
                            totalX += delta.x
                            totalY += delta.y
                            if (mode == 0) {
                                if (-totalY > slop * 2 && abs(totalY) > abs(totalX)) {
                                    mode = 2
                                } else if (abs(totalX) > slop) {
                                    mode = 1
                                }
                            }
                            if (mode == 1) {
                                indexAt(change.position.x)?.let { swept += it }
                            }
                            change.consume()
                        }
                        val list = currentCards
                        when (mode) {
                            0 -> firstIndex?.let { list.getOrNull(it) }?.let { toggle(it) }
                            1 -> sweep(swept.mapNotNull { list.getOrNull(it) })
                            2 -> {
                                val startCard = firstIndex?.let { list.getOrNull(it) }
                                if (startCard != null && currentSelected.isEmpty()) toggle(startCard)
                                swipe()
                            }
                        }
                    }
                },
        ) {
            cards.forEachIndexed { i, card ->
                key(card) {
                    val isSelected = card in selected
                    val lift by animateDpAsState(
                        if (isSelected) 0.dp else liftDp,
                        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                        label = "lift",
                    )
                    val targetX = startPx + spacingPx * i
                    val animX = remember { Animatable(totalWidthPx / 2f - cardWidthPx / 2f) }
                    val enter = remember { Animatable(0f) }
                    LaunchedEffect(targetX) {
                        animX.animateTo(targetX, tween(260, easing = FastOutSlowInEasing))
                    }
                    LaunchedEffect(Unit) { enter.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
                    val arcDp = ((i - mid) * (i - mid) * 0.35f).coerceAtMost(10f)
                    val enterOffsetPx = with(density) { (-160).dp.toPx() } * (1f - enter.value)
                    PlayingCard(
                        card = card,
                        width = cardWidth,
                        dimmed = card in dimmed,
                        highlighted = card in highlighted,
                        elevation = if (isSelected) 8.dp else 3.dp,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    animX.value.roundToInt(),
                                    (with(density) { (lift + arcDp.dp).toPx() } + enterOffsetPx).roundToInt(),
                                )
                            }
                            .graphicsLayer {
                                rotationZ = (i - mid) * 1.6f
                                alpha = 0.3f + 0.7f * enter.value
                            },
                    )
                }
            }
        }
    }
}
