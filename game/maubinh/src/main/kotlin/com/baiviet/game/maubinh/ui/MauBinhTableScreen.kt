package com.baiviet.game.maubinh.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baiviet.core.cards.Card
import com.baiviet.core.ui.card.LocalCardStyle
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.table.SeatView
import com.baiviet.core.ui.table.TableFelt
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoinsShort
import com.baiviet.game.maubinh.R
import com.baiviet.game.maubinh.engine.MauBinhPhase
import com.baiviet.game.maubinh.engine.MauBinhRevealStep

@Composable
fun MauBinhTableScreen(
    players: Int = 4,
    difficulty: Int = 1,
    betUnit: Long = 100L,
    onExit: () -> Unit,
    onOpenRules: () -> Unit,
    viewModel: MauBinhViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.start(players, difficulty, betUnit) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LockLandscape()
    BackHandler { viewModel.onIntent(MauBinhIntent.ExitClicked) }

    LaunchedEffect(Unit) {
        viewModel.effectFlow.collect { effect ->
            when (effect) {
                is MauBinhEffect.Haptic -> haptic.performHapticFeedback(
                    if (effect.strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
                )
                is MauBinhEffect.NavigateBack -> onExit()
            }
        }
    }

    CompositionLocalProvider(LocalCardStyle provides state.cardStyle) {
        TableFelt(felt = state.felt) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                // Thanh tiêu đề phía trên
                MauBinhTopBar(
                    onExit = { viewModel.onIntent(MauBinhIntent.ExitClicked) },
                    onOpenRules = onOpenRules,
                    betUnit = state.betUnit,
                )

                // Khu vực bàn chơi
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    // Hiển thị ghế ngồi của các Bot đối thủ
                    BotSeatsLayer(state)

                    // Nội dung trung tâm bàn
                    when (state.phase) {
                        MauBinhPhase.ARRANGING -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 24.dp, vertical = 6.dp),
                            ) {
                                MauBinhArrangingBoard(
                                    state = state,
                                    onIntent = viewModel::onIntent,
                                )
                            }
                        }
                        MauBinhPhase.REVEALING, MauBinhPhase.FINISHED -> {
                            RevealingTableContent(
                                state = state,
                                onNext = { viewModel.onIntent(MauBinhIntent.NextRevealStep) },
                            )
                        }
                    }
                }
            }

            // Dialogs
            if (state.instantWinDialogOpen) {
                MauBinhInstantWinDialog(
                    state = state,
                    onIntent = viewModel::onIntent,
                )
            }

            state.result?.let { result ->
                MauBinhResultDialog(
                    result = result,
                    onIntent = viewModel::onIntent,
                    notes = state.resultNotes,
                    names = state.seats.associate { it.seat to it.info.name },
                )
            }

            if (state.pairwiseMatrixOpen) {
                MauBinhPairwiseMatrixDialog(
                    comparisons = state.pairwiseComparisons,
                    onDismiss = { viewModel.onIntent(MauBinhIntent.DismissPairwiseMatrix) },
                    names = state.seats.associate { it.seat to it.info.name },
                )
            }

            if (state.exitConfirmOpen) {
                MauBinhExitDialog(
                    onConfirm = { viewModel.onIntent(MauBinhIntent.ConfirmExit) },
                    onDismiss = { viewModel.onIntent(MauBinhIntent.DismissExit) },
                )
            }
        }
    }
}

@Composable
private fun MauBinhTopBar(
    onExit: () -> Unit,
    onOpenRules: () -> Unit,
    betUnit: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(glyph = "✕", contentDescription = "Rời bàn", onClick = onExit)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.mb_game_name),
                    color = Color(0xFFF5C451),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Text(
                    text = "1 chi = ${formatCoinsShort(betUnit)}B",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(glyph = "?", contentDescription = "Trợ giúp", onClick = onOpenRules)
        }
    }
}

