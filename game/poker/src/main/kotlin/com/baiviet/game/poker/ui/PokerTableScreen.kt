package com.baiviet.game.poker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Suit
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.LockLandscape
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.table.TableFelt
import com.baiviet.core.ui.table.opponentAlignments
import com.baiviet.core.ui.table.rememberTurnProgress
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoins
import com.baiviet.core.ui.theme.formatCoinsShort
import com.baiviet.game.poker.engine.PokerAction
import com.baiviet.game.poker.engine.PokerPlayerInfo
import com.baiviet.game.poker.engine.PokerStreet
import com.baiviet.game.poker.engine.PokerView

@Composable
fun PokerTableScreen(
    players: Int,
    difficulty: Int,
    betUnit: Long,
    buyInBB: Int = 0,
    onNavigateBack: () -> Unit,
    onOpenRules: () -> Unit,
    viewModel: PokerViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.start(players, difficulty, betUnit, buyInBB) }
    val toastContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effects.collect { e ->
            when (e) {
                PokerEffect.NavigateBack -> onNavigateBack()
                is PokerEffect.ShowToast -> android.widget.Toast.makeText(toastContext, e.message, android.widget.Toast.LENGTH_SHORT).show()
                else -> Unit
            }
        }
    }
    LockLandscape()
    BackHandler { viewModel.onIntent(PokerIntent.RequestExit) }
    val state by viewModel.uiState.collectAsState()
    val view = state.view

    TableFelt(felt = FeltColor.GREEN) {
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
                            onClick = { viewModel.onIntent(PokerIntent.RequestExit) },
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "POKER TEXAS HOLD\'EM",
                            style = MaterialTheme.typography.titleMedium,
                            color = BvColors.Gold,
                            fontWeight = FontWeight.Bold,
                        )
                        if (view != null) {
                            Spacer(Modifier.width(8.dp))
                            val streetText = when (view.street) {
                                PokerStreet.PREFLOP -> "Preflop"
                                PokerStreet.FLOP -> "Flop"
                                PokerStreet.TURN -> "Turn"
                                PokerStreet.RIVER -> "River"
                                PokerStreet.SHOWDOWN -> "Showdown"
                                PokerStreet.FINISHED -> "Kết thúc"
                            }
                            Box(
                                modifier = Modifier
                                    .background(BvColors.FeltGreen, RoundedCornerShape(12.dp))
                                    .border(1.dp, BvColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = streetText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BvColors.Ivory,
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Practice Mode Toggle
                        RoundIconButton(
                            glyph = if (state.practiceMode) "🎯" else "👁",
                            contentDescription = "Luyện tập",
                            onClick = { viewModel.onIntent(PokerIntent.TogglePracticeMode) },
                        )
                        Spacer(Modifier.width(8.dp))
                        RoundIconButton(
                            glyph = "+$",
                            contentDescription = "Nạp chip",
                            onClick = { viewModel.onIntent(PokerIntent.RequestRebuy) },
                        )
                        Spacer(Modifier.width(8.dp))
                        RoundIconButton(
                            glyph = "?",
                            contentDescription = "Luật chơi",
                            onClick = onOpenRules,
                        )
                    }
                }

                // Table felt area with seats & center board
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (view != null) {
                        // Center Board: Pot & Community Cards
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Pot display
                            Box(
                                modifier = Modifier
                                    .background(BvColors.GlassBg, RoundedCornerShape(16.dp))
                                    .border(1.dp, BvColors.Gold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "POT: ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BvColors.Gold,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = formatCoins(view.pot),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BvColors.Ivory,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // 5 Community Cards
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                for (i in 0 until 5) {
                                    val card = view.communityCards.getOrNull(i)
                                    if (card != null) {
                                        PlayingCard(
                                            card = card,
                                            width = 54.dp,
                                        )
                                    } else {
                                        // Placeholder for upcoming community cards
                                        Box(
                                            modifier = Modifier
                                                .width(54.dp)
                                                .height(76.dp)
                                                .background(
                                                    Color.Black.copy(alpha = 0.25f),
                                                    RoundedCornerShape(6.dp),
                                                )
                                                .border(
                                                    1.dp,
                                                    Color.White.copy(alpha = 0.15f),
                                                    RoundedCornerShape(6.dp),
                                                ),
                                        )
                                    }
                                }
                            }
                        }

                        // Ghế đối thủ theo chiều đi ngược kim đồng hồ: Bạn → phải → trên → trái
                        val p0 = view.players.find { it.id.seat == 0 }
                        val opponents = view.players.filter { it.id.seat != 0 }.sortedBy { it.id.seat }
                        val positions = opponentAlignments(opponents.size)
                        val progress = rememberTurnProgress(
                            startedAt = state.turnStartedAt,
                            seconds = view.rules.turnSeconds,
                            active = view.currentActor != null && state.settlement == null,
                            tickSound = state.isHumanTurn,
                        )
                        opponents.forEachIndexed { i, p ->
                            Box(modifier = Modifier.align(positions[i]).padding(horizontal = 16.dp, vertical = 4.dp)) {
                                PokerSeatWidget(
                                    player = p,
                                    name = state.names[p.id] ?: "Máy ${p.id.seat}",
                                    isTurn = view.currentActor == p.id,
                                    progress = if (view.currentActor == p.id) progress else null,
                                )
                            }
                        }

                        // Bottom Center (Human Player 0)
                        p0?.let {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                // Human Hand Evaluation Badge
                                view.myHand?.let { hand ->
                                    Box(
                                        modifier = Modifier
                                            .background(BvColors.FeltGreen, RoundedCornerShape(12.dp))
                                            .border(1.dp, BvColors.Gold, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = hand.description.ifEmpty { hand.type.vietnameseName },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BvColors.Gold,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }

                                if (state.practiceMode) {
                                    val winPct = (state.practiceEquity * 100).toInt()
                                    Text(
                                        text = "Tỉ lệ thắng: $winPct%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (winPct >= 50) BvColors.Gold else Color.LightGray,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // 2 Hole cards
                                    for (card in view.myHoleCards) {
                                        PlayingCard(
                                            card = card,
                                            width = 62.dp,
                                        )
                                    }

                                    Spacer(Modifier.width(4.dp))

                                    // Stack info
                                    Column {
                                        Text(
                                            text = "Bạn",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                        )
                                        Text(
                                            text = "Stack: ${formatCoinsShort(it.stack)}",
                                            color = BvColors.Gold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        if (it.currentStreetBet > 0) {
                                            Text(
                                                text = "Cược: ${formatCoinsShort(it.currentStreetBet)}",
                                                color = Color.Yellow,
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar
                if (view != null) {
                    PokerActionBar(
                        view = view,
                        isHumanTurn = state.isHumanTurn,
                        betSliderValue = state.betSliderValue,
                        onIntent = viewModel::onIntent,
                    )
                }
            }

            // Results Dialog
            if (state.showResultDialog) {
                PokerResultDialog(
                    results = state.view?.results.orEmpty(),
                    names = state.names,
                    onNextHand = { viewModel.onIntent(PokerIntent.NextHand) },
                )
            }

            // Exit Dialog
            if (state.showExitDialog) {
                Dialog(onDismissRequest = { viewModel.onIntent(PokerIntent.DismissExit) }) {
                    GlassPanel(modifier = Modifier.padding(16.dp)) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Rời bàn chơi?",
                                style = MaterialTheme.typography.titleMedium,
                                color = BvColors.Ivory,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Rời bàn giữa ván coi như Úp: mất số chip đã cược trong ván này. Stack còn lại vẫn nằm trong ví.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                ActionButton(
                                    text = "Ở lại",
                                    style = ActionStyle.SECONDARY,
                                    onClick = { viewModel.onIntent(PokerIntent.DismissExit) },
                                )
                                ActionButton(
                                    text = "Rời bàn",
                                    style = ActionStyle.DANGER,
                                    onClick = { viewModel.onIntent(PokerIntent.ConfirmExit) },
                                )
                            }
                        }
                    }
                }
            }

            // Rebuy Dialog
            if (state.showRebuyDialog) {
                Dialog(onDismissRequest = { viewModel.onIntent(PokerIntent.DismissRebuy) }) {
                    GlassPanel(modifier = Modifier.padding(16.dp)) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Nạp thêm stack (Rebuy)",
                                style = MaterialTheme.typography.titleMedium,
                                color = BvColors.Gold,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Nạp thêm 100 BB (${formatCoins(state.tableConfig.rules.startingChipsBB * state.tableConfig.betUnit)}) vào stack chơi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                ActionButton(
                                    text = "Hủy",
                                    style = ActionStyle.SECONDARY,
                                    onClick = { viewModel.onIntent(PokerIntent.DismissRebuy) },
                                )
                                ActionButton(
                                    text = "Nạp ngay",
                                    style = ActionStyle.PRIMARY,
                                    onClick = { viewModel.onIntent(PokerIntent.ConfirmRebuy) },
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
private fun PokerSeatWidget(
    player: PokerPlayerInfo,
    name: String,
    isTurn: Boolean,
    progress: Float?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                if (isTurn) BvColors.Gold.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.4f),
                RoundedCornerShape(10.dp),
            )
            .border(
                1.dp,
                if (isTurn) BvColors.Gold else Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(10.dp),
            )
            .padding(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Dealer Button or Blind badge
            if (player.isDealer) {
                Box(
                    modifier = Modifier
                        .background(BvColors.Gold, CircleShape)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text("D", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            } else if (player.isSB) {
                Box(
                    modifier = Modifier
                        .background(Color.Blue.copy(alpha = 0.7f), CircleShape)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text("SB", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else if (player.isBB) {
                Box(
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.7f), CircleShape)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text("BB", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(4.dp))

        // Cards (face down back or shown)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (player.holeCards != null) {
                for (card in player.holeCards) {
                    PlayingCard(card = card, width = 34.dp)
                }
            } else {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (player.isFolded) Color.DarkGray else Color(0xFF1E3A8A))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = if (player.isFolded) "Đã úp" else if (player.isAllIn) "ALL-IN" else formatCoinsShort(player.stack),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (player.isFolded) Color.Gray else BvColors.Gold,
        )

        if (player.currentStreetBet > 0) {
            Text(
                text = "Cược: ${formatCoinsShort(player.currentStreetBet)}",
                fontSize = 9.sp,
                color = Color.Yellow,
            )
        }
        if (progress != null) TurnBar(progress)
    }
}

/** Thanh đếm giờ đổi màu xanh → vàng → đỏ. */
@Composable
private fun TurnBar(progress: Float) {
    val color = when {
        progress > 0.5f -> BvColors.Teal
        progress > 0.2f -> BvColors.Gold
        else -> BvColors.Red
    }
    Box(
        Modifier
            .padding(top = 3.dp)
            .width(70.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.15f)),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(color))
    }
}

@Composable
private fun PokerActionBar(
    view: PokerView,
    isHumanTurn: Boolean,
    betSliderValue: Long,
    onIntent: (PokerIntent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (!isHumanTurn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Đang chờ đối thủ hành động...",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                val canCheck = view.toCall == 0L
                val canCall = view.toCall > 0L
                val minRaise = view.minRaiseTotal
                val maxRaise = view.maxRaiseTotal

                // Bet Slider (nếu có thể Bet hoặc Raise)
                if (maxRaise > minRaise) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Mức cược: ${formatCoinsShort(betSliderValue)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BvColors.Gold,
                            modifier = Modifier.width(130.dp),
                        )
                        Slider(
                            value = betSliderValue.toFloat(),
                            onValueChange = { onIntent(PokerIntent.SetBetSlider(it.toLong())) },
                            valueRange = minRaise.toFloat()..maxRaise.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = BvColors.Gold,
                                activeTrackColor = BvColors.Gold,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Fold Button
                    ActionButton(
                        text = "Úp bài",
                        style = ActionStyle.DANGER,
                        onClick = { onIntent(PokerIntent.Fold) },
                    )

                    // Check or Call
                    if (canCheck) {
                        ActionButton(
                            text = "Xem",
                            style = ActionStyle.SECONDARY,
                            onClick = { onIntent(PokerIntent.Check) },
                        )
                    } else if (canCall) {
                        ActionButton(
                            text = "Theo ${formatCoinsShort(view.toCall)}",
                            style = ActionStyle.PRIMARY,
                            onClick = { onIntent(PokerIntent.Call) },
                        )
                    }

                    // Bet or Raise button
                    if (canCheck && maxRaise >= view.rules.startingChipsBB) {
                        ActionButton(
                            text = "Cược ${formatCoinsShort(betSliderValue)}",
                            style = ActionStyle.PRIMARY,
                            onClick = { onIntent(PokerIntent.Bet(betSliderValue)) },
                        )
                    } else if (maxRaise >= minRaise) {
                        ActionButton(
                            text = "Tố ${formatCoinsShort(betSliderValue)}",
                            style = ActionStyle.PRIMARY,
                            onClick = { onIntent(PokerIntent.Raise(betSliderValue)) },
                        )
                    }

                    // All-in button
                    ActionButton(
                        text = "Tất tay",
                        style = ActionStyle.DANGER,
                        onClick = { onIntent(PokerIntent.AllIn) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PokerResultDialog(
    results: List<com.baiviet.game.poker.rules.PokerPlayerResult>,
    onNextHand: () -> Unit,
    names: Map<com.baiviet.core.engine.PlayerId, String> = emptyMap(),
) {
    Dialog(onDismissRequest = onNextHand) {
        GlassPanel(modifier = Modifier.width(420.dp).padding(16.dp), strong = true) {
            Column(
                modifier = Modifier.padding(20.dp).heightIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "KẾT QUẢ VÁN ĐẤU",
                    style = MaterialTheme.typography.titleLarge,
                    color = BvColors.Gold,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(14.dp))

                // Danh sách cuộn được, nút "Ván tiếp theo" luôn hiện bên dưới
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    for (res in results) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (res.isWinner) BvColors.Gold.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = if (res.id.seat == 0) "Bạn" else (names[res.id] ?: "Máy ${res.id.seat}"),
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.isWinner) BvColors.Gold else Color.White,
                                    fontSize = 13.sp,
                                )
                                if (res.hand != null) {
                                    Text(
                                        text = res.hand.description.ifEmpty { res.hand.type.vietnameseName },
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                    )
                                }
                            }

                            val deltaText = if (res.delta > 0) "+${formatCoins(res.delta)}" else formatCoins(res.delta)
                            Text(
                                text = deltaText,
                                fontWeight = FontWeight.Bold,
                                color = if (res.delta > 0) Color.Green else if (res.delta < 0) Color.Red else Color.Gray,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                ActionButton(
                    text = "Ván tiếp theo",
                    style = ActionStyle.PRIMARY,
                    onClick = onNextHand,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
