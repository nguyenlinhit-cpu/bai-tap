package com.baiviet.game.samloc.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baiviet.core.cards.Card
import com.baiviet.core.engine.GameEvent
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.ui.effects.BannerData
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.CenterPileState
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.PlayedSet
import com.baiviet.core.ui.table.Scrim
import com.baiviet.core.ui.table.SeatInfo
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.game.samloc.R
import com.baiviet.game.samloc.engine.SamLocEngine
import com.baiviet.game.samloc.engine.SamLocState
import com.baiviet.game.samloc.engine.SamLocTutorialScript
import com.baiviet.game.samloc.engine.SlAction
import com.baiviet.game.samloc.engine.SlPhase
import com.baiviet.game.samloc.engine.SlTutorialStep
import com.baiviet.game.samloc.rules.SamLocRules
import com.baiviet.game.samloc.rules.sortedSl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SamLocTutorialViewModel(
    app: Application,
) : AndroidViewModel(app) {
    data class SlTutorialUi(
        val table: SlUiState = SlUiState(loading = false, playerCount = 2),
        val coach: String? = null,
        val done: Boolean = false,
        val stepIndex: Int = 1,
        val totalSteps: Int = 4,
    )

    private val engine = SamLocEngine()
    private val _ui = MutableStateFlow(SlTutorialUi())
    val ui: StateFlow<SlTutorialUi> = _ui.asStateFlow()

    private var game: SamLocState? = null
    private var pending: CompletableDeferred<SlAction>? = null
    private var expected: SlTutorialStep.Human? = null
    private var job: Job? = null
    private var nextId = 1

    init {
        restart()
    }

    fun restart() {
        job?.cancel()
        job = viewModelScope.launch { runTutorial() }
    }

    private suspend fun runTutorial() {
        nextId = 1
        val hands = listOf(SamLocTutorialScript.YOU.sortedSl(), SamLocTutorialScript.BOT.sortedSl())
        val initial =
            engine.startWithHands(
                rules = SamLocRules.DEFAULT,
                playerCount = 2,
                betUnit = 100L,
                hands = hands,
                previousWinner = PlayerId(0),
            )
        game = initial

        _ui.update {
            it.copy(
                table =
                    it.table.copy(
                        hand = hands[0],
                        seats =
                            listOf(
                                SlSeatUi(
                                    0,
                                    SeatInfo(
                                        name = "Bạn",
                                        initial = "B",
                                        coins = 50_000L,
                                        cardCount = 10,
                                        isHuman = true,
                                        isTurn = true,
                                    ),
                                ),
                                SlSeatUi(
                                    1,
                                    SeatInfo(
                                        name = "Máy hướng dẫn",
                                        initial = "M",
                                        coins = 50_000L,
                                        cardCount = 10,
                                        isHuman = false,
                                        isTurn = false,
                                    ),
                                ),
                            ),
                        pile = CenterPileState(0, emptyList()),
                        phase = SlPhase.BAO_SAM,
                        isMyTurn = true,
                    ),
                done = false,
            )
        }

        for (step in SamLocTutorialScript.STEPS) {
            when (step) {
                is SlTutorialStep.Human -> {
                    expected = step
                    _ui.update {
                        it.copy(
                            coach = step.prompt,
                            stepIndex = step.index,
                            table =
                                it.table.copy(
                                    isMyTurn = true,
                                    canCallSam = it.table.phase == SlPhase.BAO_SAM,
                                    canSkipSam = it.table.phase == SlPhase.BAO_SAM,
                                    canPass = false,
                                    canPlay = it.table.phase == SlPhase.PLAYING,
                                ),
                        )
                    }
                    pending = CompletableDeferred()
                    val action = pending!!.await()
                    pending = null
                    expected = null
                    val trans = engine.apply(game!!, PlayerId(0), action)
                    game = trans.state
                    applyTransition(trans.state, trans.events, 0)
                }

                is SlTutorialStep.Bot -> {
                    _ui.update { it.copy(coach = null) }
                    delay(700)
                    val trans = engine.apply(game!!, PlayerId(1), step.action)
                    game = trans.state
                    applyTransition(trans.state, trans.events, 1)
                }
            }
        }

        _ui.update { it.copy(coach = null, done = true) }
    }

    private fun applyTransition(
        state: SamLocState,
        events: List<GameEvent>,
        actor: Int,
    ) {
        val lastEvent = events.filterIsInstance<GameEvent.Played>().lastOrNull()
        _ui.update { current ->
            var pile = current.table.pile
            if (lastEvent != null) {
                pile =
                    CenterPileState(
                        state.moveCount,
                        pile.sets +
                            PlayedSet(
                                id = nextId++,
                                cards = lastEvent.cards,
                                from = SlSeatLayout.direction(2, actor),
                                rotation = 0f,
                                jitter = Offset.Zero,
                            ),
                    )
            }
            if (events.any { it is GameEvent.RoundStarted }) {
                pile = CenterPileState(state.moveCount, emptyList())
            }

            val humanSeat =
                current.table.seats[0].copy(
                    info =
                        current.table.seats[0].info.copy(
                            cardCount = state.hands[0].size,
                            isTurn = state.turn == 0,
                            status = if (state.hands[0].size == 1) "Báo 1" else null,
                        ),
                )
            val botSeat =
                current.table.seats[1].copy(
                    info =
                        current.table.seats[1].info.copy(
                            cardCount = state.hands[1].size,
                            isTurn = state.turn == 1,
                        ),
                )

            current.copy(
                table =
                    current.table.copy(
                        hand = state.hands[0],
                        seats = listOf(humanSeat, botSeat),
                        pile = pile,
                        phase = state.phase,
                        samCaller = state.samCaller,
                        selected = emptySet(),
                    ),
            )
        }
    }

    fun onIntent(intent: SlIntent) {
        val exp = expected ?: return
        when (intent) {
            is SlIntent.Toggle -> {
                _ui.update {
                    val sel = if (intent.card in it.table.selected) it.table.selected - intent.card else it.table.selected + intent.card
                    it.copy(table = it.table.copy(selected = sel, canPlay = true))
                }
            }

            is SlIntent.CallSam -> {
                if (exp.expectedAction == SlAction.CallSam) {
                    pending?.complete(SlAction.CallSam)
                }
            }

            is SlIntent.Play -> {
                val selected = _ui.value.table.selected
                val expectedCards = (exp.expectedAction as? SlAction.Play)?.cards?.toSet().orEmpty()
                if (selected == expectedCards) {
                    pending?.complete(exp.expectedAction)
                }
            }

            else -> {
                Unit
            }
        }
    }
}