@Composable
private fun BotSeatsLayer(state: MauBinhUiState) {
    val botSeats = state.seats.filter { it.seat != 0 }
    val anchors = MauBinhSeatLayout.anchors(state.playerCount)

    Box(modifier = Modifier.fillMaxSize()) {
        botSeats.forEach { seatUi ->
            val anchor = anchors.getOrElse(seatUi.seat) { Alignment.TopCenter }
            Box(
                modifier = Modifier
                    .align(anchor)
                    .padding(8.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SeatView(
                        info = seatUi.info,
                        timerProgress = null,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (state.phase == MauBinhPhase.ARRANGING) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (seatUi.isSubmitted) Color(0x3310B981) else Color(0x33F59E0B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (seatUi.isSubmitted) Color(0xFF10B981) else Color(0xFFF59E0B),
                            ),
                        ) {
                            Text(
                                text = if (seatUi.isSubmitted) "✓ Đã xếp xong" else "⏱ Đang xếp...",
                                color = if (seatUi.isSubmitted) Color(0xFF10B981) else Color(0xFFF59E0B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    } else {
                        // Hiển thị các chi đã lật của Bot
                        BotRevealedHandsView(seatUi, state.revealStep)
                    }
                }
            }
        }
    }
}

@Composable
private fun BotRevealedHandsView(
    seatUi: MauBinhSeatUi,
    step: MauBinhRevealStep,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x66000000))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (seatUi.instantWin != null) {
            Text(
                text = "Tới trắng: ${seatUi.instantWin.viName}",
                color = Color(0xFFF5C451),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        } else if (seatUi.isFoul) {
            Text(
                text = "⚠️ Binh lủng",
                color = Color(0xFFEF4444),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            // Chi 3
            if (seatUi.visibleChi3 != null && (step >= MauBinhRevealStep.CHI_3 || step == MauBinhRevealStep.SUMMARY)) {
                Text(
                    text = "Chi 3: ${seatUi.chi3Hand?.description ?: ""}",
                    color = Color.White,
                    fontSize = 9.sp,
                )
            }
            // Chi 2
            if (seatUi.visibleChi2 != null && (step >= MauBinhRevealStep.CHI_2 || step == MauBinhRevealStep.SUMMARY)) {
                Text(
                    text = "Chi 2: ${seatUi.chi2Hand?.description ?: ""}",
                    color = Color.White,
                    fontSize = 9.sp,
                )
            }
            // Chi 1
            if (seatUi.visibleChi1 != null && (step >= MauBinhRevealStep.CHI_1 || step == MauBinhRevealStep.SUMMARY)) {
                Text(
                    text = "Chi 1: ${seatUi.chi1Hand?.description ?: ""}",
                    color = Color.White,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
private fun RevealingTableContent(
    state: MauBinhUiState,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC0A150F))
                .border(1.dp, Color(0x60D4AF37), RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Tiêu đề bước so bài
            val stepTitle = when (state.revealStep) {
                MauBinhRevealStep.NOT_STARTED, MauBinhRevealStep.CHI_1 -> "SO CHI 1 (CHI ĐẦU - 5 LÁ)"
                MauBinhRevealStep.CHI_2 -> "SO CHI 2 (CHI GIỮA - 5 LÁ)"
                MauBinhRevealStep.CHI_3 -> "SO CHI 3 (CHI CUỐI - 3 LÁ)"
                MauBinhRevealStep.SUMMARY -> "TỔNG KẾT VÀ TÍNH ĐIỂM"
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x40D4AF37),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF5C451)),
            ) {
                Text(
                    text = stepTitle,
                    color = Color(0xFFF5C451),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            // Hiển thị các chi của người chơi
            PlayerRevealedHandBoard(state)

            Spacer(Modifier.height(10.dp))

            // Nút chuyển bước kế tiếp
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Tiếp tục >>", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PlayerRevealedHandBoard(state: MauBinhUiState) {
    val cardWidth = 38.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x33000000))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Chi 3
        RevealedChiRow(
            label = "Chi 3 (3 lá):",
            desc = state.chi3Hand?.description,
            cards = state.chi3Cards,
            cardWidth = cardWidth,
            isCurrentStep = state.revealStep == MauBinhRevealStep.CHI_3,
        )

        // Chi 2
        RevealedChiRow(
            label = "Chi 2 (5 lá):",
            desc = state.chi2Hand?.description,
            cards = state.chi2Cards,
            cardWidth = cardWidth,
            isCurrentStep = state.revealStep == MauBinhRevealStep.CHI_2,
        )

        // Chi 1
        RevealedChiRow(
            label = "Chi 1 (5 lá):",
            desc = state.chi1Hand?.description,
            cards = state.chi1Cards,
            cardWidth = cardWidth,
            isCurrentStep = state.revealStep == MauBinhRevealStep.CHI_1,
        )
    }
}

@Composable
private fun RevealedChiRow(
    label: String,
    desc: String?,
    cards: List<Card>,
    cardWidth: androidx.compose.ui.unit.Dp,
    isCurrentStep: Boolean,
) {
    val borderColor = if (isCurrentStep) Color(0xFFF5C451) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .background(if (isCurrentStep) Color(0x22F5C451) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = desc ?: "",
                color = Color(0xFFF5C451),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            cards.forEach { card ->
                PlayingCard(
                    card = card,
                    width = cardWidth,
                )
            }
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
