package com.baiviet.game.xidach.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.baiviet.game.xidach.rules.XiDachRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class XiDachRulesViewModel @Inject constructor(repo: HouseRulesRepository) : ViewModel() {
    val rules: StateFlow<XiDachRules> = repo.rules(XiDachRules.GAME_ID, XiDachRules.serializer(), XiDachRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), XiDachRules.DEFAULT)
}

@Composable
fun XiDachRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: XiDachRulesViewModel = hiltViewModel(),
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
                        text = "LUẬT CHƠI XÌ DÁCH",
                        style = MaterialTheme.typography.titleLarge,
                        color = BvColors.Gold,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Chuẩn luật dân gian Việt Nam",
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
                    RuleSection(title = "1. Bộ bài & Chia bài") {
                        Text(
                            text = "• Bộ bài 52 lá tiêu chuẩn, từ 2 đến 4 người chơi.\n" +
                                "• Mỗi người (kể cả Nhà Cái) được chia 2 lá bài úp ban đầu.\n" +
                                "• Có thể rút thêm tối đa tới 5 lá bài.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                        )
                    }
                }

                item {
                    RuleSection(title = "2. Cách tính điểm lá bài & Át (A) linh hoạt") {
                        Text(
                            text = "• Lá 2–10: Điểm theo số trên lá bài.\n" +
                                "• J, Q, K: Tính 10 điểm.\n" +
                                "• Lá Át (A) thay đổi giá trị theo số lá:\n" +
                                "  - Khi có 2 lá: A tính 10 hoặc 11 điểm.\n" +
                                "  - Khi có 3 lá: A tính 1 hoặc 10 điểm.\n" +
                                "  - Khi có 4–5 lá: A chỉ tính 1 điểm.\n" +
                                "• Hệ thống tự động chọn phương án tốt nhất ≤ 21 điểm.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                        )
                    }
                }

                item {
                    RuleSection(title = "3. Thứ bậc tay bài đặc biệt") {
                        Text(
                            text = "Xếp từ mạnh nhất đến yếu nhất:\n" +
                                "1. Xì bàng (2 lá Át) — Ăn ${rules.xiBangMultiplier}× cược.\n" +
                                "2. Xì dách (1 Át + 1 Tây/10) — Ăn ${rules.xiDachMultiplier}× cược.\n" +
                                "3. Ngũ linh (5 lá tổng ≤ 21) — Ăn ${rules.nguLinhMultiplier}× cược.\n" +
                                "   (Hai người cùng Ngũ linh: Người ít điểm hơn thắng!)\n" +
                                "4. Đủ tuổi (16–21 điểm; Cái: 15–21 điểm) — Ăn 1× cược.\n" +
                                "5. Non (< 16 với con, < 15 với cái): Chưa đủ tuổi.\n" +
                                "6. Quắc (> 21 điểm): Cháy bài.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                        )
                    }
                }

                item {
                    RuleSection(title = "4. Quy trình xét bài & Luật hiện tại") {
                        Text(
                            text = "• Nhà con: Bắt buộc rút khi Non (< ${rules.playerMinScore}), chỉ được Dằn khi đủ tuổi.\n" +
                                "• Nhà cái: Bắt buộc rút tới ≥ ${rules.dealerMinScore} mới được quyền Xét bài.\n" +
                                "• Cả hai cùng Quắc: ${if (rules.bothBustPlayerLoses) "Nhà con luôn thua (Luật nhà đang bật)" else "Hòa tiền (Luật mặc định)"}.\n" +
                                "• Chuyển vai trò cái: ${if (rules.winnerBecomesDealer) "Bật (Ai có Xì bàng/Xì dách làm cái ván sau)" else "Tắt"}.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleSection(
    title: String,
    content: @Composable () -> Unit,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        strong = true,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = BvColors.Gold,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
