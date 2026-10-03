package com.baiviet.game.samloc.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.Scrim
import com.baiviet.core.ui.table.TabularNumbers
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoins
import com.baiviet.core.ui.theme.formatSignedCoins
import com.baiviet.game.samloc.R
import com.baiviet.game.samloc.rules.SamLocRules
import kotlinx.coroutines.launch

// ───────────────────────── Bảng kết quả ─────────────────────────

@Composable
fun SlResultPanel(
    result: SlResultUi,
    onNewGame: () -> Unit,
    onChangeTable: () -> Unit,
    onReview: () -> Unit,
) {
    Scrim {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(0.85f).fillMaxHeight(0.88f)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.sl_result_title),
                    color = BvColors.Gold,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(result.rows, key = { it.seat }) { row -> SlResultRowView(row) }
                    if (result.capped) {
                        item { SlNote(stringResource(R.string.sl_result_capped)) }
                    }
                    result.note?.let { item { SlNote(it) } }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                ) {
                    ActionButton(stringResource(R.string.sl_review), onReview, style = ActionStyle.SECONDARY)
                    ActionButton(stringResource(R.string.sl_change_table), onChangeTable, style = ActionStyle.SECONDARY)
                    ActionButton(stringResource(R.string.sl_new_game), onNewGame, enabled = result.canPlayAgain, pulse = true)
                }
            }
        }
    }
}

@Composable
private fun SlResultRowView(row: SlResultRow) {
    val deltaColor =
        when {
            row.delta > 0 -> BvColors.Teal
            row.delta < 0 -> BvColors.Red
            else -> BvColors.TextSecondary
        }
    val animCoins = remember(row.delta) { Animatable(0f) }
    LaunchedEffect(row.delta) {
        animCoins.animateTo(row.delta.toFloat(), tween(700))
    }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (row.delta > 0) BvColors.Gold else Color.White.copy(0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    row.rankLabel,
                    color = if (row.delta > 0) BvColors.NavyDark else BvColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    row.name,
                    color = if (row.isHuman) BvColors.Gold else BvColors.TextPrimary,
                    fontWeight = if (row.isHuman) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (row.reasons.isNotEmpty()) {
                    Text(
                        row.reasons.joinToString(" · "),
                        color = BvColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Text(
                formatSignedCoins(animCoins.value.toLong()),
                color = deltaColor,
                style = MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun SlNote(text: String) {
    Text(
        text,
        color = BvColors.TextMuted,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

// ───────────────────────── Cảnh báo Báo 1 ─────────────────────────

@Composable
fun SlBao1WarningDialog(
    nextPlayerName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Scrim {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(0.85f)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.sl_warn_bao_1_title),
                    color = BvColors.Red,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.sl_warn_bao_1_message, nextPlayerName),
                    color = BvColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                    ActionButton(stringResource(R.string.sl_choose_again), onCancel, style = ActionStyle.SECONDARY)
                    ActionButton(stringResource(R.string.sl_confirm_play), onConfirm, style = ActionStyle.DANGER)
                }
            }
        }
    }
}

// ───────────────────────── Pha Báo Sâm ─────────────────────────

@Composable
fun SlBaoSamPromptOverlay(
    secondsLeft: Int,
    onCallSam: () -> Unit,
    onSkipSam: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = 90.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(0.75f)) {
            Column(
                Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.sl_sam_phase_prompt, secondsLeft),
                    color = BvColors.Gold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ActionButton(stringResource(R.string.sl_skip_sam), onSkipSam, style = ActionStyle.SECONDARY)
                    ActionButton(stringResource(R.string.sl_call_sam), onCallSam, style = ActionStyle.PRIMARY, pulse = true)
                }
            }
        }
    }
}

// ───────────────────────── Xem lại ván ─────────────────────────

@Composable
fun SlReviewDialog(
    log: List<String>,
    onClose: () -> Unit,
) {
    Scrim(onClick = onClose) {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(0.85f).fillMaxHeight(0.8f)) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.sl_log_title),
                        color = BvColors.Gold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    ActionButton(stringResource(R.string.sl_close), onClose, style = ActionStyle.SECONDARY)
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(log) { entry ->
                        Text(
                            entry,
                            color = BvColors.TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────── Thoát bàn ─────────────────────────

@Composable
fun SlExitConfirmDialog(
    penalty: Long,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Scrim {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(0.85f)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.sl_exit_title),
                    color = BvColors.Gold,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.sl_exit_message, formatCoins(penalty)),
                    color = BvColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                    ActionButton(stringResource(R.string.sl_cancel), onCancel, style = ActionStyle.SECONDARY)
                    ActionButton(stringResource(R.string.sl_exit), onConfirm, style = ActionStyle.DANGER)
                }
            }
        }
    }
}

