package com.baiviet.game.phom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoins
import com.baiviet.game.phom.R

@Composable
fun PhomResultDialog(
    result: PhomResultUi,
    onPlayAgain: () -> Unit,
    onReview: () -> Unit,
    onExit: () -> Unit,
    notes: List<String> = emptyList(),
    canPlayAgain: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Color.Transparent,
        confirmButton = {},
        text = {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = result.title,
                        color = if (result.isWin) BvColors.Gold else BvColors.TextPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(8.dp))

                    val sign = if (result.deltaCoins >= 0) "+" else ""
                    Text(
                        text = "$sign${formatCoins(result.deltaCoins)} xu",
                        color = if (result.deltaCoins >= 0) BvColors.Gold else BvColors.LoseRed,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.rankedSeats.forEachIndexed { idx, seat ->
                            val pResult = result.results.firstOrNull { it.seat == seat }
                            val rankLabel = when {
                                pResult?.isU == true -> "Ù"
                                pResult?.isUKhan == true -> "Ù khan"
                                pResult?.isMom == true -> "Móm"
                                idx == 0 -> "Nhất"
                                idx == 1 -> "Nhì"
                                idx == 2 -> "Ba"
                                else -> "Bét"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (seat == 0) Color.White.copy(0.12f) else Color.Transparent)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val name = result.names.getOrNull(seat) ?: if (seat == 0) "Bạn" else "Máy $seat"
                                Text(
                                    text = "$name ($rankLabel)",
                                    color = if (seat == 0) BvColors.Gold else BvColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (seat == 0) FontWeight.Bold else FontWeight.Normal,
                                )
                                Text(
                                    text = if (pResult?.isMom == true) "Móm" else "${pResult?.points ?: 0} điểm",
                                    color = BvColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                result.deltas.getOrNull(seat)?.let { d ->
                                    Text(
                                        text = (if (d > 0) "+" else "") + formatCoins(d),
                                        color = if (d >= 0) BvColors.Gold else BvColors.LoseRed,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    notes.forEach {
                        Text(it, color = BvColors.Amber, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ActionButton(
                            text = stringResource(R.string.phom_review),
                            onClick = onReview,
                            style = ActionStyle.SECONDARY,
                            modifier = Modifier.weight(1f),
                        )
                        ActionButton(
                            text = if (canPlayAgain) stringResource(R.string.phom_replay) else "Rời bàn",
                            onClick = if (canPlayAgain) onPlayAgain else onExit,
                            style = ActionStyle.PRIMARY,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun PhomExitDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.phom_exit_confirm_title), color = BvColors.Gold) },
        text = { Text(stringResource(R.string.phom_exit_confirm_msg), color = BvColors.TextPrimary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.phom_exit_confirm_ok), color = BvColors.LoseRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.phom_exit_confirm_cancel), color = BvColors.TextSecondary)
            }
        },
    )
}

@Composable
fun PhomReviewDialog(state: PhomUiState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.phom_close), color = BvColors.Gold)
            }
        },
        title = { Text(stringResource(R.string.phom_review), color = BvColors.Gold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.seats.forEach { seatUi ->
                    val seat = seatUi.seat
                    val melds = state.exposedMelds.getOrNull(seat).orEmpty()
                    val trash = state.discardPiles.getOrNull(seat).orEmpty()

                    Column {
                        Text(
                            text = if (seat == 0) "Bạn" else "Máy $seat",
                            color = BvColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        if (melds.isNotEmpty()) {
                            Text(
                                text = "Phỏm: " + melds.joinToString("; ") { it.description },
                                color = BvColors.Gold,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            Text("Không có phỏm (Móm)", color = BvColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        if (trash.isNotEmpty()) {
                            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                trash.forEach { card ->
                                    PlayingCard(card = card, width = 36.dp)
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}
