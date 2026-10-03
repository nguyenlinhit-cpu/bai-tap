package com.baiviet.game.samloc.ui

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.core.data.HouseRulesRepository
import com.baiviet.core.ui.card.PlayingCard
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
class SlRulesViewModel
    @Inject
    constructor(
        repo: HouseRulesRepository,
    ) : ViewModel() {
        val rules: StateFlow<SamLocRules> =
            repo
                .rules(SamLocRules.GAME_ID, SamLocRules.serializer(), SamLocRules.DEFAULT)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SamLocRules.DEFAULT)
    }

private data class SlSection(
    val title: Int,
    val content: @Composable (SamLocRules) -> Unit,
)

private fun cards(vararg spec: Pair<Rank, Suit>) = spec.map { Card(it.first, it.second) }

@Composable
fun SamLocRulesScreen(
    onBack: () -> Unit,
    viewModel: SlRulesViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val sections =
        listOf(
            SlSection(R.string.sl_rules_intro_h) { SlBody(stringResource(R.string.sl_rules_intro)) },
            SlSection(R.string.sl_rules_deck_h) { SlBody(stringResource(R.string.sl_rules_deck)) },
            SlSection(R.string.sl_rules_order_h) {
                SlBody(stringResource(R.string.sl_rules_order))
                SlCardRow(
                    cards(
                        Rank.THREE to Suit.SPADE,
                        Rank.FOUR to Suit.CLUB,
                        Rank.TEN to Suit.DIAMOND,
                        Rank.KING to Suit.HEART,
                        Rank.ACE to Suit.SPADE,
                        Rank.TWO to Suit.HEART,
                    ),
                    40.dp,
                    Modifier.padding(top = 8.dp),
                )
            },
            SlSection(R.string.sl_rules_combos_h) {
                SlCombo(R.string.sl_rules_single, cards(Rank.SEVEN to Suit.HEART))
                SlCombo(R.string.sl_rules_pair, cards(Rank.NINE to Suit.SPADE, Rank.NINE to Suit.DIAMOND))
                SlCombo(R.string.sl_rules_triple, cards(Rank.JACK to Suit.CLUB, Rank.JACK to Suit.DIAMOND, Rank.JACK to Suit.HEART))
                SlCombo(
                    R.string.sl_rules_quad,
                    cards(
                        Rank.EIGHT to Suit.SPADE,
                        Rank.EIGHT to Suit.CLUB,
                        Rank.EIGHT to Suit.DIAMOND,
                        Rank.EIGHT to Suit.HEART,
                    ),
                )
                SlCombo(R.string.sl_rules_straight, cards(Rank.ACE to Suit.SPADE, Rank.TWO to Suit.HEART, Rank.THREE to Suit.DIAMOND))
                SlCombo(R.string.sl_rules_straight, cards(Rank.TWO to Suit.CLUB, Rank.THREE to Suit.SPADE, Rank.FOUR to Suit.HEART))
                SlCombo(R.string.sl_rules_straight, cards(Rank.QUEEN to Suit.CLUB, Rank.KING to Suit.DIAMOND, Rank.ACE to Suit.HEART))
            },
            SlSection(R.string.sl_rules_cut_h) { r ->
                SlBody(stringResource(R.string.sl_rules_cut, r.cutCards))
            },
            SlSection(R.string.sl_rules_sam_h) { r ->
                SlBody(stringResource(R.string.sl_rules_sam, r.samFailPenaltyCards, r.samWinCards))
            },
            SlSection(R.string.sl_rules_bao_1_h) {
                SlBody(stringResource(R.string.sl_rules_bao_1))
            },
            SlSection(R.string.sl_rules_finish_two_h) { r ->
                SlBody(stringResource(R.string.sl_rules_finish_two, r.finishWithTwoPenaltyCards))
            },
            SlSection(R.string.sl_rules_instant_h) { r ->
                SlBody(stringResource(R.string.sl_rules_instant, r.instantWinCards))
            },
            SlSection(R.string.sl_rules_payout_h) { r ->
                SlBody(
                    stringResource(
                        R.string.sl_rules_payout_body,
                        r.defaultTwoCards,
                        r.quadPenaltyCards,
                        r.congCards,
                        r.cutCards,
                        r.samWinCards,
                        r.samFailPenaltyCards,
                        r.instantWinCards,
                        r.finishWithTwoPenaltyCards,
                    ),
                )
            },
        )

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BvColors.NavyDark, BvColors.PurpleDark)))
            .safeDrawingPadding(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton("←", stringResource(R.string.sl_close), onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.sl_rules_title),
                    color = BvColors.Gold,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    stringResource(R.string.sl_rules_current, "1B"),
                    color = BvColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        // Thanh mục lục cuộn ngang
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

        // Nội dung luật chi tiết
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
private fun SlBody(text: String) {
    Text(
        text,
        color = BvColors.TextPrimary,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f,
    )
}

@Composable
private fun SlCombo(
    labelRes: Int,
    cards: List<Card>,
) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(stringResource(labelRes), color = BvColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
        SlCardRow(cards, 36.dp, Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SlCardRow(
    cards: List<Card>,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        cards.forEach { PlayingCard(it, width = width) }
    }
}
