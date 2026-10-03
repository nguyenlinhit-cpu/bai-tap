package com.baiviet.game.lieng.ui

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
import androidx.compose.runtime.collectAsState
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
import com.baiviet.core.engine.PlayerId
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
import com.baiviet.core.ui.theme.formatCoins
import com.baiviet.core.ui.theme.formatCoinsShort
import com.baiviet.game.lieng.engine.LiengAction
import com.baiviet.game.lieng.engine.LiengPhase

@Composable
fun LiengTableScreen(
    players: Int,
    difficulty: Int,
    betUnit: Long,
    onNavigateBack: () -> Unit,
    onOpenRules: () -> Unit,
    viewModel: LiengViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.start(players, difficulty, betUnit) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LiengEffect.NavigateBack -> onNavigateBack()
                is LiengEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                else -> Unit
            }
        }
    }
    LockLandscape()
    BackHandler { viewModel.onIntent(LiengIntent.RequestExit) }
    val state by viewModel.uiState.collectAsState()
    val view = state.view
    val progress = rememberTurnProgress(
        startedAt = state.turnStartedAt,
        seconds = view?.rules?.turnSeconds ?: 15,
        active = view?.currentActor != null && state.settlement == null,
        tickSound = state.isHumanTurn,
    )

    TableFelt(felt = FeltColor.RED) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoundIconButton(
                            glyph = "✕",
                            contentDescription = "Thoát bàn",
                            onClick = { viewModel.onIntent(LiengIntent.RequestExit) },
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "LIÊNG",
                            style = MaterialTheme.typography.titleMedium,
                            color = BvColors.Gold,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoundIconButton(
                            glyph = "?",
                            contentDescription = "Luật chơi",
                            onClick = onOpenRules,
                        )
                    }
                }

                // Table felt area with seats & center pot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (view != null) {
                        // Center Pot
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .border(1.dp, BvColors.Gold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "POT",
                                    color = BvColors.Gold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${formatCoins(view.pot)} xu",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Vòng ${view.currentRound} / ${view.rules.maxBettingRounds}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                )
                            }
                        }

                        // Opponent Seats
                        val otherPlayers = view.players.filter { it.id != state.humanId }
                        val positions = opponentAlignments(otherPlayers.size)
                        otherPlayers.forEachIndexed { index, pInfo ->
                            val alignment = positions[index]
                            Box(
                                modifier = Modifier
                                    .align(alignment)
                                    .padding(16.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    SeatView(
                                        info = SeatInfo(
                                            name = state.names[pInfo.id] ?: "Máy",
                                            initial = (state.names[pInfo.id] ?: "M").take(1),
                                            coins = state.balances[pInfo.id] ?: 0L,
                                            cardCount = pInfo.cardCount,
                                            status = when {
                                                pInfo.isFolded -> "Đã úp"
                                                pInfo.isAllIn -> "Tất tay"
                                                pInfo.currentRoundBet > 0 -> "Cược ${pInfo.currentRoundBet}"
                                                else -> null
                                            },
                                            isTurn = view.currentActor == pInfo.id,
                                            isHuman = false,
                                        ),
                                        timerProgress = if (view.currentActor == pInfo.id) progress else null,
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy((-20).dp),
                                        modifier = Modifier.padding(top = 4.dp),
                                    ) {
                                        if (pInfo.cards != null) {
                                            pInfo.cards.forEach { card ->
                                                PlayingCard(card = card, width = 42.dp, faceUp = true)
                                            }
                                        } else {
                                            repeat(pInfo.cardCount) {
                                                PlayingCard(card = null, width = 42.dp, faceUp = false)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Human Area at Bottom
                if (view != null) {
                    val humanHand = view.myHand
                    val humanPlayer = view.players.find { it.id == state.humanId }
                    val isFolded = humanPlayer?.isFolded == true

                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        strong = true,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isFolded) "Đã úp bài" else humanHand.description(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isFolded) Color.Gray else BvColors.Gold,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Ví: ${formatCoinsShort(state.balances[state.humanId] ?: 0L)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.8f),
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy((-16).dp)) {
                                    humanHand.cards.forEach { card ->
                                        PlayingCard(card = card, width = 64.dp, faceUp = !isFolded)
                                    }
                                }
                            }

                            // Action Controls
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (state.isHumanTurn && !isFolded) {
                                    val canCheck = state.legalActions.any { it is LiengAction.Check }
                                    val canCall = state.legalActions.any { it is LiengAction.Call }
                                    val canRaise = state.legalActions.any { it is LiengAction.Raise }

                                    ActionButton(
                                        text = "Nặn bài",
                                        onClick = { viewModel.onIntent(LiengIntent.RequestSqueeze) },
                                        style = ActionStyle.SECONDARY,
                                    )

                                    ActionButton(
                                        text = "Úp bài",
                                        onClick = { viewModel.onIntent(LiengIntent.Fold) },
                                        style = ActionStyle.DANGER,
                                    )

                                    if (canCheck) {
                                        ActionButton(
                                            text = "Xem",
                                            onClick = { viewModel.onIntent(LiengIntent.Check) },
                                            style = ActionStyle.PRIMARY,
                                        )
                                    }

                                    if (canCall) {
                                        ActionButton(
                                            text = "Theo (${view.toCall})",
                                            onClick = { viewModel.onIntent(LiengIntent.Call) },
                                            style = ActionStyle.PRIMARY,
                                        )
                                    }

                                    if (canRaise) {
                                        val raises = state.legalActions.filterIsInstance<LiengAction.Raise>().map { it.amount }
                                        listOf(raises.first(), raises.last()).distinct().forEach { amount ->
                                            ActionButton(
                                                text = "Tố +${formatCoinsShort(amount)}",
                                                onClick = { viewModel.onIntent(LiengIntent.Raise(amount)) },
                                                style = ActionStyle.PRIMARY,
                                            )
                                        }
                                    }
                                    if (LiengAction.AllIn in state.legalActions) {
                                        ActionButton(
                                            text = "Tất tay",
                                            onClick = { viewModel.onIntent(LiengIntent.AllIn) },
                                            style = ActionStyle.DANGER,
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (view.phase == LiengPhase.FINISHED) "Ván bài kết thúc" else "Đang chờ đối thủ...",
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card Squeeze Dialog
            if (state.showSqueezeDialog && state.squeezingCard != null) {
                CardSqueezeDialog(
                    card = state.squeezingCard!!,
                    onRevealed = { /* Feedback */ },
                    onDismiss = { viewModel.onIntent(LiengIntent.DismissSqueeze) },
                    title = "Nặn bài Liêng",
                    subtitle = "Vuốt hé lá bài thứ 3 hồi hộp",
                )
            }

            // Result Dialog
            if (state.showResultDialog && state.settlement != null) {
                LiengResultDialog(
                    results = view?.results.orEmpty(),
                    pot = view?.pot ?: 0L,
                    names = state.names,
                    onNextRound = { viewModel.onIntent(LiengIntent.NextRound) },
                )
            }

            // Exit Dialog
            if (state.showExitDialog) {
                Dialog(onDismissRequest = { viewModel.onIntent(LiengIntent.DismissExit) }) {
                    GlassPanel(modifier = Modifier.width(320.dp), strong = true) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Rời bàn chơi?", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("Thoát giữa ván coi như Úp bài: mất số xu đã bỏ vào pot.", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ActionButton(
                                    text = "Ở lại",
                                    onClick = { viewModel.onIntent(LiengIntent.DismissExit) },
                                    modifier = Modifier.weight(1f),
                                    style = ActionStyle.SECONDARY,
                                )
                                ActionButton(
                                    text = "Rời bàn",
                                    onClick = {
                                        viewModel.onIntent(LiengIntent.ConfirmExit)
                                    },
                                    modifier = Modifier.weight(1f),
                                    style = ActionStyle.DANGER,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiengResultDialog(
    results: List<com.baiviet.game.lieng.rules.LiengPlayerResult>,
    pot: Long,
    names: Map<PlayerId, String>,
    onNextRound: () -> Unit,
) {
    Dialog(onDismissRequest = {}) {
        GlassPanel(
            modifier = Modifier
                .width(420.dp)
                .padding(16.dp),
            strong = true,
        ) {
            Column(
                modifier = Modifier.padding(20.dp).heightIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "KẾT QUẢ VÁN BÀI",
                    style = MaterialTheme.typography.titleLarge,
                    color = BvColors.Gold,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Tổng Pot: ${formatCoins(pot)} xu",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))

                // Danh sách cuộn được, nút "Ván tiếp theo" luôn hiện bên dưới
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    results.forEach { res ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = names[res.playerId] ?: "Người chơi ${res.playerId.seat + 1}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "${if (res.delta > 0) "+" else ""}${res.delta} xu",
                                color = if (res.delta > 0) BvColors.Teal else if (res.delta < 0) BvColors.SuitRed else Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = res.reason,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                ActionButton(
                    text = "Ván tiếp theo",
                    onClick = onNextRound,
                    modifier = Modifier.fillMaxWidth(),
                    style = ActionStyle.PRIMARY,
                )
            }
        }
    }
}
