package com.baiviet.game.tienlen.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.Scrim
import com.baiviet.core.ui.table.TabularNumbers
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoins
import com.baiviet.core.ui.theme.formatSignedCoins
import com.baiviet.game.tienlen.R
import com.baiviet.game.tienlen.rules.TienLenRules
import kotlinx.coroutines.launch

// ───────────────────────── Bảng kết quả ─────────────────────────

@Composable
fun ResultPanel(
    result: ResultUi,
    onNewGame: () -> Unit,
    onChangeTable: () -> Unit,
    onReview: () -> Unit,
) {
    Scrim {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(0.8f).fillMaxHeight(0.88f)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.tl_result_title),
                    color = BvColors.Gold,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(result.rows, key = { it.seat }) { row -> ResultRowView(row) }
                    if (result.capped) {
                        item { Note(stringResource(R.string.tl_result_capped)) }
                    }
                    result.note?.let { item { Note(it) } }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                ) {
                    ActionButton(stringResource(R.string.tl_review), onReview, style = ActionStyle.SECONDARY)
                    ActionButton(stringResource(R.string.tl_change_table), onChangeTable, style = ActionStyle.SECONDARY)
                    ActionButton(stringResource(R.string.tl_new_game), onNewGame, enabled = result.canPlayAgain, pulse = true)
                }
            }
        }
    }
}

@Composable
private fun ResultRowView(row: ResultRow) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(row.delta) { anim.animateTo(row.delta.toFloat(), tween(900)) }
    val color = when {
        row.delta > 0 -> BvColors.WinGreen
        row.delta < 0 -> BvColors.LoseRed
        else -> BvColors.TextSecondary
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (row.isHuman) BvColors.Gold.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.rankLabel,
                color = BvColors.TextOnGold,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (row.rankLabel == stringResource(R.string.tl_rank_first)) BvColors.Gold else BvColors.TextSecondary)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                row.name,
                color = BvColors.Ivory,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (row.isHuman) FontWeight.ExtraBold else FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatSignedCoins(anim.value.toLong()),
                color = color,
                style = MaterialTheme.typography.titleLarge.merge(TabularNumbers),
                fontWeight = FontWeight.ExtraBold,
            )
        }
        row.reasons.forEach {
            Text(it, color = BvColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, color = BvColors.Amber, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(4.dp))
}

// ───────────────────────── Hộp thoại ─────────────────────────

@Composable
fun ExitDialog(penalty: Long, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Scrim(onClick = onCancel) {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 420.dp).clickable(enabled = false) {}) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.tl_exit_title), color = BvColors.Gold, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.tl_exit_message, formatCoins(penalty)),
                    color = BvColors.Ivory,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(stringResource(R.string.tl_cancel), onCancel, style = ActionStyle.SECONDARY)
                    ActionButton(stringResource(R.string.tl_exit), onConfirm, style = ActionStyle.DANGER)
                }
            }
        }
    }
}

@Composable
fun LogDialog(lines: List<String>, onClose: () -> Unit) {
    Scrim(onClick = onClose) {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(0.8f).fillMaxHeight(0.85f)) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.tl_log_title), color = BvColors.Gold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(lines.size) { i ->
                        Text("${i + 1}. ${lines[i]}", color = BvColors.Ivory, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
                ActionButton(stringResource(R.string.tl_close), onClose, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

/** Luật tóm tắt mở từ nút "?" trên bàn. */
@Composable
fun BoxScope.QuickRulesSheet(rules: TienLenRules, betUnit: Long, onFullRules: () -> Unit, onDismiss: () -> Unit) {
    Scrim(onClick = onDismiss) {
        Box(Modifier.fillMaxSize()) {
            GlassPanel(
                strong = true,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.75f)
                    .clickable(enabled = false) {},
            ) {
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    RulesSummary(rules, betUnit)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton(stringResource(R.string.tl_rules_title), onFullRules, style = ActionStyle.SECONDARY)
                        ActionButton(stringResource(R.string.tl_close), onDismiss)
                    }
                }
            }
        }
    }
}

