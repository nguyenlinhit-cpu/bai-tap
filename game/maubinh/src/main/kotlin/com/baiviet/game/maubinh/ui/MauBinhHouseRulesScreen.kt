package com.baiviet.game.maubinh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.baiviet.game.maubinh.rules.MauBinhRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MauBinhHouseRulesViewModel @Inject constructor(
    private val repo: HouseRulesRepository,
) : ViewModel() {
    val rules: StateFlow<MauBinhRules> = repo.rules(MauBinhRules.GAME_ID, MauBinhRules.serializer(), MauBinhRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MauBinhRules.DEFAULT)

    fun save(rules: MauBinhRules) {
        viewModelScope.launch {
            repo.save(MauBinhRules.GAME_ID, MauBinhRules.serializer(), rules)
        }
    }

    fun reset() {
        viewModelScope.launch {
            repo.reset(MauBinhRules.GAME_ID)
        }
    }
}

@Composable
fun MauBinhHouseRulesScreen(
    onBack: () -> Unit,
    viewModel: MauBinhHouseRulesViewModel = hiltViewModel(),
) {
    val currentRules by viewModel.rules.collectAsStateWithLifecycle()
    var aceLowest by remember(currentRules) { mutableStateOf(currentRules.aceTwoThreeFourFiveIsLowest) }
    var sapThreeChi by remember(currentRules) { mutableStateOf(currentRules.sapThreeChiMultiplier) }
    var sapLang by remember(currentRules) { mutableStateOf(currentRules.sapLangMultiplier) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BvColors.NavyDark, Color(0xFF0F172A), BvColors.NavyDark),
                ),
            )
            .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(glyph = "←", contentDescription = "Quay lại", onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Luật nhà Mậu Binh",
                    style = MaterialTheme.typography.titleLarge,
                    color = BvColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "A-2-3-4-5 là sảnh nhỏ nhất",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BvColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Mặc định tắt: sảnh A-2-3-4-5 là sảnh lớn thứ hai (sau 10-J-Q-K-A). Khi bật: là sảnh nhỏ nhất trong mọi sảnh.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BvColors.TextSecondary,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = aceLowest,
                                onCheckedChange = { aceLowest = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BvColors.Gold,
                                    checkedTrackColor = BvColors.GoldDeep,
                                ),
                            )
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Sập 3 chi (Nhân đôi chi)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BvColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Thắng cả 3 chi trước một đối thủ -> tổng số chi thắng nhân đôi (×2). Mặc định bật.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BvColors.TextSecondary,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = sapThreeChi,
                                onCheckedChange = { sapThreeChi = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BvColors.Gold,
                                    checkedTrackColor = BvColors.GoldDeep,
                                ),
                            )
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Sập làng (Nhân đôi thêm)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BvColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Bàn từ 3 người trở lên: người thắng cả 3 chi trước tất cả người chơi sẽ được nhân đôi thêm một lần nữa (×4). Mặc định bật.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BvColors.TextSecondary,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = sapLang,
                                onCheckedChange = { sapLang = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BvColors.Gold,
                                    checkedTrackColor = BvColors.GoldDeep,
                                ),
                            )
                        }
                    }
                }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                "Mức phạt Binh Lủng",
                                style = MaterialTheme.typography.titleMedium,
                                color = BvColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Số chi bị phạt cố định đền cho mỗi người chơi hợp lệ: ${currentRules.foulPenaltyChi} chi/người (tương đương bị sập).",
                                style = MaterialTheme.typography.bodySmall,
                                color = BvColors.TextSecondary,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionButton(
                    text = "Mặc định",
                    onClick = {
                        viewModel.reset()
                        aceLowest = MauBinhRules.DEFAULT.aceTwoThreeFourFiveIsLowest
                        sapThreeChi = MauBinhRules.DEFAULT.sapThreeChiMultiplier
                        sapLang = MauBinhRules.DEFAULT.sapLangMultiplier
                    },
                    style = ActionStyle.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    text = "Lưu thiết lập",
                    onClick = {
                        val newRules = currentRules.copy(
                            aceTwoThreeFourFiveIsLowest = aceLowest,
                            sapThreeChiMultiplier = sapThreeChi,
                            sapLangMultiplier = sapLang,
                        )
                        viewModel.save(newRules)
                        onBack()
                    },
                    style = ActionStyle.PRIMARY,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
