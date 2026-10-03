package com.baiviet.game.tienlen.ui

import android.app.Application
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
import com.baiviet.core.ui.theme.formatSignedCoins
import com.baiviet.game.tienlen.R
import com.baiviet.game.tienlen.engine.TienLenEngine
import com.baiviet.game.tienlen.engine.TienLenState
import com.baiviet.game.tienlen.engine.TlAction
import com.baiviet.game.tienlen.engine.TutorialScript
import com.baiviet.game.tienlen.engine.TutorialStep
import com.baiviet.game.tienlen.rules.TienLenRules
import com.baiviet.game.tienlen.rules.sortedTl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ván tập Tiến lên: bài sắp sẵn, có hướng dẫn chỉ tay, chỉ cho phép thao tác đúng bước.
 * Nội dung: đánh rác, chặt heo bằng 3 đôi thông, chặn đôi, bỏ lượt và đi tự do.
 */
class TienLenTutorialViewModel(app: Application) : AndroidViewModel(app) {

    data class TutorialUi(
        val table: TlUiState = TlUiState(loading = false, playerCount = 2),
        val coach: String? = null,
        val expectPass: Boolean = false,
        val done: Boolean = false,
        val summary: List<String> = emptyList(),
    )

    private val engine = TienLenEngine()
    private val _ui = MutableStateFlow(TutorialUi())
    val ui: StateFlow<TutorialUi> = _ui.asStateFlow()

    private var game: TienLenState? = null
    private var pending: CompletableDeferred<TlAction>? = null
    private var expected: TutorialStep.Human? = null
    private var job: Job? = null
    private var nextId = 1

    private fun str(id: Int): String = getApplication<Application>().getString(id)

    init {
        restart()
    }

    fun restart() {
        job?.cancel()
        job = viewModelScope.launch { run() }
    }

    private suspend fun run() {
        var s = engine.startWithHands(TienLenRules(), 2, BET, listOf(TutorialScript.YOU, TutorialScript.BOT))
        game = s
        _ui.value = TutorialUi(table = TlUiState(loading = false, playerCount = 2, betUnit = BET, rules = s.rules))
        refresh(s)
        _ui.update { it.copy(table = it.table.copy(hand = s.hands[0].sortedTl())) }
        for (step in TutorialScript.STEPS) {
            val action = when (step) {
                is TutorialStep.Bot -> {
                    _ui.update { it.copy(coach = str(R.string.tl_tut_bot_wait), expectPass = false, table = it.table.copy(turnSeat = 1, turnStartedAt = System.currentTimeMillis(), isMyTurn = false, hintCards = emptySet())) }
                    delay(1100)
                    step.action
                }
                is TutorialStep.Human -> {
                    expected = step
                    val d = CompletableDeferred<TlAction>()
                    pending = d
                    _ui.update {
                        it.copy(
                            coach = str(STEP_TEXTS[step.index - 1]),
                            expectPass = step.cards == null,
                            table = it.table.copy(
                                turnSeat = 0,
                                turnStartedAt = System.currentTimeMillis(),
                                isMyTurn = true,
                                canPass = step.cards == null,
                                canPlay = false,
                                hintCards = step.cards.orEmpty(),
                                selected = emptySet(),
                                message = null,
                            ),
                        )
                    }
                    d.await().also { pending = null; expected = null }
                }
            }
            val seat = s.turn
            val t = engine.apply(s, PlayerId(seat), action)
            s = t.state
            game = s
            for (e in t.events) handle(e)
            refresh(s)
        }
        val settlement = engine.settle(s)
        val lines = settlement.lines.map { "${it.reason}: ${formatSignedCoins(if (it.to?.seat == 0) it.amount else -it.amount)}" }
        _ui.update { it.copy(done = true, coach = null, summary = lines, table = it.table.copy(turnSeat = null, isMyTurn = false)) }
    }

    private suspend fun handle(e: GameEvent) {
        when (e) {
            is GameEvent.Played -> {
                val seat = e.player.seat
                val set = PlayedSet(nextId++, e.cards, SeatLayout.direction(2, seat), if (seat == 0) -4f else 5f, Offset.Zero)
                _ui.update {
                    it.copy(
                        table = it.table.copy(
                            pile = it.table.pile.copy(sets = it.table.pile.sets + set),
                            hand = if (seat == 0) it.table.hand - e.cards.toSet() else it.table.hand,
                            selected = emptySet(),
                            hintCards = emptySet(),
                            isMyTurn = false,
                            canPlay = false,
                        ),
                    )
                }
                delay(400)
            }
            is GameEvent.Cut -> {
                _ui.update { it.copy(table = it.table.copy(banner = BannerData(nextId++, str(R.string.tl_banner_cut_two), "+600"))) }
                delay(1500)
                _ui.update { it.copy(table = it.table.copy(banner = null)) }
            }
            is GameEvent.RoundStarted -> {
                delay(400)
                _ui.update { it.copy(table = it.table.copy(pile = CenterPileState(nextId++, emptyList()))) }
            }
            is GameEvent.Finished -> {
                _ui.update { it.copy(table = it.table.copy(banner = BannerData(nextId++, str(R.string.tl_banner_win)))) }
                delay(1500)
                _ui.update { it.copy(table = it.table.copy(banner = null)) }
            }
            else -> delay(200)
        }
    }

