package com.baiviet.game.tienlen.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.baiviet.game.tienlen.R
import com.baiviet.game.tienlen.rules.TienLenRules
import com.baiviet.game.tienlen.rules.TlScoring
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TlHouseRulesViewModel @Inject constructor(private val repo: HouseRulesRepository) : ViewModel() {
    val rules: StateFlow<TienLenRules> = repo.rules(TienLenRules.GAME_ID, TienLenRules.serializer(), TienLenRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.Eagerly, TienLenRules.DEFAULT)

    fun update(transform: (TienLenRules) -> TienLenRules) {
        viewModelScope.launch {
            repo.save(TienLenRules.GAME_ID, TienLenRules.serializer(), transform(rules.value))
        }
    }

    fun reset() {
        viewModelScope.launch { repo.reset(TienLenRules.GAME_ID) }
    }
}

/** Màn Luật nhà: bật/tắt biến thể, chỉnh số. Lưu ngay khi thay đổi. */
@Composable
fun TienLenHouseRulesScreen(onBack: () -> Unit, viewModel: TlHouseRulesViewModel = hiltViewModel()) {
    val r by viewModel.rules.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BvColors.NavyDark, BvColors.PurpleDark)))
            .safeDrawingPadding(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton("←", stringResource(R.string.tl_close), onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.tl_house_title),
                color = BvColors.Gold,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            ActionButton(stringResource(R.string.tl_house_reset), viewModel::reset, style = ActionStyle.SECONDARY)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    SectionTitle(stringResource(R.string.tl_house_section_rules))
                    Text(stringResource(R.string.tl_house_scoring), color = BvColors.Ivory, style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.tl_house_scoring_desc), color = BvColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Segment(stringResource(R.string.tl_mode_count), r.scoring == TlScoring.COUNT_CARDS) {
                            viewModel.update { it.copy(scoring = TlScoring.COUNT_CARDS) }
                        }
                        Segment(stringResource(R.string.tl_mode_ranking), r.scoring == TlScoring.RANKING) {
                            viewModel.update { it.copy(scoring = TlScoring.RANKING) }
                        }
                    }
                    Toggle(R.string.tl_house_3s, r.require3SpadesOnFirstMove) { v -> viewModel.update { it.copy(require3SpadesOnFirstMove = v) } }
                    Toggle(R.string.tl_house_4pairs, r.fourPairsCutWithoutTurn) { v -> viewModel.update { it.copy(fourPairsCutWithoutTurn = v) } }
                    Toggle(R.string.tl_house_quad_pair, r.quadCutsPairOfTwos) { v -> viewModel.update { it.copy(quadCutsPairOfTwos = v) } }
                    Toggle(R.string.tl_house_4threes, r.quadThreeInstantWinFirstGame) { v -> viewModel.update { it.copy(quadThreeInstantWinFirstGame = v) } }
                    Toggle(R.string.tl_house_5pairs, r.fiveConsecutivePairsInstantWin) { v -> viewModel.update { it.copy(fiveConsecutivePairsInstantWin = v) } }
                    Stepper(R.string.tl_house_samecolor, r.sameColorInstantWinCount, 12..13) { v -> viewModel.update { it.copy(sameColorInstantWinCount = v) } }
                    Toggle(R.string.tl_house_instant_pen, r.instantWinCountsPenalties) { v -> viewModel.update { it.copy(instantWinCountsPenalties = v) } }
                    Toggle(R.string.tl_house_no_two_finish, r.forbidFinishWithTwo) { v -> viewModel.update { it.copy(forbidFinishWithTwo = v) } }
                    Stepper(R.string.tl_house_turn, r.turnSeconds, 10..60, step = 5) { v -> viewModel.update { it.copy(turnSeconds = v) } }
                }
            }
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    SectionTitle(stringResource(R.string.tl_house_section_money) + " — " + stringResource(R.string.tl_house_money))
                    Stepper(R.string.tl_money_black_two, r.blackTwoCards, 1..20) { v -> viewModel.update { it.copy(blackTwoCards = v) } }
                    Stepper(R.string.tl_money_red_two, r.redTwoCards, 1..20) { v -> viewModel.update { it.copy(redTwoCards = v) } }
                    Stepper(R.string.tl_money_three_pairs, r.threePairsCards, 1..40) { v -> viewModel.update { it.copy(threePairsCards = v) } }
                    Stepper(R.string.tl_money_quad, r.quadCards, 1..40) { v -> viewModel.update { it.copy(quadCards = v) } }
                    Stepper(R.string.tl_money_four_pairs, r.fourPairsCards, 1..60) { v -> viewModel.update { it.copy(fourPairsCards = v) } }
                    Stepper(R.string.tl_money_cong, r.congCards, 13..52) { v -> viewModel.update { it.copy(congCards = v) } }
                    Stepper(R.string.tl_money_instant, r.instantWinCards, 13..52) { v -> viewModel.update { it.copy(instantWinCards = v) } }
                }
            }
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) { RulesSummary(r, betUnit = null) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = BvColors.Teal, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun Segment(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) BvColors.TextOnGold else BvColors.Ivory,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BvColors.Gold else BvColors.GlassBgStrong)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

@Composable
private fun Toggle(label: Int, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!value) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), color = BvColors.Ivory, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = BvColors.Gold, checkedTrackColor = BvColors.Gold.copy(alpha = 0.35f)),
        )
    }
}

@Composable
private fun Stepper(label: Int, value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), color = BvColors.Ivory, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        StepButton("−", value - step >= range.first) { onChange(value - step) }
        Text(
            "$value",
            color = BvColors.Gold,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.widthIn(min = 44.dp).padding(horizontal = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepButton("+", value + step <= range.last) { onChange(value + step) }
    }
}

@Composable
private fun StepButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (enabled) BvColors.GlassBgStrong else BvColors.GlassBg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = if (enabled) BvColors.Ivory else BvColors.TextMuted, style = MaterialTheme.typography.titleLarge)
    }
}
