package com.baiviet.game.maubinh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.game.maubinh.rules.MauBinhPairwiseComparison

/**
 * Hộp thoại hỏi người chơi có muốn Báo Mậu Binh Tới Trắng không ngay khi chia bài.
 */
@Composable
fun MauBinhInstantWinDialog(
    state: MauBinhUiState,
    onIntent: (MauBinhIntent) -> Unit,
) {
    val iw = state.detectedInstantWin ?: return
    AlertDialog(
        onDismissRequest = { onIntent(MauBinhIntent.DismissInstantWinDialog) },
        title = {
            Text(
                text = "🎉 BÁO MẬU BINH TỚI TRẮNG?",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF5C451),
                fontSize = 18.sp,
            )
        },
        text = {
            Column {
                Text(
                    text = "Bài của bạn đạt tới trắng: ${iw.viName}!",
                    color = Color.White,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Nếu báo Mậu Binh, bạn sẽ thắng ngay mà không cần so chi với các đối thủ thông thường.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onIntent(MauBinhIntent.DeclareInstantWin) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            ) {
                Text("Báo Mậu Binh", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(MauBinhIntent.DismissInstantWinDialog) }) {
                Text("Tự xếp tay", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B),
    )
}

/**
 * Hộp thoại tổng kết kết quả ván Mậu Binh.
 */
@Composable
fun MauBinhResultDialog(
    result: MauBinhResultUi,
    onIntent: (MauBinhIntent) -> Unit,
    notes: List<String> = emptyList(),
    names: Map<Int, String> = emptyMap(),
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Tiêu đề Thắng / Thua
                val titleColor = if (result.isWin) Color(0xFFF5C451) else Color(0xFFEF4444)
                Text(
                    text = result.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = titleColor,
                )

                Spacer(Modifier.height(4.dp))

                // Tổng chi & Xu
                val sign = if (result.totalChi > 0) "+" else ""
                Text(
                    text = "$sign${result.totalChi} Chi  ($sign${result.deltaCoins} Xu)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (result.totalChi >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                )

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0x33FFFFFF))
                Spacer(Modifier.height(10.dp))

                // Tóm tắt đối đầu từng cặp
                Text(
                    text = "Kết quả đối đầu:",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(result.pairwiseComparisons) { pair ->
                        PairwiseRow(pair, names)
                    }
                }

                notes.forEach {
                    Text(it, color = Color(0xFFF59E0B), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(14.dp))

                // Các nút điều hướng
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { onIntent(MauBinhIntent.OpenPairwiseMatrix) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                    ) {
                        Text("Bảng chi tiết", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = { onIntent(MauBinhIntent.PlayAgain) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    ) {
                        Text("Chơi tiếp", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PairwiseRow(pair: MauBinhPairwiseComparison, names: Map<Int, String>) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x22FFFFFF),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${names.nameOf(pair.p1)} vs ${names.nameOf(pair.p2)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Text(
                    text = names.withNames(pair.summaryReason),
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }

            val sign = if (pair.netChiForP1 > 0) "+" else ""
            val chiColor = when {
                pair.netChiForP1 > 0 -> Color(0xFF10B981)
                pair.netChiForP1 < 0 -> Color(0xFFEF4444)
                else -> Color(0xFFF59E0B)
            }
            Text(
                text = "$sign${pair.netChiForP1} chi",
                color = chiColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}

/**
 * Hộp thoại bảng so chi đối đầu đầy đủ (Ma trận đối đầu).
 */
@Composable
fun MauBinhPairwiseMatrixDialog(
    comparisons: List<MauBinhPairwiseComparison>,
    onDismiss: () -> Unit,
    names: Map<Int, String> = emptyMap(),
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "BẢNG SO CHI CHI TIẾT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                )

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(comparisons) { pair ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33000000))
                                .padding(8.dp),
                        ) {
                            Text(
                                text = "Cặp đấu: ${names.nameOf(pair.p1)} và ${names.nameOf(pair.p2)}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF5C451),
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "• " + (pair.chi1Comparison?.description ?: "Chi 1: Hòa/Lủng"),
                                fontSize = 11.sp,
                                color = Color.White,
                            )
                            Text(
                                text = "• " + (pair.chi2Comparison?.description ?: "Chi 2: Hòa/Lủng"),
                                fontSize = 11.sp,
                                color = Color.White,
                            )
                            Text(
                                text = "• " + (pair.chi3Comparison?.description ?: "Chi 3: Hòa/Lủng"),
                                fontSize = 11.sp,
                                color = Color.White,
                            )
                            if (pair.isSap3ChiForP1 || pair.isSap3ChiForP2) {
                                Text(
                                    text = "★ Sập 3 chi (×2 điểm)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFEAB308),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            if (pair.isSapLangForP1 || pair.isSapLangForP2) {
                                Text(
                                    text = "★ Sập làng (×4 điểm)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFEC4899),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                ) {
                    Text("Đóng")
                }
            }
        }
    }
}

@Composable
fun MauBinhExitDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rời bàn chơi?", fontWeight = FontWeight.Bold) },
        text = { Text("Nếu rời bàn khi đang xếp bài hoặc so bài, bạn sẽ bị tính là thua cuộc.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            ) {
                Text("Rời bàn", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ở lại") }
        },
        containerColor = Color(0xFF1E293B),
    )
}

private fun Map<Int, String>.nameOf(id: PlayerId): String = this[id.seat] ?: "Ghế ${id.seat + 1}"

/** Thay mã ghế "P0", "P1"… trong câu mô tả của luật bằng tên người chơi. */
private fun Map<Int, String>.withNames(text: String): String =
    Regex("\\bP(\\d)\\b").replace(text) { m -> nameOf(PlayerId(m.groupValues[1].toInt())) }