    private fun refresh(s: TienLenState) {
        val names = listOf(str(R.string.tl_you), str(R.string.tl_tut_bot_name))
        val seats = (0..1).map { seat ->
            SeatUi(
                seat,
                SeatInfo(
                    name = names[seat],
                    initial = names[seat].first().toString(),
                    coins = 0L,
                    cardCount = s.hands[seat].size,
                    status = if (seat in s.passed) str(R.string.tl_status_pass) else null,
                    isTurn = !s.finished && s.turn == seat,
                    isHuman = seat == 0,
                    avatarColor = SEAT_COLORS[seat],
                ),
            )
        }
        _ui.update { it.copy(table = it.table.copy(seats = seats)) }
    }

    fun onIntent(intent: TlIntent) {
        val step = expected
        when (intent) {
            is TlIntent.Toggle -> select { if (intent.card in it) it - intent.card else it + intent.card }
            is TlIntent.Sweep -> select { sel -> val all = intent.cards.toSet(); if (all.all { it in sel }) sel - all else sel + all }
            TlIntent.Play -> {
                val sel = _ui.value.table.selected
                if (step?.cards != null && sel == step.cards) {
                    pending?.complete(TlAction.Play(sel.toList()))
                } else {
                    _ui.update { it.copy(table = it.table.copy(message = str(R.string.tl_tut_wrong))) }
                }
            }
            TlIntent.Pass -> {
                if (step != null && step.cards == null) {
                    pending?.complete(TlAction.Pass)
                } else {
                    _ui.update { it.copy(table = it.table.copy(message = str(R.string.tl_tut_wrong))) }
                }
            }
            TlIntent.Hint -> step?.cards?.let { cards -> select { cards } }
            TlIntent.Sort -> Unit
            else -> Unit
        }
    }

    private fun select(transform: (Set<Card>) -> Set<Card>) {
        _ui.update { ui ->
            val sel = transform(ui.table.selected).intersect(ui.table.hand.toSet())
            val ok = expected?.cards?.let { it == sel } ?: false
            ui.copy(table = ui.table.copy(selected = sel, canPlay = ok, message = null))
        }
    }

    companion object {
        private const val BET = 100L
        private val STEP_TEXTS = listOf(
            R.string.tl_tut_step1, R.string.tl_tut_step2, R.string.tl_tut_step3, R.string.tl_tut_step4,
            R.string.tl_tut_step5, R.string.tl_tut_step6, R.string.tl_tut_step7,
        )
    }
}

@Composable
fun TienLenTutorialScreen(onBack: () -> Unit, viewModel: TienLenTutorialViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LockLandscape()
    TienLenTable(
        state = ui.table,
        onIntent = { if (it == TlIntent.RequestExit) onBack() else viewModel.onIntent(it) },
        onQuickRules = {},
        highlightPlay = !ui.expectPass && ui.table.canPlay,
        highlightPass = ui.expectPass,
    ) {
        ui.coach?.let { CoachBubble(it, Modifier.align(BiasAlignment(0f, -0.55f))) }
        if (ui.done) {
            Scrim {
                GlassPanel(strong = true, modifier = Modifier.widthIn(max = 480.dp)) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.tl_tut_done_title), color = BvColors.Gold, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.tl_tut_done_body), color = BvColors.Ivory, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        ui.summary.forEach { Text(it, color = BvColors.TextSecondary, style = MaterialTheme.typography.bodySmall) }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionButton(stringResource(R.string.tl_tut_restart), viewModel::restart, style = ActionStyle.SECONDARY)
                            ActionButton(stringResource(R.string.tl_tut_finish), onBack)
                        }
                    }
                }
            }
        }
    }
}

/** Bong bóng chỉ dẫn (coach mark) có viền nhấp nháy. */
@Composable
private fun CoachBubble(text: String, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "coach")
    val a by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
    Box(modifier.widthIn(max = 520.dp).fillMaxWidth(0.62f)) {
        GlassPanel(
            strong = true,
            modifier = Modifier.border(2.dp, BvColors.Teal.copy(alpha = a), RoundedCornerShape(20.dp)),
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("👉", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.padding(4.dp))
                Text(text, color = BvColors.Ivory, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
