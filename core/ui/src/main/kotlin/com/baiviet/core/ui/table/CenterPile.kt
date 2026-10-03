package com.baiviet.core.ui.table

import com.baiviet.core.ui.audio.GameAudio
import com.baiviet.core.ui.audio.Sfx
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.baiviet.core.cards.Card
import com.baiviet.core.ui.card.PlayingCard
import kotlin.math.roundToInt

/**
 * Một bộ bài vừa đánh ra giữa bàn.
 *
 * @param from hướng ghế đã đánh (vector đơn vị trên màn hình, ví dụ dưới = (0, 1))
 * @param rotation góc xoay ngẫu nhiên ±8°
 * @param jitter lệch nhẹ để các bộ chồng lớp tự nhiên
 */
@Immutable
data class PlayedSet(
    val id: Int,
    val cards: List<Card>,
    val from: Offset,
    val rotation: Float,
    val jitter: Offset,
)

/** Bài trên bàn của một vòng. Đổi [roundId] → bài cũ trượt ra và mờ dần. */
@Immutable
data class CenterPileState(val roundId: Int, val sets: List<PlayedSet>)

/**
 * Tâm bàn: các bộ vừa đánh bay từ ghế vào giữa, xoay nhẹ, chồng lớp; vòng mới thì dọn bài.
 */
@Composable
fun CenterPile(state: CenterPileState, cardWidth: Dp, modifier: Modifier = Modifier) {
    val lastSetId = state.sets.lastOrNull()?.id
    LaunchedEffect(lastSetId) { if (lastSetId != null) GameAudio.play(Sfx.PLAY) }
    AnimatedContent(
        targetState = state,
        contentKey = { it.roundId },
        transitionSpec = {
            fadeIn(tween(150)) togetherWith
                (fadeOut(tween(350)) + slideOutHorizontally(tween(350)) { it / 3 })
        },
        modifier = modifier,
        label = "pile",
    ) { pile ->
        Box(contentAlignment = Alignment.Center) {
            val visible = pile.sets.takeLast(4)
            visible.forEachIndexed { idx, set ->
                key(set.id) {
                    val depth = visible.size - 1 - idx
                    PlayedSetView(set, cardWidth, alpha = if (depth == 0) 1f else 0.85f - depth * 0.12f)
                }
            }
        }
    }
}

@Composable
private fun PlayedSetView(set: PlayedSet, cardWidth: Dp, alpha: Float) {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
    val distancePx = with(density) { 260.dp.toPx() }
    val spacing = cardWidth * 0.42f
    val n = set.cards.size
    Box(
        modifier = Modifier
            .offset {
                val p = progress.value
                val jx = with(density) { set.jitter.x.dp.toPx() }
                val jy = with(density) { set.jitter.y.dp.toPx() }
                IntOffset(
                    (set.from.x * distancePx * (1 - p) + jx).roundToInt(),
                    (set.from.y * distancePx * (1 - p) + jy).roundToInt(),
                )
            }
            .graphicsLayer {
                rotationZ = set.rotation * progress.value
                this.alpha = alpha
                val s = 0.8f + 0.2f * progress.value
                scaleX = s
                scaleY = s
            },
        contentAlignment = Alignment.Center,
    ) {
        set.cards.forEachIndexed { i, card ->
            PlayingCard(
                card = card,
                width = cardWidth,
                modifier = Modifier.offset(x = spacing * (i - (n - 1) / 2f)),
                elevation = 4.dp,
            )
        }
    }
}
