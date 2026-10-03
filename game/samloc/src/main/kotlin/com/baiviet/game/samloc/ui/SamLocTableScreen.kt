package com.baiviet.game.samloc.ui

import com.baiviet.core.ui.table.rememberTurnProgress
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import com.baiviet.core.ui.effects.CoinFlightLayer
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.CenterPile
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.HandFan
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.table.SeatView
import com.baiviet.core.ui.table.TableFelt
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoinsShort
import com.baiviet.game.samloc.R
import com.baiviet.game.samloc.engine.SlPhase
import kotlinx.coroutines.delay

/**
 * Màn bàn chơi Sâm Lốc (landscape).
 */
@Composable
fun SamLocTableScreen(
    players: Int,
    difficulty: Int,
    betUnit: Long,
    onExit: () -> Unit,
    onOpenRules: () -> Unit,
    viewModel: SamLocViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.start(players, difficulty, betUnit) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LockLandscape()
    BackHandler { viewModel.onIntent(SlIntent.RequestExit) }

    LaunchedEffect(Unit) {
        viewModel.effectFlow.collect { effect ->
            when (effect) {
                is SlEffect.Haptic -> {
                    haptic.performHapticFeedback(
                        if (effect.strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
                    )
                }

                SlEffect.Exit -> {
                    onExit()
                }
            }
        }
    }

    var showQuickRules by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }

    SamLocTable(
        state = state,
        onIntent = viewModel::onIntent,
        onQuickRules = { showQuickRules = true },
    ) {
        // Lớp Báo Sâm nếu đang đến lượt mình
        if (state.phase == SlPhase.BAO_SAM && state.turnSeat == 0) {
            val secondsLeft =
                ((state.turnMillis - (System.currentTimeMillis() - state.turnStartedAt)) / 1000)
                    .coerceIn(0, state.rules.samPhaseSeconds.toLong())
                    .toInt()
            SlBaoSamPromptOverlay(
                secondsLeft = secondsLeft,
                onCallSam = { viewModel.onIntent(SlIntent.CallSam) },
                onSkipSam = { viewModel.onIntent(SlIntent.SkipSam) },
            )
        }

        // Lớp Cảnh báo Báo 1
        if (state.showBao1Warning) {
            val nextName =
                state.seats
                    .getOrNull(1)
                    ?.info
                    ?.name ?: "Người kế tiếp"
            SlBao1WarningDialog(
                nextPlayerName = nextName,
                onConfirm = { viewModel.onIntent(SlIntent.ConfirmBao1Play) },
                onCancel = { viewModel.onIntent(SlIntent.CancelBao1Play) },
            )
        }

        // Lớp Kết quả ván
        state.result?.let { result ->
            SlResultPanel(
                result = result,
                onNewGame = { viewModel.onIntent(SlIntent.NewGame) },
                onChangeTable = onExit,
                onReview = { showLog = true },
            )
        }

        // Lớp Hướng dẫn nhanh
        if (state.showGuide) {
            SlQuickGuideDialog(onDismiss = { viewModel.onIntent(SlIntent.DismissGuide) })
        }

        // Lớp Xác nhận thoát
        if (state.showExitConfirm) {
            SlExitConfirmDialog(
                penalty = state.exitPenalty,
                onConfirm = { viewModel.onIntent(SlIntent.ConfirmExit) },
                onCancel = { viewModel.onIntent(SlIntent.CancelExit) },
            )
        }

        if (showLog) SlReviewDialog(state.log, onClose = { showLog = false })
        if (showQuickRules) {
            SlQuickRulesSheet(
                rules = state.rules,
                onClose = { showQuickRules = false },
            )
        }
    }
}

