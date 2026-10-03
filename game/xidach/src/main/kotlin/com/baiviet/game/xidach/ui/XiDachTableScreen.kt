package com.baiviet.game.xidach.ui

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
import android.widget.Toast
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
import com.baiviet.game.xidach.engine.XiDachAction
import com.baiviet.game.xidach.engine.XiDachPhase
import com.baiviet.game.xidach.engine.XiDachPlayerInfo
import com.baiviet.game.xidach.engine.XiDachPlayerStatus
import com.baiviet.game.xidach.rules.XiDachHandStatus
import com.baiviet.game.xidach.rules.XiDachPlayerResult

/**
 * Bàn Xì dách (ngang): bạn ở dưới, các bot ngồi quanh bàn ngược chiều kim đồng hồ,
 * nhà cái có vương miện. Cái được Xét từng nhà hoặc Xét tất cả khi đủ 15 điểm.
 *
 * @param dealerOption vai trò cái: 0 = Bạn, 1 = Máy, 2 = Luân phiên
 */
@Composable
fun XiDachTableScreen(
    players: Int,
    difficulty: Int,
    betUnit: Long,
    dealerOption: Int,
    onNavigateBack: () -> Unit,
    onOpenRules: () -> Unit,
    viewModel: XiDachViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.start(players, difficulty, betUnit, dealerOption) }
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                XiDachEffect.NavigateBack -> onNavigateBack()
                is XiDachEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    LockLandscape()
    BackHandler { viewModel.onIntent(XiDachIntent.RequestExit) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = state.view
    val progress = rememberTurnProgress(
        startedAt = state.turnStartedAt,
        seconds = state.tableConfig.rules.turnSeconds,
        active = view != null && view.phase != XiDachPhase.FINISHED,
        tickSound = state.isHumanTurn,
    )

    TableFelt(felt = FeltColor.BLUE) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoundIconButton("✕", "Thoát bàn", onClick = { viewModel.onIntent(XiDachIntent.RequestExit) })
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("XÌ DÁCH", style = MaterialTheme.typography.titleMedium, color = BvColors.Gold, fontWeight = FontWeight.Bold)
                            Text(
                                "Cược ${formatCoinsShort(state.tableConfig.betUnit)} · ${dealerModeLabel(state.dealerMode)}",
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
                                isDealer = info.id == view.dealerId,
                                isTurn = view.currentActor == info.id,
                                progress = if (view.currentActor == info.id) progress else null,
                                canInspect = state.isHumanTurn && state.legalActions.any { it is XiDachAction.Inspect && it.target == info.id },
                                onInspect = { viewModel.onIntent(XiDachIntent.Inspect(info.id)) },
                                modifier = Modifier.align(positions[index]),
                            )
                        }
                    }
                }

                if (view != null) {
                    val hand = view.myHand
                    val iAmDealer = view.dealerId == state.humanId
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
                                timerProgress = if (state.isHumanTurn) progress else null,
                                showCardCount = false,
                                avatarSize = 48.dp,
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    text = hand.description(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = when (hand.status) {
                                        XiDachHandStatus.DU_TUOI -> BvColors.Teal
                                        XiDachHandStatus.NON -> Color(0xFFFFB74D)
                                        XiDachHandStatus.QUAC -> BvColors.SuitRed
                                    },
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy((-16).dp)) {
                                    hand.cards.forEach { card -> PlayingCard(card = card, width = 60.dp, faceUp = true) }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (state.isHumanTurn) {
                                    ActionButton("Nặn bài", { viewModel.onIntent(XiDachIntent.RequestSqueeze) }, style = ActionStyle.SECONDARY)
                                    if (state.legalActions.any { it is XiDachAction.Hit }) {
                                        ActionButton("Rút", { viewModel.onIntent(XiDachIntent.Hit) })
                                    }
                                    if (state.legalActions.any { it is XiDachAction.Stand }) {
                                        ActionButton("Dằn", { viewModel.onIntent(XiDachIntent.Stand) }, style = ActionStyle.SECONDARY)
                                    }
                                    if (state.legalActions.any { it is XiDachAction.InspectAll }) {
                                        ActionButton("Xét tất cả", { viewModel.onIntent(XiDachIntent.InspectAll) }, pulse = true)
                                    }
                                } else {
                                    Text(
                                        text = when {
                                            view.phase == XiDachPhase.FINISHED -> "Ván bài kết thúc"
                                            view.currentActor != null -> "Chờ ${state.names[view.currentActor] ?: "đối thủ"}…"
                                            else -> "Đang chia bài…"
                                        },
                                        color = Color.White.copy(alpha = 0.6f),
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
                    onDismiss = { viewModel.onIntent(XiDachIntent.DismissSqueeze) },
                )
            }

            val settlement = state.settlement
            if (state.showResultDialog && settlement != null) {
                XiDachResultDialog(
                    results = view?.results.orEmpty(),
                    settlement = settlement,
                    names = state.names,
                    notes = state.notes,
                    canPlayAgain = state.canPlayAgain,
                    onNextRound = { viewModel.onIntent(XiDachIntent.NextRound) },
                )
            }

            if (state.showExitDialog) {
                ExitDialog(
                    onStay = { viewModel.onIntent(XiDachIntent.DismissExit) },
                    onLeave = { viewModel.onIntent(XiDachIntent.ConfirmExit) },
                )
            }
        }
    }
}

private fun dealerModeLabel(mode: XiDachDealerMode) = when (mode) {
    XiDachDealerMode.PLAYER_DEALER -> "Bạn làm cái"
    XiDachDealerMode.BOT_DEALER -> "Máy làm cái"
    XiDachDealerMode.ROTATING -> "Cái luân phiên"
}

@Composable
private fun OpponentSeat(
    info: XiDachPlayerInfo,
    name: String,
    coins: Long,
    isDealer: Boolean,
    isTurn: Boolean,
    progress: Float?,
    canInspect: Boolean,
    onInspect: () -> Unit,
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
                    status = when (info.status) {
                        XiDachPlayerStatus.STOOD -> "Dằn"
                        XiDachPlayerStatus.BUST -> "Quắc"
                        XiDachPlayerStatus.RESOLVED -> if (isDealer) null else "Đã xét"
                        else -> null
                    },
                    isTurn = isTurn,
                    avatarColor = if (isDealer) BvColors.Gold else BvColors.Teal,
                ),
                timerProgress = progress,
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
        if (canInspect) {
            Spacer(Modifier.height(4.dp))
            ActionButton("Xét", onInspect, style = ActionStyle.SECONDARY)
        }
    }
}

@Composable
private fun ExitDialog(onStay: () -> Unit, onLeave: () -> Unit) {
    Dialog(onDismissRequest = onStay) {
        GlassPanel(modifier = Modifier.width(340.dp), strong = true) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rời bàn chơi?", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Thoát giữa ván bị xử thua: làm con mất tiền cược, làm cái trả cược cho các nhà chưa xét.",
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton("Ở lại", onStay, modifier = Modifier.weight(1f), style = ActionStyle.SECONDARY)
                    ActionButton("Rời bàn", onLeave, modifier = Modifier.weight(1f), style = ActionStyle.DANGER)
                }
            }
        }
    }
}

/** Bảng kết quả: tiền +/- thực nhận của từng người (đã chặn theo số dư) và lý do từng nhà. */
@Composable
fun XiDachResultDialog(
    results: List<XiDachPlayerResult>,
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
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
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
                                "${names[res.playerId] ?: "Nhà ${res.playerId.seat + 1}"}: ${res.reason}",
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
