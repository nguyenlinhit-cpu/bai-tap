package com.baiviet.game.tienlen.ui

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
import com.baiviet.game.tienlen.R
import com.baiviet.game.tienlen.rules.TlScoring
import kotlinx.coroutines.delay

/**
 * Màn bàn chơi Tiến lên (luôn nằm ngang).
 */
@Composable
fun TienLenTableScreen(
    players: Int,
    difficulty: Int,
    betUnit: Long,
    onExit: () -> Unit,
    onOpenRules: () -> Unit,
    viewModel: TienLenViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.start(players, difficulty, betUnit) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LockLandscape()
    BackHandler { viewModel.onIntent(TlIntent.RequestExit) }

    LaunchedEffect(Unit) {
        viewModel.effectFlow.collect { effect ->
            when (effect) {
                is TlEffect.Haptic -> haptic.performHapticFeedback(
                    if (effect.strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
                )
                TlEffect.Exit -> onExit()
            }
        }
    }

    var showQuickRules by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }

    TienLenTable(
        state = state,
        onIntent = viewModel::onIntent,
        onQuickRules = { showQuickRules = true },
    ) {
        state.result?.let { result ->
            ResultPanel(
                result = result,
                onNewGame = { viewModel.onIntent(TlIntent.NewGame) },
                onChangeTable = onExit,
                onReview = { showLog = true },
            )
        }
        if (state.showGuide) {
            QuickGuide(onDone = { viewModel.onIntent(TlIntent.DismissGuide) })
        }
        if (state.showExitConfirm) {
            ExitDialog(
                penalty = state.exitPenalty,
                onConfirm = { viewModel.onIntent(TlIntent.ConfirmExit) },
                onCancel = { viewModel.onIntent(TlIntent.CancelExit) },
            )
        }
        if (showLog) LogDialog(state.log, onClose = { showLog = false })
        if (showQuickRules) {
            QuickRulesSheet(
                rules = state.rules,
                betUnit = state.betUnit,
                onFullRules = {
                    showQuickRules = false
                    onOpenRules()
                },
                onDismiss = { showQuickRules = false },
            )
        }
    }
}

/** Khóa màn hình ngang + ẩn thanh hệ thống khi đang ở bàn chơi. */
@Composable
fun LockLandscape() {
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

/**
 * Bố cục bàn chơi — dùng chung cho ván thật và ván tập.
 *
 * @param overlay lớp phủ (bảng kết quả, hướng dẫn, coach marks…)
 */
@Composable
fun TienLenTable(
    state: TlUiState,
    onIntent: (TlIntent) -> Unit,
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
                val anchors = SeatLayout.anchors(state.playerCount)
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

                // Bài trên tay
                HandFan(
                    cards = state.hand,
                    selected = state.selected,
                    cardWidth = cardWidth,
                    onToggle = { onIntent(TlIntent.Toggle(it)) },
                    onSweep = { onIntent(TlIntent.Sweep(it)) },
                    onSwipeUp = { onIntent(TlIntent.Play) },
                    enabled = !state.dealing && state.result == null,
                    dimmed = state.dimCards,
                    highlighted = state.hintCards,
                    modifier = Modifier
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
                            text = stringResource(R.string.tl_hint),
                            onClick = { onIntent(TlIntent.Hint) },
                            enabled = state.isMyTurn,
                            style = ActionStyle.SECONDARY,
                            modifier = Modifier.width(100.dp),
                        )
                        ActionButton(
                            text = stringResource(R.string.tl_sort),
                            onClick = { onIntent(TlIntent.Sort) },
                            enabled = state.hand.isNotEmpty() && !state.dealing,
                            style = ActionStyle.SECONDARY,
                            modifier = Modifier.width(100.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = stringResource(R.string.tl_pass),
                            onClick = { onIntent(TlIntent.Pass) },
                            enabled = state.isMyTurn && state.canPass,
                            style = ActionStyle.DANGER,
                            pulse = highlightPass,
                            modifier = Modifier.width(100.dp),
                        )
                        ActionButton(
                            text = stringResource(R.string.tl_play),
                            onClick = { onIntent(TlIntent.Play) },
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
private fun BoxScope.TopBar(state: TlUiState, onIntent: (TlIntent) -> Unit, onQuickRules: () -> Unit) {
    Row(
        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton("✕", stringResource(R.string.tl_cd_exit), onClick = { onIntent(TlIntent.RequestExit) })
        Spacer(Modifier.width(8.dp))
        GlassPanel(shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(
                    stringResource(R.string.tl_title),
                    color = BvColors.Gold,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    stringResource(R.string.tl_bet_label, formatCoinsShort(state.betUnit)) + " · " +
                        stringResource(
                            if (state.rules.scoring == TlScoring.COUNT_CARDS) R.string.tl_mode_count else R.string.tl_mode_ranking,
                        ),
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
        RoundIconButton(if (state.soundOn) "♪" else "∅", stringResource(R.string.tl_cd_sound), onClick = { onIntent(TlIntent.ToggleSound) })
        RoundIconButton("?", stringResource(R.string.tl_cd_rules), onClick = onQuickRules)
    }
}

@Composable
private fun BotSeat(seat: SeatUi, state: TlUiState, cardWidth: androidx.compose.ui.unit.Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SeatView(info = seat.info, timerProgress = timerFor(state, seat.seat), avatarSize = 50.dp)
        if (seat.revealed.isNotEmpty()) {
            Box(Modifier.padding(top = 4.dp)) {
                seat.revealed.forEachIndexed { i, card ->
                    PlayingCard(
                        card = card,
                        width = cardWidth * 0.62f,
                        modifier = Modifier.offset(x = (cardWidth * 0.62f) * 0.38f * i - (cardWidth * 0.62f) * 0.19f * (seat.revealed.size - 1)),
                    )
                }
            }
        }
    }
}

/** Phần thời gian còn lại của lượt cho ghế [seat], cập nhật mỗi 100ms. */
@Composable
private fun timerFor(state: TlUiState, seat: Int): Float? {
    val active = state.turnSeat == seat && state.result == null
    val progress = rememberTurnProgress(
        startedAt = state.turnStartedAt,
        seconds = (state.turnMillis / 1000L).toInt().coerceAtLeast(1),
        active = active,
        tickSound = active && seat == 0,
    )
    return if (active) progress else null
}
