package com.baiviet.game.phom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.baiviet.core.data.HouseRulesRepository
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
class PhomRulesViewModel @Inject constructor(repo: HouseRulesRepository) : ViewModel() {
    val rules: StateFlow<PhomRules> = repo.rules(PhomRules.GAME_ID, PhomRules.serializer(), PhomRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhomRules.DEFAULT)
}

private data class PhomSection(val title: Int, val content: @Composable (PhomRules) -> Unit)

@Composable
fun PhomRulesScreen(onBack: () -> Unit, viewModel: PhomRulesViewModel = hiltViewModel()) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val sections = listOf(
        PhomSection(R.string.phom_rules_intro_h) { PhomBody(stringResource(R.string.phom_rules_intro)) },
        PhomSection(R.string.phom_rules_deck_h) { PhomBody(stringResource(R.string.phom_rules_deck)) },
        PhomSection(R.string.phom_rules_ranks_h) { PhomBody(stringResource(R.string.phom_rules_ranks)) },
        PhomSection(R.string.phom_rules_melds_h) { PhomBody(stringResource(R.string.phom_rules_melds_body)) },
        PhomSection(R.string.phom_rules_flow_h) { PhomBody(stringResource(R.string.phom_rules_flow_body)) },
        PhomSection(R.string.phom_rules_eat_h) { PhomBody(stringResource(R.string.phom_rules_eat_body)) },
        PhomSection(R.string.phom_rules_u_h) { PhomBody(stringResource(R.string.phom_rules_u_body)) },
        PhomSection(R.string.phom_rules_score_h) { PhomBody(stringResource(R.string.phom_rules_score_body)) },
    )

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
                    stringResource(R.string.phom_rules_title),
                    color = BvColors.Gold,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    stringResource(R.string.phom_rules_current, "1B"),
                    color = BvColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(sections) { idx, sec ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.08f))
                        .clickable { scope.launch { listState.animateScrollToItem(idx) } }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        stringResource(sec.title),
                        color = BvColors.TextPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(sections) { _, sec ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            stringResource(sec.title),
                            color = BvColors.Gold,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        sec.content(rules)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhomBody(text: String) {
    Text(
        text,
        color = BvColors.TextPrimary,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f,
    )
}
