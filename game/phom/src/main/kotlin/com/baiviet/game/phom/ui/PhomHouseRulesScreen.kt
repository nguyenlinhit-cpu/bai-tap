package com.baiviet.game.phom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
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
import com.baiviet.game.phom.R
import com.baiviet.game.phom.rules.PhomRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhomHouseRulesViewModel @Inject constructor(
    private val repo: HouseRulesRepository,
) : ViewModel() {
    val rules: StateFlow<PhomRules> = repo.rules(PhomRules.GAME_ID, PhomRules.serializer(), PhomRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhomRules.DEFAULT)

    fun save(rules: PhomRules) {
        viewModelScope.launch {
            repo.save(PhomRules.GAME_ID, PhomRules.serializer(), rules)
        }
    }

    fun reset() {
        viewModelScope.launch {
            repo.reset(PhomRules.GAME_ID)
        }
    }
}

@Composable
fun PhomHouseRulesScreen(
    onBack: () -> Unit,
    viewModel: PhomHouseRulesViewModel = hiltViewModel(),
) {
    val persisted by viewModel.rules.collectAsStateWithLifecycle()
    var draft by remember(persisted) { mutableStateOf(persisted) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BvColors.NavyDark, BvColors.PurpleDark)))
            .safeDrawingPadding(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton("←", stringResource(R.string.phom_close), onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.phom_hr_title),
                    color = BvColors.Gold,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    stringResource(R.string.phom_hr_subtitle),
                    color = BvColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item {
                RuleSwitch(
                    title = stringResource(R.string.phom_hr_u_khan_title),
                    desc = stringResource(R.string.phom_hr_u_khan_desc),
                    checked = draft.uKhanEnabled,
                    onCheckedChange = { draft = draft.copy(uKhanEnabled = it) },
                )
            }
            item {
                RuleSwitch(
                    title = stringResource(R.string.phom_hr_eat_rate_title),
                    desc = stringResource(R.string.phom_hr_eat_rate_desc),
                    checked = draft.progressiveEatPenalty,
                    onCheckedChange = { draft = draft.copy(progressiveEatPenalty = it) },
                )
            }
            item {
                RuleSwitch(
                    title = stringResource(R.string.phom_hr_eat_chot_den_title),
                    desc = stringResource(R.string.phom_hr_eat_chot_den_desc),
                    checked = draft.eatChotDenEnabled,
                    onCheckedChange = { draft = draft.copy(eatChotDenEnabled = it) },
                )
            }
            item {
                RuleSwitch(
                    title = stringResource(R.string.phom_hr_mom_layoff_title),
                    desc = stringResource(R.string.phom_hr_mom_layoff_desc),
                    checked = draft.momCanLayOff,
                    onCheckedChange = { draft = draft.copy(momCanLayOff = it) },
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActionButton(
                text = stringResource(R.string.phom_hr_reset),
                onClick = { viewModel.reset() },
                style = ActionStyle.SECONDARY,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                text = stringResource(R.string.phom_hr_save),
                onClick = {
                    viewModel.save(draft)
                    onBack()
                },
                style = ActionStyle.PRIMARY,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RuleSwitch(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = BvColors.TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(desc, color = BvColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BvColors.Gold,
                    checkedTrackColor = BvColors.Teal,
                ),
            )
        }
    }
}