@Composable
fun SamLocTable(
    state: SlUiState,
    onIntent: (SlIntent) -> Unit,
    onQuickRules: () -> Unit,
    highlightPlay: Boolean = false,
    highlightPass: Boolean = false,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val cardWidth = if (state.cardStyle.large) 64.dp else 54.dp
    CompositionLocalProvider(LocalCardStyle provides state.cardStyle) {
        TableFelt(felt = state.felt) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                TopBar(state, onIntent, onQuickRules)

                // Ghế bot
                val anchors = SlSeatLayout.anchors(state.playerCount)
                state.seats.filter { it.seat != 0 }.forEach { seat ->
                    val a = anchors[seat.seat]
                    Box(Modifier.align(BiasAlignment(a.x * 2 - 1, a.y * 2 - 1))) {
                        BotSeat(seat, state, cardWidth)
                    }
                }

                // Tâm bàn
                CenterPile(
                    state = state.pile,
                    cardWidth = cardWidth * 0.92f,
                    modifier = Modifier.align(BiasAlignment(0f, 0f)),
                )
                state.message?.let {
                    Text(
                        text = it,
                        color = BvColors.Ivory,
                        style = MaterialTheme.typography.titleSmall.copy(shadow = Shadow(Color.Black, blurRadius = 6f)),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(BiasAlignment(0f, 0.3f)),
                    )
                }

                // Ghế của bạn
                state.seats.firstOrNull { it.seat == 0 }?.let { me ->
                    SeatView(
                        info = me.info,
                        timerProgress = timerFor(state, 0),
                        avatarSize = 46.dp,
                        showCardCount = false,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 4.dp),
                    )
                }

                // Bài trên tay (10 lá)
                HandFan(
                    cards = state.hand,
                    selected = state.selected,
                    cardWidth = cardWidth,
                    onToggle = { onIntent(SlIntent.Toggle(it)) },
                    onSweep = { onIntent(SlIntent.Sweep(it)) },
                    onSwipeUp = { onIntent(SlIntent.Play) },
                    enabled = !state.dealing && state.result == null && state.phase == SlPhase.PLAYING,
                    dimmed = state.dimCards,
                    highlighted = state.hintCards,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 96.dp, end = 220.dp, bottom = 2.dp),
                )

                // Nút hành động
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = stringResource(R.string.sl_hint),
                            onClick = { onIntent(SlIntent.Hint) },
                            enabled = state.isMyTurn && state.phase == SlPhase.PLAYING,
                            style = ActionStyle.SECONDARY,
                            modifier = Modifier.width(100.dp),
                        )
                        ActionButton(
                            text = stringResource(R.string.sl_sort),
                            onClick = { onIntent(SlIntent.Sort) },
                            enabled = state.hand.isNotEmpty() && !state.dealing,
                            style = ActionStyle.SECONDARY,
                            modifier = Modifier.width(100.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = stringResource(R.string.sl_pass),
                            onClick = { onIntent(SlIntent.Pass) },
                            enabled = state.isMyTurn && state.canPass,
                            style = ActionStyle.DANGER,
                            pulse = highlightPass,
                            modifier = Modifier.width(100.dp),
                        )
                        ActionButton(
                            text = stringResource(R.string.sl_play),
                            onClick = { onIntent(SlIntent.Play) },
                            enabled = state.isMyTurn && state.canPlay,
                            style = ActionStyle.PRIMARY,
                            pulse = highlightPlay || (state.isMyTurn && state.canPlay),
                            modifier = Modifier.width(100.dp),
                        )
                    }
                }

                CoinFlightLayer(state.flights)
                BigBanner(state.banner)
                overlay()
            }
        }
    }
}

@Composable
private fun BoxScope.TopBar(
    state: SlUiState,
    onIntent: (SlIntent) -> Unit,
    onQuickRules: () -> Unit,
) {
    Row(
        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton("✕", stringResource(R.string.sl_cd_exit), onClick = { onIntent(SlIntent.RequestExit) })
        Spacer(Modifier.width(8.dp))
        GlassPanel(
            shape =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(14.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(
                    stringResource(R.string.sl_title),
                    color = BvColors.Gold,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    stringResource(R.string.sl_bet_label, formatCoinsShort(state.betUnit)),
                    color = BvColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
    Row(
        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RoundIconButton(if (state.soundOn) "♪" else "∅", stringResource(R.string.sl_cd_sound), onClick = { onIntent(SlIntent.ToggleSound) })
        RoundIconButton("?", stringResource(R.string.sl_cd_rules), onClick = onQuickRules)
    }
}

@Composable
private fun BotSeat(
    seat: SlSeatUi,
    state: SlUiState,
    cardWidth: androidx.compose.ui.unit.Dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SeatView(info = seat.info, timerProgress = timerFor(state, seat.seat), avatarSize = 50.dp)
        if (seat.revealed.isNotEmpty()) {
            Box(Modifier.padding(top = 4.dp)) {
                seat.revealed.forEachIndexed { i, card ->
                    PlayingCard(
                        card = card,
                        width = cardWidth * 0.62f,
                        modifier =
                            Modifier.offset(
                                x =
                                    (cardWidth * 0.62f) * 0.38f * i - (cardWidth * 0.62f) * 0.19f * (seat.revealed.size - 1),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun timerFor(
    state: SlUiState,
    seat: Int,
): Float? {
    val active = state.turnSeat == seat && state.result == null
    val progress = rememberTurnProgress(
        startedAt = state.turnStartedAt,
        seconds = (state.turnMillis / 1000L).toInt().coerceAtLeast(1),
        active = active,
        tickSound = active && seat == 0,
    )
    return if (active) progress else null
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
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = previous
        }
    }
}
