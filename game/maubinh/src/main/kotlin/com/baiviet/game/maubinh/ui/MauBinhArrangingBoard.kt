package com.baiviet.game.maubinh.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baiviet.core.cards.Card
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.theme.BvColors

/**
 * Bảng xếp 3 chi tương tác cho người chơi trong giai đoạn ARRANGING.
 */
@Composable
fun MauBinhArrangingBoard(
    state: MauBinhUiState,
    onIntent: (MauBinhIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardWidth = 46.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC0B1E13))
            .border(1.dp, Color(0x40D4AF37), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. Thanh trạng thái & Đồng hồ 60s
        ArrangingHeader(
            state = state,
            onIntent = onIntent,
        )

        Spacer(Modifier.height(8.dp))

        // 2. Chi 3 (3 lá)
        ChiSlotRow(
            title = "Chi 3 (Chi cuối)",
            maxCards = 3,
            cards = state.chi3Cards,
            handDesc = state.chi3Hand?.description,
            selectedCard = state.selectedCard,
            cardWidth = cardWidth,
            chiIndex = 3,
            onCardClick = { card -> handleCardClick(card, 3, state, onIntent) },
            onEmptySlotClick = { handleEmptySlotClick(3, state, onIntent) },
        )

        Spacer(Modifier.height(6.dp))

        // 3. Chi 2 (5 lá)
        ChiSlotRow(
            title = "Chi 2 (Chi giữa)",
            maxCards = 5,
            cards = state.chi2Cards,
            handDesc = state.chi2Hand?.description,
            selectedCard = state.selectedCard,
            cardWidth = cardWidth,
            chiIndex = 2,
            onCardClick = { card -> handleCardClick(card, 2, state, onIntent) },
            onEmptySlotClick = { handleEmptySlotClick(2, state, onIntent) },
        )

        Spacer(Modifier.height(6.dp))

        // 4. Chi 1 (5 lá - phải mạnh nhất)
        ChiSlotRow(
            title = "Chi 1 (Chi đầu - Mạnh nhất)",
            maxCards = 5,
            cards = state.chi1Cards,
            handDesc = state.chi1Hand?.description,
            selectedCard = state.selectedCard,
            cardWidth = cardWidth,
            chiIndex = 1,
            onCardClick = { card -> handleCardClick(card, 1, state, onIntent) },
            onEmptySlotClick = { handleEmptySlotClick(1, state, onIntent) },
        )

        // 5. Lá bài chưa xếp (nếu có)
        AnimatedVisibility(visible = state.unassignedCards.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Bài chưa xếp (${state.unassignedCards.size} lá):",
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.unassignedCards.forEach { card ->
                        PlayingCard(
                            card = card,
                            width = 40.dp,
                            highlighted = state.selectedCard == card,
                            modifier = Modifier.clickable {
                                onIntent(MauBinhIntent.SelectCard(card))
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 6. Thanh công cụ thao tác
        ArrangingActionButtons(
            state = state,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun ArrangingHeader(
    state: MauBinhUiState,
    onIntent: (MauBinhIntent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Đèn báo hợp lệ / Binh lủng
        val badgeColor by animateColorAsState(
            targetValue = when {
                !state.isFull13 -> Color(0xFFF59E0B)
                state.isFoul -> Color(0xFFEF4444)
                else -> Color(0xFF10B981)
            },
            label = "badgeColor",
        )

        val statusText = when {
            !state.isFull13 -> "Chưa đủ 13 lá (${state.chi1Cards.size + state.chi2Cards.size + state.chi3Cards.size}/13)"
            state.isFoul -> "⚠️ BINH LỦNG! (Chi 1 ≥ Chi 2 ≥ Chi 3)"
            else -> "✓ HỢP LỆ"
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = badgeColor.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor),
        ) {
            Text(
                text = statusText,
                color = badgeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        // Đồng hồ đếm ngược 60s
        val timerColor = if (state.timeLeftSeconds <= 10) Color(0xFFEF4444) else Color(0xFFF5C451)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x40000000),
            border = androidx.compose.foundation.BorderStroke(1.dp, timerColor),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "⏱ ${state.timeLeftSeconds}s",
                    color = timerColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun ChiSlotRow(
    title: String,
    maxCards: Int,
    cards: List<Card>,
    handDesc: String?,
    selectedCard: Card?,
    cardWidth: Dp,
    chiIndex: Int,
    onCardClick: (Card) -> Unit,
    onEmptySlotClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33000000))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Nhãn chi + loại bài
        Column(modifier = Modifier.width(130.dp)) {
            Text(
                text = title,
                color = Color(0xFFD4AF37),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = handDesc ?: "Chưa đủ lá",
                color = if (handDesc != null) Color.White else Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        // Các slot bài
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until maxCards) {
                if (i < cards.size) {
                    val card = cards[i]
                    PlayingCard(
                        card = card,
                        width = cardWidth,
                        highlighted = selectedCard == card,
                        modifier = Modifier.clickable { onCardClick(card) },
                    )
                } else {
                    // Ô trống chờ xếp
                    Box(
                        modifier = Modifier
                            .size(cardWidth, cardWidth * 1.4f)
                            .clip(RoundedCornerShape(cardWidth * 0.09f))
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color(0x44D4AF37), RoundedCornerShape(cardWidth * 0.09f))
                            .clickable { onEmptySlotClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+",
                            color = Color(0x66FFFFFF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrangingActionButtons(
    state: MauBinhUiState,
    onIntent: (MauBinhIntent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Nút Xếp gợi ý
        OutlinedButton(
            onClick = { onIntent(MauBinhIntent.AutoArrange) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF38BDF8),
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("✨ Xếp gợi ý", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        // Nút Xếp lại (Reset)
        OutlinedButton(
            onClick = { onIntent(MauBinhIntent.ResetArrangement) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF94A3B8),
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B)),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("↺ Xếp lại", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        // Nút Xếp xong (Submit)
        val canSubmit = state.isFull13 && !state.isFoul
        Button(
            onClick = { onIntent(MauBinhIntent.Submit) },
            modifier = Modifier.weight(1.2f),
            enabled = canSubmit,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981),
                disabledContainerColor = Color(0xFF334155),
            ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                text = "✓ Xếp xong",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (canSubmit) Color.White else Color(0xFF94A3B8),
            )
        }
    }
}

private fun handleCardClick(
    clickedCard: Card,
    chiIndex: Int,
    state: MauBinhUiState,
    onIntent: (MauBinhIntent) -> Unit,
) {
    val selected = state.selectedCard
    if (selected == null) {
        onIntent(MauBinhIntent.SelectCard(clickedCard))
    } else if (selected == clickedCard) {
        // Bỏ chọn
        onIntent(MauBinhIntent.SelectCard(clickedCard))
    } else {
        // Hoán đổi 2 lá
        onIntent(MauBinhIntent.SwapCards(selected, clickedCard))
    }
}

private fun handleEmptySlotClick(
    targetChi: Int,
    state: MauBinhUiState,
    onIntent: (MauBinhIntent) -> Unit,
) {
    val selected = state.selectedCard
    if (selected != null) {
        onIntent(MauBinhIntent.AssignCardToChi(selected, targetChi))
    }
}
