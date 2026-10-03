package com.baiviet.game.samloc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.baiviet.game.samloc.R
import com.baiviet.game.samloc.rules.SamLocRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlHouseRulesViewModel
    @Inject
    constructor(
        private val repo: HouseRulesRepository,
    ) : ViewModel() {
        val rules: StateFlow<SamLocRules> =
            repo
                .rules(SamLocRules.GAME_ID, SamLocRules.serializer(), SamLocRules.DEFAULT)
                .stateIn(viewModelScope, SharingStarted.Eagerly, SamLocRules.DEFAULT)

        fun update(transform: (SamLocRules) -> SamLocRules) {
            viewModelScope.launch {
                repo.save(SamLocRules.GAME_ID, SamLocRules.serializer(), transform(rules.value))
            }
        }

        fun reset() {
            viewModelScope.launch { repo.reset(SamLocRules.GAME_ID) }
        }
    }

/**
 * Màn Luật nhà Sâm Lốc: bật/tắt các biến thể luật chơi.
 */
@Composable
fun SamLocHouseRulesScreen(
    onBack: () -> Unit,
    viewModel: SlHouseRulesViewModel = hiltViewModel(),
) {
    val r by viewModel.rules.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BvColors.NavyDark, BvColors.PurpleDark)))
            .safeDrawingPadding(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton("←", stringResource(R.string.sl_close), onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.sl_house_title),
                color = BvColors.Gold,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            ActionButton(stringResource(R.string.sl_house_reset), viewModel::reset, style = ActionStyle.SECONDARY)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SlHeader(stringResource(R.string.sl_house_section_rules))

            SlToggle(
                title = stringResource(R.string.sl_house_quad_cuts_pair_twos),
                desc = stringResource(R.string.sl_house_quad_cuts_pair_twos_desc),
                checked = r.quadCutsPairOfTwos,
                onCheckedChange = { v -> viewModel.update { it.copy(quadCutsPairOfTwos = v) } },
            )

            SlToggle(
                title = stringResource(R.string.sl_house_three_triples),
                desc = stringResource(R.string.sl_house_three_triples_desc),
                checked = r.threeTriplesInstantWin,
                onCheckedChange = { v -> viewModel.update { it.copy(threeTriplesInstantWin = v) } },
            )

            SlToggle(
                title = stringResource(R.string.sl_house_diff_twos),
                desc = stringResource(R.string.sl_house_diff_twos_desc),
                checked = r.differentiateTwoColors,
                onCheckedChange = { v -> viewModel.update { it.copy(differentiateTwoColors = v) } },
            )

            Spacer(Modifier.height(8.dp))
            SlHeader(stringResource(R.string.sl_house_section_penalties))

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.sl_house_sam_payout) + ": ${r.samWinCards} lá",
                        color = BvColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.sl_house_instant_payout) + ": ${r.instantWinCards} lá",
                        color = BvColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.sl_house_finish_two_payout) + ": ${r.finishWithTwoPenaltyCards} lá",
                        color = BvColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SlHeader(title: String) {
    Text(
        title,
        color = BvColors.Gold,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}

@Composable
private fun SlToggle(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    GlassPanel(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Switch) { onCheckedChange(!checked) },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = BvColors.TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(desc, color = BvColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = BvColors.NavyDark,
                        checkedTrackColor = BvColors.Gold,
                    ),
            )
        }
    }
}
