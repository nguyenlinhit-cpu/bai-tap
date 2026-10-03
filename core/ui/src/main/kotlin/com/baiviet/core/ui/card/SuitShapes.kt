package com.baiviet.core.ui.card

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.baiviet.core.cards.Suit

/**
 * Hình chất bài vẽ vector trong hộp đơn vị [0,1]×[0,1] — sắc nét ở mọi mật độ điểm ảnh.
 */
object SuitShapes {

    val heart: Path = Path().apply {
        moveTo(0.5f, 0.93f)
        cubicTo(0.06f, 0.62f, -0.02f, 0.32f, 0.2f, 0.13f)
        cubicTo(0.36f, 0.0f, 0.5f, 0.1f, 0.5f, 0.26f)
        cubicTo(0.5f, 0.1f, 0.64f, 0.0f, 0.8f, 0.13f)
        cubicTo(1.02f, 0.32f, 0.94f, 0.62f, 0.5f, 0.93f)
        close()
    }

    val diamond: Path = Path().apply {
        moveTo(0.5f, 0.02f)
        quadraticTo(0.7f, 0.3f, 0.9f, 0.5f)
        quadraticTo(0.7f, 0.7f, 0.5f, 0.98f)
        quadraticTo(0.3f, 0.7f, 0.1f, 0.5f)
        quadraticTo(0.3f, 0.3f, 0.5f, 0.02f)
        close()
    }

    val spade: Path = Path().apply {
        moveTo(0.5f, 0.03f)
        cubicTo(0.96f, 0.36f, 1.02f, 0.62f, 0.78f, 0.74f)
        cubicTo(0.64f, 0.81f, 0.55f, 0.75f, 0.53f, 0.68f)
        quadraticTo(0.56f, 0.88f, 0.72f, 0.97f)
        lineTo(0.28f, 0.97f)
        quadraticTo(0.44f, 0.88f, 0.47f, 0.68f)
        cubicTo(0.45f, 0.75f, 0.36f, 0.81f, 0.22f, 0.74f)
        cubicTo(-0.02f, 0.62f, 0.04f, 0.36f, 0.5f, 0.03f)
        close()
    }

    val club: Path = Path().apply {
        addOval(Rect(center = Offset(0.5f, 0.28f), radius = 0.21f))
        addOval(Rect(center = Offset(0.26f, 0.57f), radius = 0.21f))
        addOval(Rect(center = Offset(0.74f, 0.57f), radius = 0.21f))
        addRect(Rect(0.44f, 0.3f, 0.56f, 0.7f))
        moveTo(0.5f, 0.55f)
        quadraticTo(0.54f, 0.86f, 0.7f, 0.97f)
        lineTo(0.3f, 0.97f)
        quadraticTo(0.46f, 0.86f, 0.5f, 0.55f)
        close()
    }

    fun pathOf(suit: Suit): Path = when (suit) {
        Suit.HEART -> heart
        Suit.DIAMOND -> diamond
        Suit.SPADE -> spade
        Suit.CLUB -> club
    }
}

/** Vẽ chất bài có tâm [center], cạnh [size] px; [flipped] = xoay 180° (nửa dưới lá bài). */
fun DrawScope.drawSuit(suit: Suit, center: Offset, size: Float, color: Color, flipped: Boolean = false) {
    val path = SuitShapes.pathOf(suit)
    translate(center.x - size / 2, center.y - size / 2) {
        rotate(if (flipped) 180f else 0f, pivot = Offset(size / 2, size / 2)) {
            scale(size, size, pivot = Offset.Zero) {
                drawPath(path, color)
            }
        }
    }
}