// ───────────────────────── Luật nhanh (Bottom Sheet) ─────────────────────────

@Composable
fun SlQuickRulesSheet(
    rules: SamLocRules,
    onClose: () -> Unit,
) {
    Scrim(onClick = onClose) {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(0.9f).fillMaxHeight(0.85f)) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.sl_rules_title),
                        color = BvColors.Gold,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    ActionButton(stringResource(R.string.sl_close), onClose, style = ActionStyle.SECONDARY)
                }
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RuleSection(
                        stringResource(R.string.sl_rules_deck_h),
                        stringResource(R.string.sl_rules_deck),
                    )
                    RuleSection(
                        stringResource(R.string.sl_rules_order_h),
                        stringResource(R.string.sl_rules_order),
                    )
                    RuleSection(
                        stringResource(R.string.sl_rules_combos_h),
                        "${stringResource(
                            R.string.sl_rules_single,
                        )}\n${stringResource(
                            R.string.sl_rules_pair,
                        )}\n${stringResource(
                            R.string.sl_rules_triple,
                        )}\n${stringResource(R.string.sl_rules_quad)}\n${stringResource(R.string.sl_rules_straight)}",
                    )
                    RuleSection(
                        stringResource(R.string.sl_rules_cut_h),
                        stringResource(R.string.sl_rules_cut, rules.cutCards),
                    )
                    RuleSection(
                        stringResource(R.string.sl_rules_sam_h),
                        stringResource(R.string.sl_rules_sam, rules.samFailPenaltyCards, rules.samWinCards),
                    )
                    RuleSection(
                        stringResource(R.string.sl_rules_bao_1_h),
                        stringResource(R.string.sl_rules_bao_1),
                    )
                    RuleSection(
                        stringResource(R.string.sl_rules_finish_two_h),
                        stringResource(R.string.sl_rules_finish_two, rules.finishWithTwoPenaltyCards),
                    )
                    RuleSection(
                        stringResource(R.string.sl_rules_instant_h),
                        stringResource(R.string.sl_rules_instant, rules.instantWinCards),
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleSection(
    title: String,
    content: String,
) {
    Column {
        Text(title, color = BvColors.Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(2.dp))
        Text(content, color = BvColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────── Hướng dẫn nhanh ─────────────────────────

@Composable
fun SlQuickGuideDialog(onDismiss: () -> Unit) {
    val pagerState = rememberPagerState { 5 }
    val scope = rememberCoroutineScope()

    Scrim(onClick = onDismiss) {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(0.9f).heightIn(max = 420.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.sl_guide_title),
                        color = BvColors.Gold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    ActionButton(stringResource(R.string.sl_guide_skip), onDismiss, style = ActionStyle.SECONDARY)
                }
                Spacer(Modifier.height(10.dp))
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    GuidePage(page)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(5) { i ->
                            Box(
                                Modifier
                                    .size(if (pagerState.currentPage == i) 14.dp else 8.dp, 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (pagerState.currentPage == i) BvColors.Gold else Color.White.copy(0.2f)),
                            )
                        }
                    }
                    if (pagerState.currentPage < 4) {
                        ActionButton(
                            stringResource(R.string.sl_guide_next),
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        )
                    } else {
                        ActionButton(stringResource(R.string.sl_guide_done), onClick = onDismiss, pulse = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidePage(index: Int) {
    val (titleRes, bodyRes) =
        when (index) {
            0 -> R.string.sl_guide_1_title to R.string.sl_guide_1_body
            1 -> R.string.sl_guide_2_title to R.string.sl_guide_2_body
            2 -> R.string.sl_guide_3_title to R.string.sl_guide_3_body
            3 -> R.string.sl_guide_4_title to R.string.sl_guide_4_body
            else -> R.string.sl_guide_5_title to R.string.sl_guide_5_body
        }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(titleRes),
            color = BvColors.Gold,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(bodyRes),
            color = BvColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
