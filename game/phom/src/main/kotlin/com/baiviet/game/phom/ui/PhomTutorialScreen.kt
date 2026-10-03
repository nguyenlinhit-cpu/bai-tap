package com.baiviet.game.phom.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baiviet.core.cards.Card
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.SeatInfo
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.game.phom.R
import com.baiviet.game.phom.engine.PhomAction
import com.baiviet.game.phom.engine.PhomEngine
import com.baiviet.game.phom.engine.PhomPhase
import com.baiviet.game.phom.engine.PhomState
import com.baiviet.game.phom.engine.PhomTutorialScript
import com.baiviet.game.phom.engine.PhomTutorialStep
import com.baiviet.game.phom.rules.PhomEatRules
import com.baiviet.game.phom.rules.PhomRules
import com.baiviet.game.phom.rules.sortedPhom
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhomTutorialUi(
    val table: PhomUiState = PhomUiState(),
    val coach: String? = null,
    val stepIndex: Int = 1,
    val totalSteps: Int = 4,
    val done: Boolean = false,
)

class PhomTutorialViewModel : ViewModel() {
    private val engine = PhomEngine()
    private val _ui = MutableStateFlow(PhomTutorialUi())
    val ui = _ui.asStateFlow()

    private var game: PhomState? = null
    private var pending: CompletableDeferred<PhomAction>? = null
    private var expected: PhomTutorialStep.Human? = null
    private var job: Job? = null

    init {
        restart()
    }

    fun restart() {
        job?.cancel()
        job = viewModelScope.launch { runTutorial() }
    }

    private suspend fun runTutorial() {
        val hands = listOf(PhomTutorialScript.YOU.sortedPhom(), PhomTutorialScript.BOT.sortedPhom())
        val stock = PhomTutorialScript.STOCK
        val initial = engine.startWithHands(
            rules = PhomRules.DEFAULT,
            playerCount = 2,
            betUnit = 100L,
            hands = hands,
            stock = stock,
            leader = 1, // Bot đi đầu đánh 7 rô
        )
        game = initial

        _ui.update {
            it.copy(
                table = it.table.copy(
                    hand = hands[0],
                    seats = listOf(
                        PhomSeatUi(0, SeatInfo("Bạn", "B", 50_000L, 9, isHuman = true, isTurn = false)),
                        PhomSeatUi(1, SeatInfo("Máy hướng dẫn", "M", 50_000L, 10, isHuman = false, isTurn = true)),
                    ),
                    phase = PhomPhase.DISCARD,
                    isMyTurn = false,
                ),
                done = false,
            )
        }

        for (step in PhomTutorialScript.STEPS) {
            when (step) {
                is PhomTutorialStep.Human -> {
                    expected = step
                    _ui.update {
                        it.copy(
                            coach = step.prompt,
                            stepIndex = step.index,
                            table = it.table.copy(
                                isMyTurn = true,
                                canEat = it.table.phase == PhomPhase.DRAW_OR_EAT,
                                canDraw = it.table.phase == PhomPhase.DRAW_OR_EAT,
                                canDiscard = it.table.phase == PhomPhase.DISCARD,
                            ),
                        )
                    }
                    pending = CompletableDeferred()
                    val action = pending!!.await()
                    pending = null
                    expected = null
                    val trans = engine.apply(game!!, PlayerId(0), action)
                    game = trans.state
                    applyTransition(trans.state)
                }
                is PhomTutorialStep.Bot -> {
                    _ui.update { it.copy(coach = null) }
                    delay(700)
                    val trans = engine.apply(game!!, PlayerId(1), step.action)
                    game = trans.state
                    applyTransition(trans.state)
                }
            }
        }

        _ui.update { it.copy(coach = null, done = true) }
    }

    private fun applyTransition(state: PhomState) {
        _ui.update { current ->
            val humanSeat = current.table.seats[0].copy(
                info = current.table.seats[0].info.copy(
                    cardCount = state.hands[0].size,
                    isTurn = state.turn == 0,
                ),
            )
            val botSeat = current.table.seats[1].copy(
                info = current.table.seats[1].info.copy(
                    cardCount = state.hands[1].size,
                    isTurn = state.turn == 1,
                ),
            )

            current.copy(
                table = current.table.copy(
                    hand = state.hands[0],
                    seats = listOf(humanSeat, botSeat),
                    discardPiles = state.discardPiles,
                    stockCount = state.stock.size,
                    lastDiscard = state.lastDiscard,
                    lastDiscarder = state.lastDiscarder,
                    phase = state.phase,
                    selected = emptySet(),
                ),
            )
        }
    }

    fun onIntent(intent: PhomIntent) {
        val exp = expected ?: return
        when (intent) {
            is PhomIntent.Toggle -> {
                _stateToggle(intent.card)
            }
            is PhomIntent.Draw -> {
                val action = PhomAction.Draw
                if (exp.checkAction(action)) pending?.complete(action)
            }
            is PhomIntent.Eat -> {
                val cur = game ?: return
                val incoming = cur.lastDiscard ?: return
                val validMelds = PhomEatRules.findPossibleEatMelds(cur.hands[0], cur.eatenCards[0], incoming)
                val meld = validMelds.firstOrNull() ?: return
                val action = PhomAction.Eat(meld.cards)
                if (exp.checkAction(action)) pending?.complete(action)
            }
            is PhomIntent.Discard -> {
                val sel = _ui.value.table.selected.firstOrNull() ?: return
                val action = PhomAction.Discard(sel)
                if (exp.checkAction(action)) pending?.complete(action)
            }
            else -> Unit
        }
    }

    private fun _stateToggle(card: Card) {
        _ui.update {
            val sel = if (card in it.table.selected) it.table.selected - card else it.table.selected + card
            it.copy(table = it.table.copy(selected = sel))
        }
    }
}

@Composable
fun PhomTutorialScreen(onBack: () -> Unit, viewModel: PhomTutorialViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LockLandscape()
    BackHandler { onBack() }

    Box(Modifier.fillMaxSize()) {
        PhomTable(
            state = ui.table,
            onIntent = viewModel::onIntent,
            onQuickRules = {},
        )

        ui.coach?.let { prompt ->
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
                CoachOverlay(prompt, ui.stepIndex, ui.totalSteps)
            }
        }

        if (ui.done) {
            TutorialFinishedDialog(onBack, onReplay = { viewModel.restart() })
        }
    }
}

@Composable
private fun CoachOverlay(prompt: String, stepIndex: Int, totalSteps: Int) {
    val anim = rememberInfiniteTransition().animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
    )

    GlassPanel(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .border(2.dp, BvColors.Gold.copy(alpha = anim.value), RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "HƯỚNG DẪN • BƯỚC $stepIndex/$totalSteps",
                color = BvColors.Gold,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = prompt,
                color = BvColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TutorialFinishedDialog(onExit: () -> Unit, onReplay: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.phom_tut_finish_title), color = BvColors.Gold) },
        text = { Text(stringResource(R.string.phom_tut_finish_msg), color = BvColors.TextPrimary) },
        confirmButton = {
            TextButton(onClick = onExit) {
                Text(stringResource(R.string.phom_close), color = BvColors.Gold)
            }
        },
        dismissButton = {
            TextButton(onClick = onReplay) {
                Text(stringResource(R.string.phom_replay), color = BvColors.TextSecondary)
            }
        },
    )
}
