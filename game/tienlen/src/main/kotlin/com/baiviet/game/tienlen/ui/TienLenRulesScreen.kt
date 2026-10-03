package com.baiviet.game.tienlen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.core.data.HouseRulesRepository
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoins
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
class TlRulesViewModel @Inject constructor(repo: HouseRulesRepository) : ViewModel() {
    val rules: StateFlow<TienLenRules> = repo.rules(TienLenRules.GAME_ID, TienLenRules.serializer(), TienLenRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TienLenRules.DEFAULT)
}

private data class Section(val title: Int, val content: @Composable (TienLenRules) -> Unit)

private fun cards(vararg spec: Pair<Rank, Suit>) = spec.map { Card(it.first, it.second) }

/**
 * Màn Luật chơi: có mục lục, cuộn được; số liệu đọc từ RuleConfig đang áp dụng.
 */
@Composable
fun TienLenRulesScreen(onBack: () -> Unit, viewModel: TlRulesViewModel = hiltViewModel()) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val sections = listOf(
        Section(R.string.tl_rules_intro_h) { Body(stringResource(R.string.tl_rules_intro)) },
        Section(R.string.tl_rules_deck_h) { Body(stringResource(R.string.tl_rules_deck)) },
        Section(R.string.tl_rules_order_h) {
            Body(stringResource(R.string.tl_rules_order))
            CardRow(
                cards(
                    Rank.THREE to Suit.SPADE, Rank.FOUR to Suit.SPADE, Rank.TEN to Suit.CLUB, Rank.KING to Suit.DIAMOND,
                    Rank.ACE to Suit.HEART, Rank.TWO to Suit.SPADE, Rank.TWO to Suit.HEART,
                ),
                40.dp,
                Modifier.padding(top = 8.dp),
            )
        },
        Section(R.string.tl_rules_combos_h) {
            Combo(R.string.tl_rules_single, cards(Rank.SEVEN to Suit.HEART))
            Combo(R.string.tl_rules_pair, cards(Rank.NINE to Suit.SPADE, Rank.NINE to Suit.HEART))
            Combo(R.string.tl_rules_triple, cards(Rank.JACK to Suit.CLUB, Rank.JACK to Suit.DIAMOND, Rank.JACK to Suit.HEART))
            Combo(R.string.tl_rules_straight, cards(Rank.QUEEN to Suit.SPADE, Rank.KING to Suit.HEART, Rank.ACE to Suit.DIAMOND))
            Combo(
                R.string.tl_rules_pairseq,
                cards(
                    Rank.FIVE to Suit.SPADE, Rank.FIVE to Suit.HEART, Rank.SIX to Suit.CLUB,
                    Rank.SIX to Suit.DIAMOND, Rank.SEVEN to Suit.SPADE, Rank.SEVEN to Suit.HEART,
                ),
            )
            Combo(R.string.tl_rules_quad, cards(Rank.EIGHT to Suit.SPADE, Rank.EIGHT to Suit.CLUB, Rank.EIGHT to Suit.DIAMOND, Rank.EIGHT to Suit.HEART))
        },
        Section(R.string.tl_rules_flow_h) { r ->
            Body(
                stringResource(
                    R.string.tl_rules_flow,
                    if (r.require3SpadesOnFirstMove) stringResource(R.string.tl_rules_flow_must) else "",
                    stringResource(if (r.scoring == TlScoring.COUNT_CARDS) R.string.tl_rules_flow_end_count else R.string.tl_rules_flow_end_rank),
                ),
            )
        },
        Section(R.string.tl_rules_cut_h) { r ->
            Body(
                stringResource(
                    R.string.tl_rules_cut,
                    if (r.quadCutsPairOfTwos) stringResource(R.string.tl_rules_cut_quad_pair) else "",
                    if (r.fourPairsCutWithoutTurn) stringResource(R.string.tl_rules_cut_noturn) else "",
                ),
            )
            CardRow(
                cards(
                    Rank.TWO to Suit.HEART, Rank.FIVE to Suit.SPADE, Rank.FIVE to Suit.HEART, Rank.SIX to Suit.CLUB,
                    Rank.SIX to Suit.DIAMOND, Rank.SEVEN to Suit.SPADE, Rank.SEVEN to Suit.HEART,
                ),
                36.dp,
                Modifier.padding(top = 8.dp),
            )
        },
        Section(R.string.tl_rules_instant_h) { r ->
            Body(
                stringResource(
                    R.string.tl_rules_instant,
                    if (r.fiveConsecutivePairsInstantWin) stringResource(R.string.tl_rules_instant_fivepairs) else "",
                    stringResource(R.string.tl_rules_instant_sixpairs, r.sameColorInstantWinCount),
                    if (r.quadThreeInstantWinFirstGame) stringResource(R.string.tl_rules_instant_fourthrees) else "",
                ),
            )
        },
        Section(R.string.tl_rules_money_h) { r -> RulesSummary(r, betUnit = null, showTitle = false) },
        Section(R.string.tl_rules_exit_h) { Body(stringResource(R.string.tl_rules_exit)) },
        Section(R.string.tl_rules_terms_h) { Body(stringResource(R.string.tl_rules_terms)) },
        Section(R.string.tl_rules_tips_h) { Body(stringResource(R.string.tl_rules_tips)) },
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BvColors.NavyDark, BvColors.PurpleDark)))
            .safeDrawingPadding(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton("←", stringResource(R.string.tl_close), onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.tl_rules_title), color = BvColors.Gold, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    stringResource(
                        R.string.tl_rules_current,
                        stringResource(if (rules.scoring == TlScoring.COUNT_CARDS) R.string.tl_mode_count else R.string.tl_mode_ranking),
                        stringResource(R.string.tl_house_title).substringBefore(" –"),
                    ),
                    color = BvColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        // Mục lục
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(sections) { index, section ->
                Text(
                    stringResource(section.title),
                    color = BvColors.Ivory,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BvColors.GlassBgStrong)
                        .clickable { scope.launch { listState.animateScrollToItem(index) } }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(sections) { _, section ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(stringResource(section.title), color = BvColors.Teal, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        section.content(rules)
                    }
                }
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(text, color = BvColors.Ivory, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Combo(textRes: Int, example: List<Card>) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Body(stringResource(textRes))
        CardRow(example, 34.dp, Modifier.padding(top = 4.dp))
    }
}

/**
 * Bảng tính tiền đọc từ RuleConfig. [betUnit] khác null → hiện thêm số xu tương ứng.
 */
@Composable
fun RulesSummary(rules: TienLenRules, betUnit: Long?, showTitle: Boolean = true) {
    if (showTitle) {
        Text(stringResource(R.string.tl_rules_money_h), color = BvColors.Gold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
    }
    fun xuOf(units: Int, halves: Boolean): String {
        if (betUnit == null) return ""
        val xu = if (halves) units * betUnit / 2 else units * betUnit
        return " = ${formatCoins(xu)} xu"
    }
    if (rules.scoring == TlScoring.COUNT_CARDS) {
        Body(stringResource(R.string.tl_rules_money_count))
        val cardsLabel: @Composable (Int) -> String = { stringResource(R.string.tl_money_cards, it) }
        MoneyRow(stringResource(R.string.tl_money_left), cardsLabel(1) + xuOf(1, false))
        MoneyRow(stringResource(R.string.tl_money_black_two), cardsLabel(rules.blackTwoCards) + xuOf(rules.blackTwoCards, false))
        MoneyRow(stringResource(R.string.tl_money_red_two), cardsLabel(rules.redTwoCards) + xuOf(rules.redTwoCards, false))
        MoneyRow(stringResource(R.string.tl_money_three_pairs), cardsLabel(rules.threePairsCards) + xuOf(rules.threePairsCards, false))
        MoneyRow(stringResource(R.string.tl_money_quad), cardsLabel(rules.quadCards) + xuOf(rules.quadCards, false))
        MoneyRow(stringResource(R.string.tl_money_four_pairs), cardsLabel(rules.fourPairsCards) + xuOf(rules.fourPairsCards, false))
        MoneyRow(stringResource(R.string.tl_money_cong), cardsLabel(rules.congCards) + xuOf(rules.congCards, false))
        MoneyRow(
            stringResource(R.string.tl_money_instant),
            cardsLabel(rules.instantWinCards) + xuOf(rules.instantWinCards, false) +
                if (rules.instantWinCountsPenalties) " " + stringResource(R.string.tl_money_instant_pen) else "",
        )
    } else {
        Body(stringResource(R.string.tl_rules_money_rank))
        fun b(v: Int) = "${v}B"
        fun h(v: Int) = if (v % 2 == 0) "${v / 2}B" else "${v / 2},5B"
        val p4 = rules.rankingPayout4
        val p3 = rules.rankingPayout3
        val p2 = rules.rankingPayout2
        Body(stringResource(R.string.tl_money_rank_4, b(p4[0]), b(p4[1]), b(p4[2]), b(p4[3])))
        Body(stringResource(R.string.tl_money_rank_3, b(p3[0]), b(p3[1]), b(p3[2])))
        Body(stringResource(R.string.tl_money_rank_2, b(p2[0]), b(p2[1])))
        MoneyRow(stringResource(R.string.tl_money_black_two), h(rules.rankingBlackTwoHalves) + xuOf(rules.rankingBlackTwoHalves, true))
        MoneyRow(stringResource(R.string.tl_money_red_two), h(rules.rankingRedTwoHalves) + xuOf(rules.rankingRedTwoHalves, true))
        MoneyRow(stringResource(R.string.tl_money_three_pairs), h(rules.rankingThreePairsHalves) + xuOf(rules.rankingThreePairsHalves, true))
        MoneyRow(stringResource(R.string.tl_money_quad), h(rules.rankingQuadHalves) + xuOf(rules.rankingQuadHalves, true))
        MoneyRow(stringResource(R.string.tl_money_four_pairs), h(rules.rankingFourPairsHalves) + xuOf(rules.rankingFourPairsHalves, true))
        MoneyRow(stringResource(R.string.tl_money_cong), h(rules.rankingCongHalves) + xuOf(rules.rankingCongHalves, true))
        MoneyRow(stringResource(R.string.tl_money_instant), h(rules.rankingInstantWinHalves) + xuOf(rules.rankingInstantWinHalves, true))
    }
    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.tl_money_note), color = BvColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun MoneyRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, color = BvColors.Ivory, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, color = BvColors.Gold, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
