package com.baiviet.core.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.baiviet.core.ui.theme.BvColors

/** Màu nỉ mặt bàn. */
enum class FeltColor(val color: Color) {
    GREEN(BvColors.FeltGreen),
    RED(BvColors.FeltRed),
    BLUE(BvColors.FeltBlue),
}

/**
 * Nền bàn chơi: nền navy → tím than, mặt nỉ bo tròn có viền gỗ + chỉ vàng,
 * vignette ở rìa và spotlight nhẹ ở tâm.
 */
@Composable
fun TableFelt(
    felt: FeltColor,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(BvColors.NavyDark, BvColors.PurpleDark)))
            .drawBehind {
                val w = size.width
                val h = size.height
                val marginX = w * 0.035f
                val marginY = h * 0.07f
                val tableSize = Size(w - marginX * 2, h - marginY * 2)
                val radius = CornerRadius(tableSize.height / 2)
                val rim = h * 0.035f

                // Bóng đổ dưới bàn
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(marginX, marginY + rim * 0.6f),
                    size = tableSize,
                    cornerRadius = radius,
                )
                // Viền gỗ
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(BvColors.WoodLight, BvColors.WoodDark, BvColors.WoodLight)),
                    topLeft = Offset(marginX, marginY),
                    size = tableSize,
                    cornerRadius = radius,
                )
                // Mặt nỉ
                val feltTopLeft = Offset(marginX + rim, marginY + rim)
                val feltSize = Size(tableSize.width - rim * 2, tableSize.height - rim * 2)
                val feltRadius = CornerRadius(feltSize.height / 2)
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            lerp(felt.color, Color.White, 0.18f),
                            felt.color,
                            lerp(felt.color, Color.Black, 0.45f),
                        ),
                        center = Offset(w / 2, h * 0.45f),
                        radius = feltSize.width * 0.62f,
                    ),
                    topLeft = feltTopLeft,
                    size = feltSize,
                    cornerRadius = feltRadius,
                )
                // Chỉ vàng
                val inset = rim * 0.35f
                drawRoundRect(
                    color = BvColors.Gold.copy(alpha = 0.55f),
                    topLeft = feltTopLeft + Offset(inset, inset),
                    size = Size(feltSize.width - inset * 2, feltSize.height - inset * 2),
                    cornerRadius = CornerRadius(feltSize.height / 2 - inset),
                    style = Stroke(width = h * 0.003f),
                )
                // Spotlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(w / 2, h * 0.42f),
                        radius = h * 0.42f,
                    ),
                    radius = h * 0.42f,
                    center = Offset(w / 2, h * 0.42f),
                )
                // Vignette toàn màn
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                        center = Offset(w / 2, h / 2),
                        radius = w * 0.75f,
                    ),
                )
            },
        content = content,
    )
}
