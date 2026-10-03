package com.baiviet.game.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baiviet.core.data.EconomyConfig
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.CoinCounter
import com.baiviet.core.ui.table.CoinIcon
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoins
import com.baiviet.core.ui.theme.formatCoinsShort
import com.baiviet.game.R
import com.baiviet.game.catalog.GameCatalog
import com.baiviet.game.ui.guide.QuickGuideDialog
import com.baiviet.game.ui.guide.QuickGuides
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Chọn bàn: số người (minh họa ghế), độ khó, mức cược (khóa mức không đủ xu),
 * các nút Luật nhà, Luật chơi, Hướng dẫn, Ván tập, Vào bàn.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TableSetupScreen(
    gameId: String,
    onBack: () -> Unit,
    onEnter: (players: Int, difficulty: Int, bet: Long, option: Int) -> Unit,
    onRules: () -> Unit,
    onHouseRules: () -> Unit,
    onTutorial: () -> Unit,
    viewModel: TableSetupViewModel = hiltViewModel(),
) {
    LaunchedEffect(gameId) { viewModel.init(gameId) }
    LifecycleResumeEffect(gameId) {
        viewModel.refreshResume()
        onPauseOrDispose {}
    }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val game = GameCatalog.byId(gameId)
    var showGuide by remember { mutableStateOf(false) }
    var autoGuideShown by remember { mutableStateOf(false) }
    // Hướng dẫn nhanh tự hiện lần đầu vào game
    LaunchedEffect(ui.gameId, ui.guideSeen) {
        if (ui.gameId == gameId && !ui.guideSeen && !autoGuideShown) {
            autoGuideShown = true
            showGuide = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BvColors.NavyDark, BvColors.PurpleDark))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton("←", stringResource(R.string.back), onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(game.nameRes), color = BvColors.Gold, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.table_setup), color = BvColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
                CoinIcon(18.dp)
                Spacer(Modifier.width(4.dp))
                CoinCounter(ui.balance, style = MaterialTheme.typography.titleMedium)
            }

            if (!game.available) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.coming_soon_desc),
                        color = BvColors.Ivory,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            val resume = ui.resume
            if (resume != null) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Bạn có ván đang dở", color = BvColors.Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${resume.players} người · cược ${formatCoinsShort(resume.bet)} — vào bàn để chơi tiếp đúng chỗ đang dừng.",
                            color = BvColors.Ivory,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionButton(
                                "Chơi tiếp",
                                { onEnter(resume.players, resume.difficulty, resume.bet, resume.option) },
                                pulse = true,
                            )
                            if (resume.stateJson == null) {
                                // Đang nghỉ giữa hai ván: được mở bàn mới
                                ActionButton("Mở bàn mới", viewModel::discardResume, style = ActionStyle.SECONDARY)
                            }
                        }
                    }
                }
            }

            // Số người
            Section(stringResource(R.string.player_count)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(2, 3, 4).forEach { n ->
                        SeatOption(n, selected = ui.choice.players == n, onClick = { viewModel.setPlayers(n) })
                    }
                }
            }

            // Độ khó
            Section(stringResource(R.string.difficulty)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(R.string.difficulty_easy, R.string.difficulty_normal, R.string.difficulty_hard).forEachIndexed { i, res ->
                        Chip(stringResource(res), ui.choice.difficulty == i, enabled = true) { viewModel.setDifficulty(i) }
                    }
                }
            }

            // Vai trò nhà cái (Xì dách, Ba cây) / Số xu mang vào bàn (Poker)
            if (gameId == "xidach" || gameId == "bacay") {
                Section("Nhà cái") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Bạn làm cái", "Máy làm cái", "Luân phiên").forEachIndexed { i, label ->
                            Chip(label, ui.choice.option == i, enabled = true) { viewModel.setOption(i) }
                        }
                    }
                }
            }
            if (gameId == "poker") {
                Section("Mua vào (buy-in)") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(50, 100, 200).forEach { bb ->
                            Chip("$bb BB", ui.choice.option == bb, enabled = true) { viewModel.setOption(bb) }
                        }
                    }
                    Text(
                        "Mang vào bàn ${formatCoins(ui.bet * ui.choice.option)} xu (mù lớn = mức cược).",
                        color = BvColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            // Mức cược
            Section(stringResource(R.string.bet_level)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EconomyConfig.BET_LEVELS.forEachIndexed { i, bet ->
                        val unlocked = ui.isBetUnlocked(i)
                        Chip(
                            text = if (unlocked) formatCoinsShort(bet) else "🔒 " + formatCoinsShort(bet),
                            selected = ui.choice.betIndex == i,
                            enabled = unlocked,
                        ) { viewModel.setBet(i) }
                    }
                }
                Text(
                    stringResource(R.string.min_balance, formatCoins(ui.minBalanceFor(ui.bet))),
                    color = if (ui.canEnter) BvColors.TextSecondary else BvColors.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(stringResource(R.string.house_rules), onHouseRules, enabled = game.available, style = ActionStyle.SECONDARY)
                ActionButton(stringResource(R.string.game_rules), onRules, enabled = game.available, style = ActionStyle.SECONDARY)
                ActionButton(stringResource(R.string.tutorial), { showGuide = true }, enabled = game.available, style = ActionStyle.SECONDARY)
                ActionButton(stringResource(R.string.practice), onTutorial, enabled = game.available, style = ActionStyle.SECONDARY)
            }
            ActionButton(
                text = stringResource(R.string.enter_table),
                onClick = {
                    val r = ui.resume
                    if (r != null) onEnter(r.players, r.difficulty, r.bet, r.option)
                    else onEnter(ui.choice.players, ui.choice.difficulty, ui.bet, ui.choice.option)
                },
                enabled = game.available && ui.canEnter,
                pulse = true,
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).align(Alignment.CenterHorizontally),
            )
            if (!ui.canEnter) {
                Text(stringResource(R.string.not_enough_coins), color = BvColors.Red, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
        if (showGuide) {
            QuickGuideDialog(
                title = "Hướng dẫn nhanh · ${stringResource(game.nameRes)}",
                pages = QuickGuides.forGame(gameId),
                onDone = {
                    showGuide = false
                    viewModel.markGuideSeen()
                },
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = BvColors.Teal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) BvColors.TextOnGold else BvColors.Ivory,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(50))
            .background(if (selected) BvColors.Gold else BvColors.GlassBgStrong)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

/** Minh họa ghế quanh bàn tròn cho 2/3/4 người. */
@Composable
private fun SeatOption(players: Int, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) BvColors.Gold.copy(alpha = 0.2f) else BvColors.GlassBg)
            .border(if (selected) 2.dp else 1.dp, if (selected) BvColors.Gold else BvColors.GlassBorder, RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(BvColors.FeltGreen))
            val positions = when (players) {
                2 -> listOf(Alignment.BottomCenter, Alignment.TopCenter)
                3 -> listOf(Alignment.BottomCenter, Alignment.TopEnd, Alignment.TopStart)
                else -> listOf(Alignment.BottomCenter, Alignment.CenterEnd, Alignment.TopCenter, Alignment.CenterStart)
            }
            positions.forEachIndexed { i, a ->
                Box(
                    Modifier
                        .align(a)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (i == 0) BvColors.Gold else BvColors.Teal),
                )
            }
        }
        Text(
            stringResource(
                when (players) {
                    2 -> R.string.players_2
                    3 -> R.string.players_3
                    else -> R.string.players_4
                },
            ),
            color = BvColors.Ivory,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
