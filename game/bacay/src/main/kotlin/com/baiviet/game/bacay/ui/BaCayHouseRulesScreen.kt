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
import com.baiviet.game.bacay.rules.BaCayMode
import com.baiviet.game.bacay.rules.BaCayRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BaCayHouseRulesViewModel @Inject constructor(
    private val repo: HouseRulesRepository,
) : ViewModel() {

    val rules: StateFlow<BaCayRules> = repo.rules(BaCayRules.GAME_ID, BaCayRules.serializer(), BaCayRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BaCayRules.DEFAULT)

    fun updateRules(newRules: BaCayRules) {
        viewModelScope.launch {
            repo.save(BaCayRules.GAME_ID, BaCayRules.serializer(), newRules)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repo.reset(BaCayRules.GAME_ID)
        }
    }
}

@Composable
fun BaCayHouseRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: BaCayHouseRulesViewModel = hiltViewModel(),
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
                        text = "LUẬT NHÀ — BA CÂY",
                        style = MaterialTheme.typography.titleLarge,
                        color = BvColors.Gold,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tùy biến thể thức bài cào 3 lá",
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Chế độ chơi: Nhất ăn tất",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (rules.gameMode == BaCayMode.WINNER_TAKES_ALL) {
                                            "Đang bật: Nhất ăn tất (gộp pot và ăn cả bàn)"
                                        } else {
                                            "Mặc định: Chơi có nhà cái (các con so bài với cái)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                Switch(
                                    checked = rules.gameMode == BaCayMode.WINNER_TAKES_ALL,
                                    onCheckedChange = { isWinnerTakesAll ->
                                        viewModel.updateRules(rules.copy(gameMode = if (isWinnerTakesAll) BaCayMode.WINNER_TAKES_ALL else BaCayMode.WITH_DEALER))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BvColors.Gold,
                                        checkedTrackColor = BvColors.GoldDeep,
                                    ),
                                )
                            }
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sáp đứng trên Ba Tiên",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (rules.sapBeatsBaTien) {
                                            "Đang bật: Sáp (3 lá cùng số) lớn hơn Ba Tiên"
                                        } else {
                                            "Mặc định (tắt): Ba Tiên lớn hơn Sáp"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                Switch(
                                    checked = rules.sapBeatsBaTien,
                                    onCheckedChange = { viewModel.updateRules(rules.copy(sapBeatsBaTien = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BvColors.Gold,
                                        checkedTrackColor = BvColors.GoldDeep,
                                    ),
                                )
                            }
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Ba Tiên ăn gấp đôi (×2)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (rules.baTienPaysDouble) {
                                            "Đang bật: Thắng bằng Ba Tiên nhận thưởng gấp đôi"
                                        } else {
                                            "Mặc định (tắt): Thắng bằng Ba Tiên ăn bình thường 1×"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                Switch(
                                    checked = rules.baTienPaysDouble,
                                    onCheckedChange = { viewModel.updateRules(rules.copy(baTienPaysDouble = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BvColors.Gold,
                                        checkedTrackColor = BvColors.GoldDeep,
                                    ),
                                )
                            }
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bằng điểm thì hòa tiền",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (rules.tieIsPush) {
                                            "Đang bật: Bằng nút bài thì hòa tiền không so chất"
                                        } else {
                                            "Mặc định (tắt): Bằng điểm so lá bài lớn nhất (Rô > Cơ > Chuồn > Bích)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                Switch(
                                    checked = rules.tieIsPush,
                                    onCheckedChange = { viewModel.updateRules(rules.copy(tieIsPush = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BvColors.Gold,
                                        checkedTrackColor = BvColors.GoldDeep,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            ActionButton(
                text = "Khôi phục mặc định",
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth(),
                style = ActionStyle.SECONDARY,
            )
        }
    }
}
