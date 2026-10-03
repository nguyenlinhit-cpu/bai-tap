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
import com.baiviet.game.lieng.rules.LiengRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiengHouseRulesViewModel @Inject constructor(
    private val repo: HouseRulesRepository,
) : ViewModel() {

    val rules: StateFlow<LiengRules> = repo.rules(LiengRules.GAME_ID, LiengRules.serializer(), LiengRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiengRules.DEFAULT)

    fun updateRules(newRules: LiengRules) {
        viewModelScope.launch {
            repo.save(LiengRules.GAME_ID, LiengRules.serializer(), newRules)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repo.reset(LiengRules.GAME_ID)
        }
    }
}

@Composable
fun LiengHouseRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: LiengHouseRulesViewModel = hiltViewModel(),
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
                        text = "LUẬT NHÀ — LIÊNG",
                        style = MaterialTheme.typography.titleLarge,
                        color = BvColors.Gold,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tùy biến thể thức bài cào tố Liêng",
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
                                        text = "Thứ tự chất: Cơ trên Rô",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (rules.suitOrderCoOverRo) {
                                            "Đang bật: ♥ Cơ > ♦ Rô > ♣ Chuồn > ♠ Bích"
                                        } else {
                                            "Mặc định (tắt): ♦ Rô > ♥ Cơ > ♣ Chuồn > ♠ Bích"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                Switch(
                                    checked = rules.suitOrderCoOverRo,
                                    onCheckedChange = { viewModel.updateRules(rules.copy(suitOrderCoOverRo = it)) },
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
                                        text = "So số trước so chất",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (rules.rankBeforeSuit) {
                                            "Đang bật: So số lá lớn nhất trước (A > K > ... > 2), cùng số mới so chất"
                                        } else {
                                            "Mặc định (tắt): So chất trước, cùng chất mới so số"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                Switch(
                                    checked = rules.rankBeforeSuit,
                                    onCheckedChange = { viewModel.updateRules(rules.copy(rankBeforeSuit = it)) },
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
                                        text = "Cùng dây Liêng chia đều pot",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (rules.liengTieIsPush) {
                                            "Đang bật: Cùng giá trị Liêng thì hòa và chia đều pot"
                                        } else {
                                            "Mặc định (tắt): So chất lá cao nhất của dây Liêng"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                Switch(
                                    checked = rules.liengTieIsPush,
                                    onCheckedChange = { viewModel.updateRules(rules.copy(liengTieIsPush = it)) },
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