/**
 * Màn ván tập tương tác Sâm Lốc.
 */
@Composable
fun SamLocTutorialScreen(
    onBack: () -> Unit,
    viewModel: SamLocTutorialViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LockLandscape()
    BackHandler { onBack() }

    SamLocTable(
        state = ui.table,
        onIntent = viewModel::onIntent,
        onQuickRules = {},
    ) {
        ui.coach?.let { prompt ->
            CoachOverlay(prompt, ui.stepIndex, ui.totalSteps)
        }

        if (ui.done) {
            TutorialFinishedDialog(onBack, onReplay = { viewModel.restart() })
        }
    }
}

@Composable
private fun CoachOverlay(
    prompt: String,
    stepIndex: Int,
    totalSteps: Int,
) {
    val anim =
        rememberInfiniteTransition().animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        )

    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 54.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        GlassPanel(
            strong = true,
            modifier =
                Modifier
                    .widthIn(max = 540.dp)
                    .border(2.dp, BvColors.Teal.copy(alpha = anim.value.coerceIn(0.5f, 1f)), RoundedCornerShape(16.dp)),
        ) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.sl_tut_step_format, stepIndex, totalSteps, "Hướng dẫn"),
                    color = BvColors.Teal,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    prompt,
                    color = BvColors.Ivory,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TutorialFinishedDialog(
    onExit: () -> Unit,
    onReplay: () -> Unit,
) {
    Scrim {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(0.85f)) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.sl_tut_finished),
                    color = BvColors.Gold,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.sl_tut_finished_desc),
                    color = BvColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(stringResource(R.string.sl_tut_replay), onReplay, style = ActionStyle.SECONDARY)
                    ActionButton(stringResource(R.string.sl_tut_exit), onExit, pulse = true)
                }
            }
        }
    }
}
