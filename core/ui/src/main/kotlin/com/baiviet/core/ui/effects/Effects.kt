package com.baiviet.core.ui.effects

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.baiviet.core.ui.audio.GameAudio
import com.baiviet.core.ui.audio.Sfx
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baiviet.core.ui.table.CoinIcon
import com.baiviet.core.ui.theme.BvColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Banner sự kiện lớn (Chặt heo, Tới trắng…). */
@Immutable
data class BannerData(val id: Int, val title: String, val subtitle: String? = null, val positive: Boolean = true)

/**
 * Banner lớn ở giữa màn: phóng to bật nảy + particle tung ra.
 */
@Composable
fun BigBanner(banner: BannerData?, modifier: Modifier = Modifier) {
    if (banner == null) return
    key(banner.id) {
        val scale = remember { Animatable(0.3f) }
        val alpha = remember { Animatable(0f) }
        val haptic = LocalHapticFeedback.current
        LaunchedEffect(Unit) {
            GameAudio.play(if (banner.positive) Sfx.BIG_EVENT else Sfx.LOSE)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            alpha.animateTo(1f, tween(120))
            scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
        }
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ParticleBurst(
                colors = if (banner.positive) {
                    listOf(BvColors.Gold, Color(0xFFFFF1B8), BvColors.Teal, Color.White)
                } else {
                    listOf(BvColors.Red, BvColors.Amber, Color.White)
                },
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                },
            ) {
                Text(
                    text = banner.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            if (banner.positive) {
                                listOf(Color(0xFFFFF4C2), BvColors.Gold, Color(0xFFC98A1B))
                            } else {
                                listOf(Color(0xFFFFC2C2), BvColors.Red, Color(0xFF8B1111))
                            },
                        ),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 6f), 12f),
                    ),
                )
                banner.subtitle?.let {
                    Text(
                        text = it,
                        color = BvColors.Ivory,
                        style = MaterialTheme.typography.titleLarge.copy(
                            shadow = Shadow(Color.Black, Offset(0f, 3f), 6f),
                        ),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private data class Particle(val angle: Float, val speed: Float, val size: Float, val color: Color, val spin: Float)

/** Hạt tung ra từ tâm, có trọng lực, mờ dần (~1.2 giây). */
@Composable
fun ParticleBurst(colors: List<Color>, modifier: Modifier = Modifier, count: Int = 70) {
    val particles = remember {
        val r = Random(System.nanoTime())
        List(count) {
            Particle(
                angle = (r.nextFloat() * 2 * PI).toFloat(),
                speed = 300f + r.nextFloat() * 700f,
                size = 4f + r.nextFloat() * 8f,
                color = colors[r.nextInt(colors.size)],
                spin = r.nextFloat(),
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(1300, easing = LinearEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val time = t.value * 1.3f
        val c = center
        for (p in particles) {
            val x = c.x + cos(p.angle) * p.speed * time
            val y = c.y + sin(p.angle) * p.speed * time + 900f * time * time
            val a = (1f - t.value).coerceIn(0f, 1f)
            val w = p.size * (0.6f + 0.4f * sin((time * 12 + p.spin * 6).toDouble()).toFloat())
            drawRect(p.color.copy(alpha = a), topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(w, p.size))
        }
    }
}

/** Một đồng xu bay từ [from] tới [to] (tọa độ tương đối 0..1 của khung chứa). */
@Immutable
data class CoinFlight(val id: Int, val from: Offset, val to: Offset, val delayMs: Int)

/** Lớp chip bay từ người thua sang người thắng. */
@Composable
fun CoinFlightLayer(flights: List<CoinFlight>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        flights.forEach { f ->
            key(f.id) {
                val p = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(f.delayMs.toLong())
                    p.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                }
                if (p.value > 0f && p.value < 1f) {
                    val x = (f.from.x + (f.to.x - f.from.x) * p.value) * w
                    // cung bay vồng lên
                    val arc = -sin(p.value * PI).toFloat() * h * 0.12f
                    val y = (f.from.y + (f.to.y - f.from.y) * p.value) * h + arc
                    CoinIcon(
                        size = 22.dp,
                        modifier = Modifier.offset { IntOffset((x - 11.dp.toPx()).roundToInt(), (y - 11.dp.toPx()).roundToInt()) },
                    )
                }
            }
        }
    }
}
