package com.baiviet.core.ui.card

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.baiviet.core.ui.audio.GameAudio
import com.baiviet.core.ui.audio.Sfx
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.core.ui.theme.BvColors

/** Kiểu lưng bài. */
enum class CardBackDesign(val base: Color, val accent: Color) {
    CLASSIC_RED(Color(0xFF9B1C2C), Color(0xFFF5C451)),
    ROYAL_BLUE(Color(0xFF1C3D8F), Color(0xFFE8D7A0)),
    JADE(Color(0xFF0F6B5A), Color(0xFFF5C451)),
}

/** Tùy chọn hiển thị lá bài lấy từ Cài đặt. */
@Immutable
data class CardStyle(
    val fourColor: Boolean = false,
    val large: Boolean = false,
    val back: CardBackDesign = CardBackDesign.CLASSIC_RED,
) {
    fun suitColor(suit: Suit): Color = when (suit) {
        Suit.HEART -> BvColors.SuitRed
        Suit.DIAMOND -> if (fourColor) BvColors.SuitBlue else BvColors.SuitRed
        Suit.CLUB -> if (fourColor) BvColors.SuitGreen else BvColors.SuitBlack
        Suit.SPADE -> BvColors.SuitBlack
    }
}

val LocalCardStyle = staticCompositionLocalOf { CardStyle() }

/** Tỉ lệ cao/rộng của lá bài. */
const val CARD_ASPECT = 1.42f

/**
 * Lá bài vẽ vector bằng Canvas. [card] = null hoặc [faceUp] = false → vẽ lưng bài.
 *
 * Lật bài: đổi [faceUp] sẽ xoay `rotationY` quanh trục dọc, đổi mặt ở 90°.
 */
@Composable
fun PlayingCard(
    card: Card?,
    width: Dp,
    modifier: Modifier = Modifier,
    faceUp: Boolean = true,
    dimmed: Boolean = false,
    highlighted: Boolean = false,
    elevation: Dp = 3.dp,
) {
    val style = LocalCardStyle.current
    val rotation by animateFloatAsState(
        targetValue = if (faceUp && card != null) 0f else 180f,
        animationSpec = tween(380),
        label = "flip",
    )
    val shownFace = faceUp && card != null
    var lastFace by remember { mutableStateOf(shownFace) }
    LaunchedEffect(shownFace) {
        if (shownFace && !lastFace) GameAudio.play(Sfx.FLIP)
        lastFace = shownFace
    }
    val measurer = rememberTextMeasurer()
    val shape = remember(width) { RoundedCornerShape(width * 0.09f) }
    val desc = if (card != null && faceUp) card.viFullName else "Lá bài úp"
    Box(
        modifier = modifier
            .size(width, width * CARD_ASPECT)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .shadow(elevation, shape, clip = false)
            .semantics { contentDescription = desc },
    ) {
        Canvas(Modifier.size(width, width * CARD_ASPECT)) {
            val showFace = rotation <= 90f && card != null
            if (showFace) {
                drawFace(card!!, style, measurer)
            } else {
                // Lưng bài đối xứng nên không cần lật ngược
                drawBack(style.back)
            }
            if (highlighted) {
                drawRoundRect(
                    color = BvColors.Teal,
                    cornerRadius = CornerRadius(size.width * 0.09f),
                    style = Stroke(width = size.width * 0.05f),
                )
            }
            if (dimmed) {
                drawRoundRect(color = Color(0x88000000), cornerRadius = CornerRadius(size.width * 0.09f))
            }
        }
    }
}

// ───────────────────────── Mặt bài ─────────────────────────

private fun DrawScope.drawFace(card: Card, style: CardStyle, measurer: TextMeasurer) {
    val w = size.width
    val h = size.height
    val radius = CornerRadius(w * 0.09f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(BvColors.CardFace, BvColors.CardFaceEdge)),
        cornerRadius = radius,
    )
    drawRoundRect(color = BvColors.CardBorder, cornerRadius = radius, style = Stroke(width = w * 0.012f))

    val color = style.suitColor(card.suit)
    drawIndex(card, color, measurer, flipped = false)
    rotate(180f) { drawIndex(card, color, measurer, flipped = true) }

    when (card.rank) {
        Rank.JACK, Rank.QUEEN, Rank.KING -> drawCourt(card, color, measurer)
        Rank.ACE -> drawSuit(card.suit, Offset(w / 2, h / 2), w * 0.46f, color)
        else -> drawPips(card, color)
    }
}

