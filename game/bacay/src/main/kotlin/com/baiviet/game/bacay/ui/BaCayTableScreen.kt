package com.baiviet.game.bacay.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.ui.card.CardSqueezeDialog
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.LockLandscape
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.table.SeatInfo
import com.baiviet.core.ui.table.SeatView
import com.baiviet.core.ui.table.TableFelt
import com.baiviet.core.ui.table.opponentAlignments
import com.baiviet.core.ui.table.rememberTurnProgress
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoinsShort
import com.baiviet.core.ui.theme.formatSignedCoins
import com.baiviet.game.bacay.engine.BaCayPhase
import com.baiviet.game.bacay.engine.BaCayPlayerInfo
import com.baiviet.game.bacay.rules.BaCayMode
import com.baiviet.game.bacay.rules.BaCayPlayerResult

/**
 * Bàn Ba cây (ngang): bạn ở dưới với 2 lá ngửa + 1 lá úp để nặn, các bot quanh bàn.
 *
 * @param dealerOption vai trò cái: 0 = Bạn, 1 = Máy, 2 = Luân phiên
 */
@Composable
fun BaCayTableScreen(
    players: Int,
    difficulty: Int,
    betUnit: Long,
    dealerOption: Int,
    onNavigateBack: () -> Unit,
    onOpenRules: () -> Unit,
    viewModel: BaCayViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.start(players, difficulty, betUnit, dealerOption) }
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                BaCayEffect.NavigateBack -> onNavigateBack()
                is BaCayEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    LockLandscape()
    BackHandler { viewModel.onIntent(BaCayIntent.RequestExit) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = state.view
    val withDealer = state.tableConfig.rules.gameMode == BaCayMode.WITH_DEALER
    val progress = rememberTurnProgress(
        startedAt = state.turnStartedAt,
        seconds = state.tableConfig.rules.turnSeconds,
        active = state.isHumanTurn,
        tickSound = true,
    )

    TableFelt(felt = FeltColor.GREEN) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoundIconButton("✕", "Thoát bàn", onClick = { viewModel.onIntent(BaCayIntent.RequestExit) })
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("BA CÂY", style = MaterialTheme.typography.titleMedium, color = BvColors.Gold, fontWeight = FontWeight.Bold)
                            Text(
                                "Cược ${formatCoinsShort(state.tableConfig.betUnit)} · " +
                                    if (withDealer) dealerModeLabel(state.dealerMode) else "Ăn tất",
                                color = BvColors.TextSecondary,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    RoundIconButton("?", "Luật chơi", onClick = onOpenRules)
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)) {
                    if (view != null) {
                        val opponents = view.players.filter { it.id != state.humanId }
                        val positions = opponentAlignments(opponents.size)
                        opponents.forEachIndexed { index, info ->
                            OpponentSeat(
                                info = info,
                                name = state.names[info.id] ?: "Máy ${info.id.seat}",
                                coins = state.balances[info.id] ?: 0L,
                                isDealer = withDealer && info.id == view.dealerId,
                                modifier = Modifier.align(positions[index]),
                            )
                        }
                    }
                }

                if (view != null) {
                    val hand = view.myHand
                    val me = view.players.find { it.id == state.humanId }
                    val revealed = me?.isRevealed == true
                    val iAmDealer = withDealer && view.dealerId == state.humanId
                    GlassPanel(modifier = Modifier.fillMaxWidth().padding(12.dp), strong = true) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SeatView(
                                info = SeatInfo(
                                    name = if (iAmDealer) "Bạn 👑" else "Bạn",
                                    initial = "B",
                                    coins = state.balances[state.humanId] ?: 0L,
                                    cardCount = hand.cards.size,
                                    isTurn = state.isHumanTurn,
                                    isHuman = true,
                                    avatarColor = if (iAmDealer) BvColors.Gold else BvColors.Teal,
                                ),
                                timerProgress = progress,
                                showCardCount = false,
                                avatarSize = 48.dp,
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    text = if (revealed) hand.description() else "? nút — nặn lá cuối",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BvColors.Gold,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy((-16).dp)) {
                                    hand.cards.forEachIndexed { i, card ->
                                        PlayingCard(card = card, width = 60.dp, faceUp = revealed || i < hand.cards.lastIndex)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (state.isHumanTurn && !revealed) {
                                    ActionButton("Nặn bài", { viewModel.onIntent(BaCayIntent.RequestSqueeze) }, style = ActionStyle.SECONDARY, pulse = true)
                                    ActionButton("Mở bài", { viewModel.onIntent(BaCayIntent.Reveal) })
                                } else {
                                    Text(
                                        text = if (view.phase == BaCayPhase.FINISHED) "Ván bài kết thúc" else "Chờ đối thủ lật bài…",
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.showSqueezeDialog && state.squeezingCard != null) {
                CardSqueezeDialog(
                    card = state.squeezingCard!!,
                    onRevealed = {},
                    onDismiss = { viewModel.onIntent(BaCayIntent.DismissSqueeze) },
                    title = "Nặn bài Ba Cây",
                    subtitle = "Vuốt mở hé lá bài thứ 3 quyết định điểm số",
                )
            }

            val settlement = state.settlement
            if (state.showResultDialog && settlement != null) {
                BaCayResultDialog(
                    results = view?.results.orEmpty(),
                    settlement = settlement,
                    names = state.names,
                    notes = state.notes,
                    canPlayAgain = state.canPlayAgain,
                    onNextRound = { viewModel.onIntent(BaCayIntent.NextRound) },
                )
            }

            if (state.showExitDialog) {
                Dialog(onDismissRequest = { viewModel.onIntent(BaCayIntent.DismissExit) }) {
                    GlassPanel(modifier = Modifier.width(340.dp), strong = true) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rời bàn chơi?", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Thoát giữa ván bị xử thua tiền cược của ván này.",
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ActionButton("Ở lại", { viewModel.onIntent(BaCayIntent.DismissExit) }, modifier = Modifier.weight(1f), style = ActionStyle.SECONDARY)
                                ActionButton("Rời bàn", { viewModel.onIntent(BaCayIntent.ConfirmExit) }, modifier = Modifier.weight(1f), style = ActionStyle.DANGER)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun dealerModeLabel(mode: BaCayDealerMode) = when (mode) {
    BaCayDealerMode.PLAYER_DEALER -> "Bạn làm cái"
    BaCayDealerMode.BOT_DEALER -> "Máy làm cái"
    BaCayDealerMode.ROTATING -> "Cái luân phiên"
}

@Composable
private fun OpponentSeat(
    info: BaCayPlayerInfo,
    name: String,
    coins: Long,
    isDealer: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(4.dp)) {
        if (isDealer) {
            Text(
                "👑 NHÀ CÁI",
                color = BvColors.Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(BvColors.Gold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .border(1.dp, BvColors.Gold, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.height(2.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SeatView(
                info = SeatInfo(
                    name = name,
                    initial = name.take(1),
                    coins = coins,
                    cardCount = info.cardCount,
                    status = if (info.isRevealed) info.hand?.description() ?: "Đã mở" else "Đang nặn",
                    avatarColor = if (isDealer) BvColors.Gold else BvColors.Teal,
                ),
                timerProgress = null,
                avatarSize = 48.dp,
            )
            Spacer(Modifier.width(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy((-22).dp)) {
                val cards = info.cards
                if (cards != null) {
                    cards.forEach { PlayingCard(card = it, width = 40.dp, faceUp = true) }
                } else {
                    repeat(info.cardCount) { PlayingCard(card = null, width = 40.dp, faceUp = false) }
                }
            }
        }
    }
}

/** Bảng kết quả: tiền +/- thực nhận (đã chặn theo số dư) và bài từng nhà. */
@Composable
fun BaCayResultDialog(
    results: List<BaCayPlayerResult>,
    settlement: Settlement,
    names: Map<PlayerId, String>,
    notes: List<String>,
    canPlayAgain: Boolean,
    onNextRound: () -> Unit,
) {
    Dialog(onDismissRequest = {}) {
        GlassPanel(modifier = Modifier.width(440.dp).padding(16.dp), strong = true) {
            Column(
                modifier = Modifier.padding(20.dp).heightIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Phần diễn giải cuộn được, nút "Ván tiếp theo" luôn hiện bên dưới
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("KẾT QUẢ VÁN BÀI", style = MaterialTheme.typography.titleLarge, color = BvColors.Gold, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    settlement.deltas.entries.sortedByDescending { it.value }.forEach { (pid, delta) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(names[pid] ?: "Người chơi ${pid.seat + 1}", color = Color.White, fontWeight = if (pid.seat == 0) FontWeight.Bold else FontWeight.Normal)
                            Text(
                                formatSignedCoins(delta),
                                color = if (delta > 0) BvColors.Teal else if (delta < 0) BvColors.SuitRed else Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (results.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        results.forEach { res ->
                            Text(
                                "${names[res.playerId] ?: "Nhà ${res.playerId.seat + 1}"} (${res.hand.description()}): ${res.reason}",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    notes.forEach {
                        Text(it, color = BvColors.Amber, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                ActionButton(
                    text = if (canPlayAgain) "Ván tiếp theo" else "Hết xu – Rời bàn",
                    onClick = onNextRound,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
