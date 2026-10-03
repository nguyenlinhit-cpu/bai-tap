package com.baiviet.game.bacay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.baiviet.core.data.HouseRulesRepository
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.game.bacay.rules.BaCayMode
import com.baiviet.game.bacay.rules.BaCayRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BaCayRulesViewModel @Inject constructor(repo: HouseRulesRepository) : ViewModel() {
    val rules: StateFlow<BaCayRules> = repo.rules(BaCayRules.GAME_ID, BaCayRules.serializer(), BaCayRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BaCayRules.DEFAULT)
}

@Composable
fun BaCayRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: BaCayRulesViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BvColors.NavyDark)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(
                    glyph = "←",
                    contentDescription = "Quay lại",
                    onClick = onNavigateBack,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "LUẬT CHƠI BA CÂY (CÀO)",
                        style = MaterialTheme.typography.titleLarge,
                        color = BvColors.Gold,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Chuẩn bài cào 3 lá dân gian",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("1. Cách tính nút (Điểm)", style = MaterialTheme.typography.titleMedium, color = BvColors.Gold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "• Bộ bài 52 lá tiêu chuẩn, mỗi người nhận đúng 3 lá bài úp.\n" +
                                    "• Điểm lá: A = 1 điểm, 2–9 lấy theo số, 10-J-Q-K tính 0 điểm.\n" +
                                    "• Nút bài = (Tổng điểm 3 lá) % 10. Cao nhất là 9 nút, thấp nhất 0 nút (bù).",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("2. Ba Tiên (Ba Cào) & Sáp", style = MaterialTheme.typography.titleMedium, color = BvColors.Gold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "• Ba Tiên (Ba cào): 3 lá đều là hình người (J, Q, K). Lớn hơn tất cả các điểm từ 0 đến 9 nút!\n" +
                                    "• Sáp: 3 lá cùng số (AAA > KKK > ... > 222).\n" +
                                    "• Thứ tự hiện tại: ${if (rules.sapBeatsBaTien) "Sáp > Ba Tiên > Điểm nút" else "Ba Tiên > Sáp > Điểm nút"}.\n" +
                                    "• Trả thưởng Ba Tiên: ${if (rules.baTienPaysDouble) "Ăn gấp đôi (×2)" else "Ăn bình thường (1×)"}.",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("3. So sánh bằng điểm & Lá bài lớn nhất", style = MaterialTheme.typography.titleMedium, color = BvColors.Gold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "• Khi bằng điểm:\n" +
                                    "  ${if (rules.tieIsPush) "Hòa tiền (Luật nhà đang bật)" else "So lá bài mạnh nhất (Mặc định)"}.\n" +
                                    "• Thứ tự ưu tiên chất: ♦ Rô > ♥ Cơ > ♣ Chuồn > ♠ Bích.\n" +
                                    "• Cùng chất so giá trị lá: A > K > Q > J > 10 > … > 2 (Át Rô là lá bài mạnh nhất).",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("4. Chế độ chơi", style = MaterialTheme.typography.titleMedium, color = BvColors.Gold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (rules.gameMode == BaCayMode.WITH_DEALER) {
                                    "• Chơi có cái: Mỗi nhà con so bài riêng với nhà cái. Thắng ăn 1× cược, thua mất 1× cược."
                                } else {
                                    "• Ăn tất: Tất cả người chơi cược vào pot, người có bài lớn nhất ăn toàn bộ số tiền trên bàn."
                                },
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