private fun DrawScope.drawIndex(card: Card, color: Color, measurer: TextMeasurer, flipped: Boolean) {
    val w = size.width
    val label = card.rank.label
    val fontPx = w * (if (label.length > 1) 0.24f else 0.27f)
    val layout = measurer.measure(
        text = label,
        style = TextStyle(
            color = color,
            fontSize = (fontPx / density).sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
        ),
    )
    val cx = w * 0.15f
    drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, w * 0.03f))
    val suitSize = w * 0.17f
    drawSuit(card.suit, Offset(cx, w * 0.03f + layout.size.height + suitSize * 0.45f), suitSize, color)
}

/** Vị trí chấm chất (cột 0..1, hàng 0..1) cho lá 2–10. */
private fun pipLayout(rank: Rank): List<Pair<Float, Float>> {
    val third = 1f / 3f
    val sides = listOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f)
    return when (rank) {
        Rank.TWO -> listOf(0.5f to 0f, 0.5f to 1f)
        Rank.THREE -> listOf(0.5f to 0f, 0.5f to 0.5f, 0.5f to 1f)
        Rank.FOUR -> sides
        Rank.FIVE -> sides + (0.5f to 0.5f)
        Rank.SIX -> sides + listOf(0f to 0.5f, 1f to 0.5f)
        Rank.SEVEN -> sides + listOf(0f to 0.5f, 1f to 0.5f, 0.5f to 0.25f)
        Rank.EIGHT -> sides + listOf(0f to 0.5f, 1f to 0.5f, 0.5f to 0.25f, 0.5f to 0.75f)
        Rank.NINE -> sides + listOf(0f to third, 1f to third, 0f to 2 * third, 1f to 2 * third, 0.5f to 0.5f)
        Rank.TEN -> sides + listOf(
            0f to third, 1f to third, 0f to 2 * third, 1f to 2 * third,
            0.5f to third / 2, 0.5f to 1f - third / 2,
        )
        else -> emptyList()
    }
}

private fun DrawScope.drawPips(card: Card, color: Color) {
    val w = size.width
    val h = size.height
    val left = w * 0.32f
    val right = w * 0.68f
    val top = h * 0.2f
    val bottom = h * 0.8f
    val pip = w * 0.2f
    for ((cx, cy) in pipLayout(card.rank)) {
        val x = left + (right - left) * cx
        val y = top + (bottom - top) * cy
        drawSuit(card.suit, Offset(x, y), pip, color, flipped = cy > 0.5f)
    }
}

/** J/Q/K: khung vàng, vương miện/chùm lông tự vẽ, chữ lớn, đối xứng hai nửa. */
private fun DrawScope.drawCourt(card: Card, color: Color, measurer: TextMeasurer) {
    val w = size.width
    val h = size.height
    val frameLeft = w * 0.26f
    val frameTop = h * 0.13f
    val frameSize = Size(w * 0.48f, h * 0.74f)
    val tint = color.copy(alpha = 0.1f)
    drawRoundRect(tint, Offset(frameLeft, frameTop), frameSize, CornerRadius(w * 0.04f))
    drawRoundRect(
        color = BvColors.GoldDeep,
        topLeft = Offset(frameLeft, frameTop),
        size = frameSize,
        cornerRadius = CornerRadius(w * 0.04f),
        style = Stroke(width = w * 0.018f),
    )
    // đường chia đôi chéo như bài thật
    drawLine(
        color = BvColors.GoldDeep.copy(alpha = 0.6f),
        start = Offset(frameLeft, frameTop + frameSize.height),
        end = Offset(frameLeft + frameSize.width, frameTop),
        strokeWidth = w * 0.008f,
    )
    drawCourtHalf(card, color, measurer, frameLeft, frameTop, frameSize)
    rotate(180f) { drawCourtHalf(card, color, measurer, frameLeft, frameTop, frameSize) }
}

