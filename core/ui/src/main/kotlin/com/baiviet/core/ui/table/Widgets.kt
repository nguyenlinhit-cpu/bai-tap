package com.baiviet.core.ui.table

import com.baiviet.core.ui.audio.GameAudio
import com.baiviet.core.ui.audio.Sfx
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoins
import com.baiviet.core.ui.theme.formatCoinsShort

/** Kiểu chữ số đều (tabular) cho số tiền. */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

/** Panel kính mờ: nền mờ, viền sáng 1dp, bo góc 20dp. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    strong: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        (if (strong) BvColors.NavyMid else BvColors.NavyDark).copy(alpha = if (strong) 0.94f else 0.72f),
                        BvColors.PurpleDark.copy(alpha = if (strong) 0.94f else 0.72f),
                    ),
                ),
            )
            .background(BvColors.GlassBg)
            .border(BorderStroke(1.dp, BvColors.GlassBorder), shape),
        content = content,
    )
}

/** Kiểu nút hành động. */
enum class ActionStyle { PRIMARY, SECONDARY, DANGER }

/** Nút hành động lớn (≥ 48dp), chỉ sáng khi hợp lệ. */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ActionStyle = ActionStyle.PRIMARY,
    pulse: Boolean = false,
) {
    val colors = when (style) {
        ActionStyle.PRIMARY -> listOf(Color(0xFFFFD873), BvColors.GoldDeep)
        ActionStyle.SECONDARY -> listOf(Color(0xFF3FE6CF), Color(0xFF14A08F))
        ActionStyle.DANGER -> listOf(Color(0xFFF87171), Color(0xFFB91C1C))
    }
    val textColor = if (style == ActionStyle.PRIMARY) BvColors.TextOnGold else Color.White
    val pulseScale = if (pulse && enabled) {
        val t = rememberInfiniteTransition(label = "pulse")
        val s by t.animateFloat(1f, 1.06f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "s")
        s
    } else {
        1f
    }
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .scale(pulseScale)
            .defaultMinSize(minWidth = 96.dp, minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .background(Brush.verticalGradient(colors))
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .clickable(enabled = enabled, role = Role.Button) {
                GameAudio.play(Sfx.TAP)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Nút tròn nhỏ trên thanh trên (thoát, ?, âm thanh). */
@Composable
fun RoundIconButton(
    glyph: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(BvColors.GlassBgStrong)
            .border(1.dp, BvColors.GlassBorder, CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription) {
                GameAudio.play(Sfx.TAP)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = BvColors.Ivory, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

/** Số xu đếm chạy khi thay đổi. */
@Composable
fun CoinCounter(
    amount: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    color: Color = BvColors.Gold,
    short: Boolean = false,
) {
    val anim = remember { Animatable(amount.toFloat()) }
    val shownAt = remember { System.currentTimeMillis() }
    LaunchedEffect(amount) {
        // Không kêu khi số dư vừa nạp lúc mở màn
        if (anim.targetValue != amount.toFloat() && System.currentTimeMillis() - shownAt > 800L) GameAudio.play(Sfx.CHIP)
        anim.animateTo(amount.toFloat(), tween(700))
    }
    val shown = anim.value.toLong()
    Text(
        text = if (short) formatCoinsShort(shown) else formatCoins(shown),
        modifier = modifier,
        style = style.merge(TabularNumbers),
        color = color,
        fontWeight = FontWeight.Bold,
    )
}

/** Đồng xu vàng vẽ bằng Canvas. */
@Composable
fun CoinIcon(size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE89A), BvColors.GoldDeep)), r)
        drawCircle(Color(0xFF9A7420), r * 0.72f, style = Stroke(width = r * 0.16f))
        drawCircle(Color(0xFFFFF3C4).copy(alpha = 0.7f), r * 0.22f, center = center - Offset(r * 0.3f, r * 0.3f))
    }
}

/** Trạng thái hiển thị của một ghế. */
data class SeatInfo(
    val name: String,
    val initial: String,
    val coins: Long,
    val cardCount: Int,
    val status: String? = null,
    val isTurn: Boolean = false,
    val isHuman: Boolean = false,
    val avatarColor: Color = BvColors.Teal,
    val highlight: Boolean = false,
)

/**
 * Ghế người chơi: avatar tròn có vòng đếm giờ (xanh → vàng → đỏ), tên, xu, số lá, nhãn trạng thái.
 *
 * @param timerProgress phần thời gian còn lại 0..1 (null = không đếm giờ)
 */
@Composable
fun SeatView(
    info: SeatInfo,
    timerProgress: Float?,
    modifier: Modifier = Modifier,
    showCardCount: Boolean = true,
    avatarSize: Dp = 56.dp,
) {
    val glow = if (info.isTurn) {
        val t = rememberInfiniteTransition(label = "glow")
        val a by t.animateFloat(0.35f, 0.9f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), label = "a")
        a
    } else {
        0f
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(avatarSize + 12.dp)) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2 + 1.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                if (glow > 0f) {
                    drawCircle(BvColors.Teal.copy(alpha = glow * 0.45f), radius = size.minDimension / 2)
                }
                drawArc(
                    color = Color.White.copy(alpha = 0.15f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
                )
                if (timerProgress != null) {
                    val p = timerProgress.coerceIn(0f, 1f)
                    val color = when {
                        p > 0.5f -> lerp(BvColors.Amber, BvColors.WinGreen, (p - 0.5f) * 2f)
                        else -> lerp(BvColors.Red, BvColors.Amber, p * 2f)
                    }
                    drawArc(
                        color = color,
                        startAngle = -90f, sweepAngle = 360f * p, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(info.avatarColor, lerp(info.avatarColor, Color.Black, 0.5f))),
                    )
                    .border(2.dp, if (info.isHuman) BvColors.Gold else Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(info.initial, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (showCardCount && info.cardCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayingCard(card = null, width = 22.dp, faceUp = false, elevation = 2.dp)
                    Text(
                        "${info.cardCount}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TabularNumbers,
                        // Nền tối để số không lẫn vào hoa văn lưng bài
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 2.dp),
                    )
                }
            }
        }
        GlassPanel(shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 2.dp)) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(info.name, color = BvColors.Ivory, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(11.dp)
                    Spacer(Modifier.width(3.dp))
                    CoinCounter(info.coins, style = MaterialTheme.typography.labelMedium, short = true)
                }
            }
        }
        info.status?.let {
            StatusChip(it, highlight = info.highlight, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (highlight) BvColors.Gold else Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = if (highlight) BvColors.TextOnGold else BvColors.Ivory,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

/** Lớp phủ tối toàn màn cho hộp thoại/bảng kết quả. */
@Composable
fun Scrim(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BvColors.Scrim)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier.clickable(enabled = false) {}),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Hàng nút cách đều. */
@Composable
fun ButtonRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        content()
    }
}
