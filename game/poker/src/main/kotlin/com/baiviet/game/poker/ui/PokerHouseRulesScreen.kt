package com.baiviet.game.poker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.game.poker.rules.PokerRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PokerHouseRulesViewModel @Inject constructor(
    private val repo: HouseRulesRepository,
) : ViewModel() {

    val rules: StateFlow<PokerRules> = repo.rules(PokerRules.GAME_ID, PokerRules.serializer(), PokerRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PokerRules.DEFAULT)

    fun updateRules(newRules: PokerRules) {
        viewModelScope.launch {
            repo.save(PokerRules.GAME_ID, PokerRules.serializer(), newRules)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repo.reset(PokerRules.GAME_ID)
        }
    }
}

@Composable
fun PokerHouseRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: PokerHouseRulesViewModel = hiltViewModel(),
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
                    text = "TÙY BIẾN LUẬT NHÀ (POKER)",
                    style = MaterialTheme.typography.titleLarge,
                    color = BvColors.Gold,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Buy-in BB option
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Số chip Buy-in ban đầu",
                                style = MaterialTheme.typography.titleMedium,
                                color = BvColors.Ivory,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Lượng stack khởi điểm của mỗi người chơi theo số lần Big Blind (BB)",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf(50, 100, 200).forEach { bb ->
                                    val isSelected = rules.startingChipsBB == bb
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) BvColors.Gold else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(8.dp),
                                            )
                                            .clickable { viewModel.updateRules(rules.copy(startingChipsBB = bb)) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    ) {
                                        Text(
                                            text = "$bb BB",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Turn Seconds Option
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Thời gian suy nghĩ mỗi lượt",
                                style = MaterialTheme.typography.titleMedium,
                                color = BvColors.Ivory,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Thời gian tối đa để người chơi đưa ra quyết định hành động",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf(10, 15, 30).forEach { sec ->
                                    val isSelected = rules.turnSeconds == sec
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) BvColors.Gold else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(8.dp),
                                            )
                                            .clickable { viewModel.updateRules(rules.copy(turnSeconds = sec)) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    ) {
                                        Text(
                                            text = "$sec giây",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Practice Mode Show Win Rate
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hiện tỉ lệ thắng trong luyện tập",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BvColors.Ivory,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Hiển thị xác suất thắng ước tính bằng mô phỏng Monte Carlo",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                            }
                            Switch(
                                checked = rules.showWinRateInPractice,
                                onCheckedChange = { checked ->
                                    viewModel.updateRules(rules.copy(showWinRateInPractice = checked))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BvColors.Gold,
                                    checkedTrackColor = BvColors.FeltGreen,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            ActionButton(
                text = "Đặt lại luật mặc định",
                style = ActionStyle.SECONDARY,
                onClick = { viewModel.resetToDefaults() },
            )
        }
    }
}
