package com.baiviet.game.phom.ui

import com.baiviet.core.ui.table.rememberTurnProgress
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baiviet.core.ui.card.LocalCardStyle
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.effects.BigBanner
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.HandFan
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.table.SeatView
import com.baiviet.core.ui.table.TableFelt
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoinsShort
import com.baiviet.game.phom.R
import com.baiviet.game.phom.engine.PhomPhase

@Composable
fun PhomTableScreen(
    players: Int = 4,
    difficulty: Int = 1,
    betUnit: Long = 100L,
    onExit: () -> Unit,
    onOpenRules: () -> Unit,
    viewModel: PhomViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.start(players, difficulty, betUnit) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LockLandscape()
    BackHandler { viewModel.onIntent(PhomIntent.ExitClicked) }

    LaunchedEffect(Unit) {
        viewModel.effectFlow.collect { effect ->
            when (effect) {
                is PhomEffect.Haptic -> haptic.performHapticFeedback(
                    if (effect.strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
                )
                is PhomEffect.NavigateBack -> onExit()
            }
        }
    }

    PhomTable(
        state = state,
        onIntent = viewModel::onIntent,
        onQuickRules = onOpenRules,
    )

    state.result?.let { res ->
        PhomResultDialog(
            result = res,
            onPlayAgain = { viewModel.onIntent(PhomIntent.PlayAgain) },
            onReview = { viewModel.onIntent(PhomIntent.OpenReview) },
            onExit = { viewModel.onIntent(PhomIntent.ExitClicked) },
            notes = state.notes,
            canPlayAgain = state.canPlayAgain,
        )
    }

    if (state.exitConfirmOpen) {
        PhomExitDialog(
            onConfirm = { viewModel.onIntent(PhomIntent.ConfirmExit) },
            onDismiss = { viewModel.onIntent(PhomIntent.DismissExit) },
        )
    }

    if (state.reviewOpen) {
        PhomReviewDialog(
            state = state,
            onDismiss = { viewModel.onIntent(PhomIntent.DismissReview) },
        )
    }
}

@Composable
fun PhomTable(
    state: PhomUiState,
    onIntent: (PhomIntent) -> Unit,
    onQuickRules: () -> Unit,
) {
    val cardWidth = if (state.cardStyle.large) 64.dp else 54.dp

    CompositionLocalProvider(LocalCardStyle provides state.cardStyle) {
        TableFelt(felt = state.felt) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                // Top bar
                TopBar(state, onIntent, onQuickRules)

                // Seats
                val anchors = PhomSeatLayout.anchors(state.playerCount)
                val progress = rememberTurnProgress(
                    startedAt = state.turnStartedAt,
                    seconds = (state.turnMillis / 1000L).toInt(),
                    active = state.result == null && state.seats.any { it.info.isTurn },
                    tickSound = state.isMyTurn,
                )
                state.seats.forEachIndexed { i, seatUi ->
                    val anchor = anchors.getOrNull(i) ?: Alignment.Center
                    Box(Modifier.fillMaxSize(), contentAlignment = anchor) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                            SeatView(
                                info = seatUi.info,
                                timerProgress = if (seatUi.info.isTurn) progress else null,
                                showCardCount = !seatUi.info.isHuman,
                            )
                            // Phỏm đã hạ của ghế (bạn: hiện ngay trên tay bài)
                            val melds = state.exposedMelds.getOrNull(i).orEmpty()
                            if (i != 0 && melds.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
                                    melds.forEach { meld ->
                                        Row(horizontalArrangement = Arrangement.spacedBy((-16).dp)) {
                                            meld.cards.forEach { PlayingCard(card = it, width = 26.dp) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Center area: Nọc & Last Discard & Melds
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CenterArea(state, cardWidth, onIntent)
                }

                // Human Hand
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        state.message?.let {
                            Text(it, color = BvColors.Amber, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        val myMelds = state.exposedMelds.getOrNull(0).orEmpty()
                        if (myMelds.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                                myMelds.forEach { meld ->
                                    Row(horizontalArrangement = Arrangement.spacedBy((-18).dp)) {
                                        meld.cards.forEach { PlayingCard(card = it, width = 32.dp) }
                                    }
                                }
                            }
                        }
                        HandFan(
                            cards = state.hand,
                            selected = state.selected,
                            cardWidth = cardWidth,
                            onToggle = { onIntent(PhomIntent.Toggle(it)) },
                            onSweep = {},
                            onSwipeUp = {},
                            // Chừa chỗ cho ghế của bạn (trái) và cột nút (phải)
                            modifier = Modifier.padding(start = 104.dp, end = 136.dp),
                        )
                    }
                }

                // Nút hành động: cột dọc góc dưới phải, không đè lên nọc ở giữa bàn
                Box(Modifier.fillMaxSize().padding(end = 10.dp, bottom = 10.dp), contentAlignment = Alignment.BottomEnd) {
                    ActionControls(state, onIntent)
                }

                // Banner
                state.banner?.let { b ->
                    BigBanner(banner = b)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    state: PhomUiState,
    onIntent: (PhomIntent) -> Unit,
    onQuickRules: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(
                glyph = "←",
                contentDescription = stringResource(R.string.phom_back),
                onClick = { onIntent(PhomIntent.ExitClicked) },
            )
            Spacer(Modifier.width(8.dp))
            GlassPanel(modifier = Modifier.clip(RoundedCornerShape(16.dp))) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🪙", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatCoinsShort(state.betUnit),
                        color = BvColors.Gold,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundIconButton("?", stringResource(R.string.phom_help), onClick = onQuickRules)
        }
    }
}

@Composable
private fun CenterArea(
    state: PhomUiState,
    cardWidth: androidx.compose.ui.unit.Dp,
    onIntent: (PhomIntent) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Nọc (Stock deck)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(enabled = state.isMyTurn && state.canDraw) {
                onIntent(PhomIntent.Draw)
            },
        ) {
            Box {
                PlayingCard(card = null, faceUp = false, width = cardWidth)
                Text(
                    text = "${state.stockCount}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Nọc",
                color = BvColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        // Lá rác vừa đánh
        state.lastDiscard?.let { card ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlayingCard(card = card, width = cardWidth)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Vừa đánh",
                    color = BvColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ActionControls(
    state: PhomUiState,
    onIntent: (PhomIntent) -> Unit,
) {
    if (!state.isMyTurn) return

    val w = Modifier.width(120.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
        if (state.canU) {
            ActionButton(
                text = stringResource(R.string.phom_action_u),
                onClick = { onIntent(PhomIntent.DeclareU) },
                style = ActionStyle.PRIMARY,
                modifier = w,
            )
        }
        if (state.canUKhan) {
            ActionButton(
                text = stringResource(R.string.phom_action_u_khan),
                onClick = { onIntent(PhomIntent.DeclareUKhan) },
                style = ActionStyle.PRIMARY,
                modifier = w,
            )
        }
        if (state.canDraw) {
            ActionButton(
                text = stringResource(R.string.phom_action_draw),
                onClick = { onIntent(PhomIntent.Draw) },
                style = ActionStyle.SECONDARY,
                modifier = w,
            )
        }
        if (state.canEat) {
            ActionButton(
                text = stringResource(R.string.phom_action_eat),
                onClick = { onIntent(PhomIntent.Eat) },
                style = ActionStyle.PRIMARY,
                modifier = w,
            )
        }
        if (state.canMeld) {
            ActionButton(
                text = stringResource(R.string.phom_action_meld),
                onClick = { onIntent(PhomIntent.Meld) },
                style = ActionStyle.PRIMARY,
                modifier = w,
            )
        }
        if (state.canLayOff) {
            ActionButton(
                text = stringResource(R.string.phom_action_layoff),
                onClick = { onIntent(PhomIntent.LayOff) },
                style = ActionStyle.PRIMARY,
                modifier = w,
            )
        }
        if (state.canPassLayOff) {
            ActionButton(
                text = stringResource(R.string.phom_action_pass_layoff),
                onClick = { onIntent(PhomIntent.PassLayOff) },
                style = ActionStyle.SECONDARY,
                modifier = w,
            )
        }
        if (state.canDiscard) {
            ActionButton(
                text = stringResource(R.string.phom_action_discard),
                onClick = { onIntent(PhomIntent.Discard) },
                enabled = state.selected.size == 1,
                style = ActionStyle.PRIMARY,
                modifier = w,
            )
        }
    }
}

@Composable
internal fun LockLandscape() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val previous = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            activity?.requestedOrientation = previous
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
