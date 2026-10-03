package com.baiviet.game.lieng.ui

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
import com.baiviet.game.lieng.rules.LiengRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LiengRulesViewModel @Inject constructor(repo: HouseRulesRepository) : ViewModel() {
    val rules: StateFlow<LiengRules> = repo.rules(LiengRules.GAME_ID, LiengRules.serializer(), LiengRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiengRules.DEFAULT)
}

@Composable
fun LiengRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: LiengRulesViewModel = hiltViewModel(),
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
                        text = "LUẬT CHƠI LIÊNG",
                        style = MaterialTheme.typography.titleLarge,
                        color = BvColors.Gold,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Chuẩn bài cào tố dân gian",
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
                            Text("1. Thứ tự bài trong Liêng", style = MaterialTheme.typography.titleMedium, color = BvColors.Gold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Mạnh nhất đến yếu nhất:\n" +
                                    "1. Sáp: 3 lá cùng số (AAA > KKK > ... > 222).\n" +
                                    "2. Liêng: 3 lá liên tiếp không cần cùng chất (Q-K-A > J-Q-K > ... > A-2-3; K-A-2 sai).\n" +
                                    "3. Ảnh (3 Tây): 3 lá đều là J, Q, K (không phải Sáp hay Liêng).\n" +
                                    "4. Điểm: A = 1, 2–9 theo số, 10/J/Q/K = 0. Tổng % 10 (9 điểm cao nhất, 0 điểm thấp nhất).",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("2. So sánh chất & Luật đang áp dụng", style = MaterialTheme.typography.titleMedium, color = BvColors.Gold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "• Thứ tự chất: ${if (rules.suitOrderCoOverRo) "♥ Cơ > ♦ Rô > ♣ Chuồn > ♠ Bích" else "♦ Rô > ♥ Cơ > ♣ Chuồn > ♠ Bích"}.\n" +
                                    "• Quy tắc so: ${if (rules.rankBeforeSuit) "So số trước rồi so chất" else "So chất trước rồi so số"}.\n" +
                                    "• Trùng dây Liêng: ${if (rules.liengTieIsPush) "Hòa chia đều pot" else "So chất lá bài cao nhất"}.",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("3. Vòng cược tố & Pot", style = MaterialTheme.typography.titleMedium, color = BvColors.Gold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "• Mỗi người nộp tiền sàn (ante) vào pot trước khi chia bài.\n" +
                                    "• Tối đa ${rules.maxBettingRounds} vòng tố. Mỗi lượt có thể:\n" +
                                    "  - Xem (Check): Nhường lượt khi không ai tố.\n" +
                                    "  - Theo (Call): Bỏ thêm xu bằng mức cược cao nhất.\n" +
                                    "  - Tố (Raise): Cược thêm tiền nâng mức cược.\n" +
                                    "  - Úp (Fold): Bỏ bài và mất số tiền đã cược.\n" +
                                    "• Nếu mọi người đều úp chỉ còn 1 người: Người đó ăn toàn bộ pot mà không cần lật bài!",
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
