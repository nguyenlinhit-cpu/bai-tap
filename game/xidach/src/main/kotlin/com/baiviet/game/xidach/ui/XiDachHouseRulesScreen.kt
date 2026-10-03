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
import com.baiviet.game.xidach.rules.XiDachRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class XiDachHouseRulesViewModel @Inject constructor(
    private val repo: HouseRulesRepository,
) : ViewModel() {

    val rules: StateFlow<XiDachRules> = repo.rules(XiDachRules.GAME_ID, XiDachRules.serializer(), XiDachRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), XiDachRules.DEFAULT)

    fun updateRules(newRules: XiDachRules) {
        viewModelScope.launch {
            repo.save(XiDachRules.GAME_ID, XiDachRules.serializer(), newRules)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repo.reset(XiDachRules.GAME_ID)
        }
    }
}

@Composable
fun XiDachHouseRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: XiDachHouseRulesViewModel = hiltViewModel(),
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
                        text = "LUẬT NHÀ — XÌ DÁCH",
                        style = MaterialTheme.typography.titleLarge,
                        color = BvColors.Gold,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tùy chỉnh biến thể theo thói quen chơi",
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
                                        text = "Cả hai cùng quắc: Con luôn thua",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (rules.bothBustPlayerLoses) {
                                            "Đang bật: Khi cả nhà cái và nhà con cùng quắc (>21), nhà con bị tính thua."
                                        } else {
                                            "Mặc định (tắt): Cả hai cùng quắc thì hòa tiền."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                Switch(
                                    checked = rules.bothBustPlayerLoses,
                                    onCheckedChange = { viewModel.updateRules(rules.copy(bothBustPlayerLoses = it)) },
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
                                        text = "Xì bàng / Xì dách làm cái ván sau",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (rules.winnerBecomesDealer) {
                                            "Đang bật: Người chơi đạt Xì bàng hoặc Xì dách sẽ trở thành Nhà Cái ở ván tiếp theo."
                                        } else {
                                            "Mặc định (tắt): Giữ nguyên vai trò nhà cái theo chế độ đã chọn."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                Switch(
                                    checked = rules.winnerBecomesDealer,
                                    onCheckedChange = { viewModel.updateRules(rules.copy(winnerBecomesDealer = it)) },
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