// ───────────────────────── Hướng dẫn nhanh ─────────────────────────

private data class GuidePage(val title: Int, val body: Int, val cards: List<Card>)

private fun card(rank: Rank, suit: Suit) = Card(rank, suit)

private val GUIDE_PAGES = listOf(
    GuidePage(
        R.string.tl_guide_1_title, R.string.tl_guide_1_body,
        listOf(card(Rank.THREE, Suit.SPADE), card(Rank.SEVEN, Suit.HEART), card(Rank.NINE, Suit.CLUB), card(Rank.QUEEN, Suit.DIAMOND), card(Rank.ACE, Suit.HEART)),
    ),
    GuidePage(
        R.string.tl_guide_2_title, R.string.tl_guide_2_body,
        listOf(card(Rank.THREE, Suit.SPADE), card(Rank.KING, Suit.CLUB), card(Rank.ACE, Suit.DIAMOND), card(Rank.TWO, Suit.HEART)),
    ),
    GuidePage(
        R.string.tl_guide_3_title, R.string.tl_guide_3_body,
        listOf(card(Rank.NINE, Suit.CLUB), card(Rank.NINE, Suit.DIAMOND), card(Rank.TEN, Suit.SPADE), card(Rank.TEN, Suit.HEART)),
    ),
    GuidePage(
        R.string.tl_guide_4_title, R.string.tl_guide_4_body,
        listOf(
            card(Rank.FIVE, Suit.SPADE), card(Rank.FIVE, Suit.HEART), card(Rank.SIX, Suit.CLUB),
            card(Rank.SIX, Suit.DIAMOND), card(Rank.SEVEN, Suit.SPADE), card(Rank.SEVEN, Suit.HEART), card(Rank.TWO, Suit.HEART),
        ),
    ),
    GuidePage(
        R.string.tl_guide_5_title, R.string.tl_guide_5_body,
        listOf(card(Rank.FOUR, Suit.SPADE), card(Rank.FIVE, Suit.CLUB), card(Rank.SIX, Suit.HEART)),
    ),
)

/** 5 thẻ trượt, hiện tự động lần đầu vào game. */
@Composable
fun QuickGuide(onDone: () -> Unit) {
    val pager = rememberPagerState { GUIDE_PAGES.size }
    val scope = rememberCoroutineScope()
    Scrim {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(0.8f)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.tl_guide_title), color = BvColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
                HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp)) { page ->
                    val p = GUIDE_PAGES[page]
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text(stringResource(p.title), color = BvColors.Gold, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        CardRow(p.cards, 40.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(p.body), color = BvColors.Ivory, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    repeat(GUIDE_PAGES.size) { i ->
                        Box(
                            Modifier
                                .size(if (i == pager.currentPage) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (i == pager.currentPage) BvColors.Gold else BvColors.TextMuted),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(stringResource(R.string.tl_guide_skip), onDone, style = ActionStyle.SECONDARY)
                    val last = pager.currentPage == GUIDE_PAGES.lastIndex
                    ActionButton(
                        stringResource(if (last) R.string.tl_guide_done else R.string.tl_guide_next),
                        onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        pulse = last,
                    )
                }
            }
        }
    }
}

/** Hàng lá bài minh họa (chồng nhẹ lên nhau). */
@Composable
fun CardRow(cards: List<Card>, width: Dp, modifier: Modifier = Modifier) {
    val step = width * 0.6f
    Box(modifier.width(width + step * (cards.size - 1).coerceAtLeast(0)).height(width * 1.45f)) {
        cards.forEachIndexed { i, c ->
            PlayingCard(card = c, width = width, modifier = Modifier.offset(x = step * i), elevation = 2.dp)
        }
    }
}