private fun DrawScope.drawCourtHalf(
    card: Card,
    color: Color,
    measurer: TextMeasurer,
    frameLeft: Float,
    frameTop: Float,
    frameSize: Size,
) {
    val w = size.width
    val cx = frameLeft + frameSize.width / 2
    val emblemTop = frameTop + frameSize.height * 0.06f
    val emblemW = frameSize.width * 0.62f
    val emblemH = frameSize.height * 0.15f
    val gold = BvColors.Gold
    when (card.rank) {
        Rank.KING -> drawPath(crownPath(cx, emblemTop, emblemW, emblemH, points = 5), gold)
        Rank.QUEEN -> drawPath(crownPath(cx, emblemTop, emblemW * 0.85f, emblemH, points = 3), gold)
        else -> drawPath(plumePath(cx, emblemTop, emblemW * 0.6f, emblemH * 1.1f), gold)
    }
    drawPath(
        when (card.rank) {
            Rank.KING -> crownPath(cx, emblemTop, emblemW, emblemH, points = 5)
            Rank.QUEEN -> crownPath(cx, emblemTop, emblemW * 0.85f, emblemH, points = 3)
            else -> plumePath(cx, emblemTop, emblemW * 0.6f, emblemH * 1.1f)
        },
        color = color,
        style = Stroke(width = w * 0.01f),
    )
    val layout = measurer.measure(
        text = card.rank.label,
        style = TextStyle(
            color = color,
            fontSize = (w * 0.3f / density).sp,
            fontWeight = FontWeight.Black,
        ),
    )
    val textTop = emblemTop + emblemH + frameSize.height * 0.01f
    drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, textTop))
    drawSuit(
        card.suit,
        Offset(frameLeft + frameSize.width * 0.2f, frameTop + frameSize.height * 0.12f),
        w * 0.11f,
        color,
    )
}

private fun crownPath(cx: Float, top: Float, width: Float, height: Float, points: Int): Path = Path().apply {
    val left = cx - width / 2
    val bottom = top + height
    moveTo(left, bottom)
    val step = width / (points * 2)
    for (i in 0 until points * 2) {
        val x = left + step * (i + 1)
        val y = if (i % 2 == 0) top else top + height * 0.55f
        lineTo(x, y)
    }
    lineTo(left + width, bottom)
    close()
}

private fun plumePath(cx: Float, top: Float, width: Float, height: Float): Path = Path().apply {
    moveTo(cx, top)
    cubicTo(cx + width * 0.7f, top + height * 0.3f, cx + width * 0.4f, top + height * 0.8f, cx, top + height)
    cubicTo(cx - width * 0.4f, top + height * 0.8f, cx - width * 0.7f, top + height * 0.3f, cx, top)
    close()
}

// ───────────────────────── Lưng bài ─────────────────────────

private fun DrawScope.drawBack(design: CardBackDesign) {
    val w = size.width
    val h = size.height
    val radius = CornerRadius(w * 0.09f)
    drawRoundRect(color = Color.White, cornerRadius = radius)
    val inset = w * 0.06f
    val innerSize = Size(w - inset * 2, h - inset * 2)
    drawRoundRect(
        brush = Brush.linearGradient(listOf(design.base, design.base.copy(red = design.base.red * 0.6f))),
        topLeft = Offset(inset, inset),
        size = innerSize,
        cornerRadius = CornerRadius(w * 0.05f),
    )
    // Hoa văn mắt cáo
    val step = w * 0.14f
    val lineColor = design.accent.copy(alpha = 0.28f)
    clipRect(inset, inset, w - inset, h - inset) {
        var x = -h
        while (x < w + h) {
            drawLine(lineColor, Offset(x, inset), Offset(x + innerSize.height, h - inset), strokeWidth = w * 0.01f)
            drawLine(lineColor, Offset(x + innerSize.height, inset), Offset(x, h - inset), strokeWidth = w * 0.01f)
            x += step
        }
    }
    // Che phần hoa văn tràn ra viền trắng
    drawRoundRect(color = Color.White, cornerRadius = radius, style = Stroke(width = inset * 2))
    drawRoundRect(color = BvColors.CardBorder, cornerRadius = radius, style = Stroke(width = w * 0.012f))
    // Huy hiệu giữa: hình thoi vàng + vòng tròn
    val c = Offset(w / 2, h / 2)
    val d = w * 0.22f
    val rhombus = Path().apply {
        moveTo(c.x, c.y - d * 1.3f)
        lineTo(c.x + d, c.y)
        lineTo(c.x, c.y + d * 1.3f)
        lineTo(c.x - d, c.y)
        close()
    }
    drawPath(rhombus, design.base)
    drawPath(rhombus, design.accent, style = Stroke(width = w * 0.025f))
    drawCircle(design.accent, radius = d * 0.42f, center = c, style = Stroke(width = w * 0.02f))
    drawSuit(Suit.SPADE, c, d * 0.5f, design.accent)
}
