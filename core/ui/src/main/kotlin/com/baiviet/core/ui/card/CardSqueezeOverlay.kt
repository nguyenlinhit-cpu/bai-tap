package com.baiviet.core.ui.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.baiviet.core.cards.Card
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.theme.BvColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Màn nặn bài tương tác (Card Squeeze).
 *
 * Cho phép người chơi vuốt kéo lá bài che phía trên để hé lộ từ từ lá bài phía dưới,
 * tạo cảm giác hồi hộp, kịch tính đặc trưng của Ba Cây, Xì Dách, Liêng.
 */
@Composable
fun CardSqueezeDialog(
    card: Card,
    onRevealed: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "Nặn bài hồi hộp",
    subtitle: String = "Kéo vuốt lá bài lên trên hoặc sang bên để hé điểm",
) {
    val coroutineScope = rememberCoroutineScope()
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var isFullyRevealed by remember { mutableStateOf(false) }

    val maxDrag = 320f

    fun checkReveal() {
        val totalDist = kotlin.math.hypot(dragX.toDouble(), dragY.toDouble()).toFloat()
        if (totalDist > maxDrag * 0.75f && !isFullyRevealed) {
            isFullyRevealed = true
            onRevealed()
        }
    }

    Dialog(
        onDismissRequest = {
            if (isFullyRevealed) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            GlassPanel(
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                strong = true,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = BvColors.Gold,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )

                    Spacer(Modifier.height(24.dp))

                    // Khu vực nặn bài
                    Box(
                        modifier = Modifier
                            .size(width = 180.dp, height = 255.dp)
                            .shadow(16.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Lá bài thật bên dưới (mặt ngửa)
                        PlayingCard(
                            card = card,
                            width = 180.dp,
                            faceUp = true,
                        )

                        // Lá bài che phía trên (mặt úp), di chuyển khi vuốt
                        if (!isFullyRevealed) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = dragX.roundToInt(),
                                            y = dragY.roundToInt(),
                                        )
                                    }
                                    .size(width = 180.dp, height = 255.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragX = (dragX + dragAmount.x).coerceIn(-maxDrag, maxDrag)
                                                dragY = (dragY + dragAmount.y).coerceIn(-maxDrag, 0f)
                                                checkReveal()
                                            },
                                            onDragEnd = {
                                                checkReveal()
                                            },
                                        )
                                    },
                            ) {
                                PlayingCard(
                                    card = null,
                                    width = 180.dp,
                                    faceUp = false,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ActionButton(
                            text = if (isFullyRevealed) "Xong" else "Mở luôn",
                            onClick = {
                                isFullyRevealed = true
                                onRevealed()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            style = ActionStyle.PRIMARY,
                        )
                    }
                }
            }
        }
    }
}
