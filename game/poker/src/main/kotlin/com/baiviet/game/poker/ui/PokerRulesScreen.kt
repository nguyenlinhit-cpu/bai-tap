package com.baiviet.game.poker.ui

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
import com.baiviet.game.poker.rules.PokerRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PokerRulesViewModel @Inject constructor(repo: HouseRulesRepository) : ViewModel() {
    val rules: StateFlow<PokerRules> = repo.rules(PokerRules.GAME_ID, PokerRules.serializer(), PokerRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PokerRules.DEFAULT)
}

@Composable
fun PokerRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: PokerRulesViewModel = hiltViewModel(),
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
                Text(
                    text = "LUẬT CHƠI POKER TEXAS HOLD\'EM",
                    style = MaterialTheme.typography.titleLarge,
                    color = BvColors.Gold,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Rules Content
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "1. Tổng quan & Vị trí",
                                style = MaterialTheme.typography.titleMedium,
                                color = BvColors.Ivory,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "• Sử dụng bộ bài Tây 52 lá tiêu chuẩn.\n" +
                                    "• Nút Dealer (D) luân chuyển thuận chiều kim đồng hồ sau mỗi ván đấu.\n" +
                                    "• Hai vị trí bên trái Dealer đặt tiền mù: Small Blind (SB = BB / 2) và Big Blind (BB).\n" +
                                    "• Số chip Buy-in: 50–200 BB (mặc định: ${rules.startingChipsBB} BB).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "2. Bốn vòng cược",
                                style = MaterialTheme.typography.titleMedium,
                                color = BvColors.Ivory,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "• Preflop: Mỗi người được chia 2 lá tẩy riêng. Người bên trái BB hành động đầu tiên.\n" +
                                    "• Flop: Đốt 1 lá, lật 3 lá chung trên bàn. Người đầu tiên còn chơi bên trái nút đi trước.\n" +
                                    "• Turn: Đốt 1 lá, lật lá chung thứ 4.\n" +
                                    "• River: Đốt 1 lá, lật lá chung thứ 5 cuối cùng.\n" +
                                    "• Showdown: Lật bài so điểm. Người cược/tố cuối ở River mở trước. Ghép 5 lá tốt nhất từ 7 lá.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "3. Thứ bậc tay bài (Mạnh → Yếu)",
                                style = MaterialTheme.typography.titleMedium,
                                color = BvColors.Ivory,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "1. Thùng phá sảnh lớn (Royal Flush): 10-J-Q-K-A đồng chất\n" +
                                    "2. Thùng phá sảnh (Straight Flush): 5 lá liên tiếp đồng chất\n" +
                                    "3. Tứ quý (Four of a Kind): 4 lá cùng giá trị\n" +
                                    "4. Cù lũ (Full House): 1 bộ ba + 1 bộ đôi\n" +
                                    "5. Thùng (Flush): 5 lá cùng chất\n" +
                                    "6. Sảnh (Straight): 5 lá liên tiếp (A-2-3-4-5 hoặc 10-J-Q-K-A)\n" +
                                    "7. Sám cô (Three of a Kind): 3 lá cùng giá trị\n" +
                                    "8. Thú (Two Pair): 2 đôi khác nhau\n" +
                                    "9. Đôi (One Pair): 1 đôi\n" +
                                    "10. Mậu thầu (High Card): Lá bài cao nhất",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "4. Side Pot & Xu lẻ",
                                style = MaterialTheme.typography.titleMedium,
                                color = BvColors.Ivory,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "• Khi người chơi All-in với số chip ít hơn đối thủ, hệ thống sẽ tạo các tầng Side Pot riêng biệt.\n" +
                                    "• Người chơi chỉ đủ điều kiện thắng các pot mà mình đã đóng góp.\n" +
                                    "• Hòa pot chia đều; nếu có xu lẻ, trao cho người chơi gần bên trái nút Dealer nhất.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }
        }
    }
}
