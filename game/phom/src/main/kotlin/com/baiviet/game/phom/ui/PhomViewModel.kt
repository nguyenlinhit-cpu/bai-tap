package com.baiviet.game.phom.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baiviet.core.ai.BotLevel
import com.baiviet.core.ai.BotSpeed
import com.baiviet.core.ai.ThinkTime
import com.baiviet.core.cards.Card
import com.baiviet.core.data.CardBackStyle
import com.baiviet.core.data.HouseRulesRepository
import com.baiviet.core.data.SavedGameRepository
import com.baiviet.core.data.SettingsRepository
import com.baiviet.core.data.Speed
import com.baiviet.core.data.TableSession
import com.baiviet.core.data.TableSkin
import com.baiviet.core.data.WalletRepository
import com.baiviet.core.engine.IllegalActionException
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import com.baiviet.core.engine.SettlementLine
import com.baiviet.core.engine.TableConfig
import com.baiviet.core.ui.card.CardBackDesign
import com.baiviet.core.ui.card.CardStyle
import com.baiviet.core.ui.effects.BannerData
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.table.SeatInfo
import com.baiviet.game.phom.bot.PhomBot
import com.baiviet.game.phom.engine.PhomAction
import com.baiviet.game.phom.engine.PhomEngine
import com.baiviet.game.phom.engine.PhomPhase
import com.baiviet.game.phom.engine.PhomState
import com.baiviet.game.phom.rules.PhomPlayerResult
import com.baiviet.game.phom.rules.PhomRules
import com.baiviet.game.phom.rules.phomPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Game loop Phỏm: bạn + 1–3 bot, xu thật trong ví (chặn theo số dư), bot theo độ khó và tốc độ
 * trong Cài đặt. Hết giờ thì máy đánh hộ theo cách an toàn (bốc, hạ, đánh lá rác điểm cao không phá phỏm).
 * Ván dở được lưu sau mỗi nước để mở lại chơi tiếp.
 */
@HiltViewModel
class PhomViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallet: WalletRepository,
    private val settings: SettingsRepository,
    private val houseRules: HouseRulesRepository,
    private val saved: SavedGameRepository,
) : ViewModel() {

    private val engine = PhomEngine()
    private val random = Random(SecureRandom().nextLong())
    private val bot = PhomBot()

    private val _state = MutableStateFlow(PhomUiState())
    val state: StateFlow<PhomUiState> = _state.asStateFlow()

    private val effects = Channel<PhomEffect>(Channel.BUFFERED)
    val effectFlow = effects.receiveAsFlow()

    private var loop: Job? = null
    private var gameState: PhomState? = null
    private var session: TableSession? = null
    private var humanDeferred: CompletableDeferred<PhomAction>? = null
    private var newGame = CompletableDeferred<Unit>()
    private var previousWinner: PlayerId? = null
    private var level = BotLevel.NORMAL
    private var difficulty = 1
    private var speed = BotSpeed.NORMAL
    private var nextBannerId = 1

    fun start(players: Int, difficulty: Int, bet: Long) {
        if (loop != null) return
        loop = viewModelScope.launch {
            val appSettings = settings.settings.first()
            speed = when (appSettings.botSpeed) {
                Speed.SLOW -> BotSpeed.SLOW
                Speed.NORMAL -> BotSpeed.NORMAL
                Speed.FAST -> BotSpeed.FAST
            }
            _state.update { it.copy(felt = appSettings.tableSkin.toFelt(), cardStyle = appSettings.toCardStyle()) }

            val resume = saved.loadTable(PhomRules.GAME_ID)
            val p = resume?.players ?: players.coerceIn(2, 4)
            this@PhomViewModel.difficulty = resume?.difficulty ?: difficulty
            level = BotLevel.entries.getOrElse(this@PhomViewModel.difficulty) { BotLevel.NORMAL }
            val rules = houseRules.current(PhomRules.GAME_ID, PhomRules.serializer(), PhomRules.DEFAULT)
            val table = TableConfig(p, rules, resume?.bet ?: bet)
            val s = TableSession(context, wallet, PhomRules.GAME_ID, p, table.betUnit, rules.minBalanceMultiplier.toLong(), random)
            s.init(resume)
            session = s
            previousWinner = resume?.round?.takeIf { it in 0 until p }?.let { PlayerId(it) }
            _state.update { it.copy(playerCount = p, betUnit = table.betUnit, turnMillis = rules.turnSeconds * 1000L) }

            var restored = saved.decode(PhomState.serializer(), resume?.stateJson)?.takeIf { it.playerCount == p }
            while (true) {
                playGame(table, s, restored)
                restored = null
                newGame = CompletableDeferred()
                newGame.await()
            }
        }
    }

    private suspend fun persist(st: PhomState?) {
        val s = session ?: return
        val json = st?.let { saved.encode(PhomState.serializer(), it) }
        withContext(NonCancellable) {
            saved.saveTable(s.snapshot(difficulty, 0, previousWinner?.seat ?: -1, json))
        }
    }

    private suspend fun playGame(table: TableConfig<PhomRules>, s: TableSession, restored: PhomState?) {
        var st = restored ?: engine.startGame(table, SecureRandom().nextLong(), previousWinner)
        gameState = st
        _state.update { it.copy(result = null, selected = emptySet(), message = null, banner = null) }
        updateUiFromState(st)
        persist(st)
        while (!st.finished) {
            val actor = st.turn
            val legal = engine.legalActions(st, PlayerId(actor))
            if (legal.isEmpty()) break
            val action = if (actor == 0) awaitHuman(st, legal) else botAction(st, actor, legal)
            val transition = try {
                engine.apply(st, PlayerId(actor), action)
            } catch (e: IllegalActionException) {
                // Phòng hờ: hành động sai luật → để máy chọn nước an toàn
                engine.apply(st, PlayerId(actor), bot.decide(engine.viewOf(st, PlayerId(actor)), legal, BotLevel.NORMAL))
            }
            val before = st
            st = transition.state
            gameState = st
            updateUiFromState(st)
            persist(st)
            announce(before, st, actor, action)
            delay(200)
        }
        handleGameOver(st, s)
    }

    /** Banner cho sự kiện lớn: ăn cây, Ù, Ù khan. */
    private suspend fun announce(before: PhomState, after: PhomState, actor: Int, action: PhomAction) {
        val who = session?.name(actor) ?: "Máy $actor"
        val banner = when {
            after.isU && after.winner == actor -> BannerData(nextBannerId++, "Ù!", who, positive = actor == 0)
            after.isUKhan && after.winner == actor -> BannerData(nextBannerId++, "Ù khan!", who, positive = actor == 0)
            action is PhomAction.Eat && before.stock.size <= before.playerCount ->
                BannerData(nextBannerId++, "Ăn chốt!", who, positive = actor == 0)
            else -> null
        }
        if (banner != null) {
            _state.update { it.copy(banner = banner) }
            effects.send(PhomEffect.Haptic(strong = true))
            delay(1400)
            _state.update { if (it.banner?.id == banner.id) it.copy(banner = null) else it }
        }
    }

    private suspend fun awaitHuman(st: PhomState, legal: List<PhomAction>): PhomAction {
        val deferred = CompletableDeferred<PhomAction>()
        humanDeferred = deferred
        updateActionButtons(legal)
        _state.update { it.copy(isMyTurn = true, turnStartedAt = System.currentTimeMillis()) }
        effects.send(PhomEffect.Haptic(strong = false))
        val action = withTimeoutOrNull(st.rules.turnSeconds * 1000L) { deferred.await() }
            ?: bot.decide(engine.viewOf(st, PlayerId(0)), legal, BotLevel.NORMAL).also {
                _state.update { s -> s.copy(message = "Hết giờ — máy đánh hộ") }
            }
        humanDeferred = null
        _state.update { it.copy(isMyTurn = false, selected = emptySet()) }
        updateActionButtons(emptyList())
        return action
    }

    private suspend fun botAction(st: PhomState, actor: Int, legal: List<PhomAction>): PhomAction {
        _state.update { it.copy(isMyTurn = false, turnStartedAt = System.currentTimeMillis()) }
        val think = ThinkTime.pick(speed, random)
        val began = System.currentTimeMillis()
        val action = withContext(Dispatchers.Default) { bot.decide(engine.viewOf(st, PlayerId(actor)), legal, level) }
        val left = think - (System.currentTimeMillis() - began)
        if (left > 0) delay(left)
        return action
    }

    private fun updateActionButtons(legal: List<PhomAction>) {
        _state.update {
            it.copy(
                canDraw = legal.any { a -> a is PhomAction.Draw },
                canEat = legal.any { a -> a is PhomAction.Eat },
                canDiscard = legal.any { a -> a is PhomAction.Discard },
                canMeld = legal.any { a -> a is PhomAction.Meld },
                canLayOff = legal.any { a -> a is PhomAction.LayOff },
                canPassLayOff = legal.any { a -> a is PhomAction.PassLayOff },
                canU = legal.any { a -> a is PhomAction.DeclareU },
                canUKhan = legal.any { a -> a is PhomAction.DeclareUKhan },
            )
        }
    }

    private fun updateUiFromState(state: PhomState) {
        val s = session
        val seats = (0 until state.playerCount).map { seat ->
            val name = s?.name(seat) ?: if (seat == 0) "Bạn" else "Máy $seat"
            val info = SeatInfo(
                name = name,
                initial = name.take(1).uppercase(),
                coins = s?.balance(seat) ?: 0L,
                cardCount = state.inHand(seat).size,
                status = when {
                    state.winner == seat && state.isU -> "Ù"
                    state.winner == seat && state.isUKhan -> "Ù khan"
                    state.hasMelded(seat) && state.exposedMelds[seat].isEmpty() -> "Móm"
                    state.eatenCards[seat].isNotEmpty() -> "Ăn ${state.eatenCards[seat].size}"
                    else -> null
                },
                isTurn = !state.finished && state.turn == seat,
                isHuman = seat == 0,
                avatarColor = PHOM_SEAT_COLORS[seat % PHOM_SEAT_COLORS.size],
            )
            PhomSeatUi(seat, info)
        }
        _state.update {
            it.copy(
                hand = state.inHand(0),
                selected = it.selected.intersect(state.inHand(0).toSet()),
                eatenCards = state.eatenCards[0],
                seats = seats,
                discardPiles = state.discardPiles,
                exposedMelds = state.exposedMelds,
                stockCount = state.stock.size,
                lastDiscard = state.lastDiscard,
                lastDiscarder = state.lastDiscarder,
                phase = state.phase,
                turn = state.turn,
                playerCount = state.playerCount,
                betUnit = state.betUnit,
            )
        }
    }

    private suspend fun handleGameOver(state: PhomState, s: TableSession) {
        val (settlement, notes) = s.settle(engine.settle(state))
        val rankedSeats = if ((state.isU || state.isUKhan) && state.winner != null) {
            listOf(state.winner) + (0 until state.playerCount).filter { it != state.winner }
        } else {
            engine.rankEndOfGame(state)
        }
        previousWinner = PlayerId(rankedSeats.first())
        val canAgain = s.canContinue
        if (canAgain) persist(null) else withContext(NonCancellable) { saved.clear(PhomRules.GAME_ID) }

        val results = (0 until state.playerCount).map { seat ->
            val melds = state.exposedMelds[seat]
            PhomPlayerResult(
                seat = seat,
                isU = state.isU && state.winner == seat,
                isUKhan = state.isUKhan && state.winner == seat,
                isMom = melds.isEmpty() && state.winner != seat,
                points = state.inHand(seat).sumOf { it.phomPoint },
                melds = melds,
                remainingTrash = state.inHand(seat),
                meldOrder = state.playerMeldOrder[seat] ?: Int.MAX_VALUE,
            )
        }
        val myDelta = settlement.deltas[PlayerId(0)] ?: 0L
        val isWin = rankedSeats.first() == 0
        val title = when {
            isWin && state.isU -> "BẠN ĐÃ Ù!"
            isWin && state.isUKhan -> "BẠN Ù KHAN!"
            isWin -> "BẠN VỀ NHẤT!"
            else -> "KẾT THÚC VÁN"
        }
        updateUiFromState(state)
        _state.update {
            it.copy(
                isMyTurn = false,
                notes = notes + if (!canAgain) listOf("Bạn không đủ xu để chơi tiếp mức cược này.") else emptyList(),
                canPlayAgain = canAgain,
                result = PhomResultUi(
                    title = title,
                    isWin = isWin,
                    deltaCoins = myDelta,
                    names = (0 until state.playerCount).map { seat -> s.name(seat) },
                    deltas = (0 until state.playerCount).map { seat -> settlement.deltas[PlayerId(seat)] ?: 0L },
                    results = results,
                    rankedSeats = rankedSeats,
                ),
            )
        }
    }

    fun onIntent(intent: PhomIntent) {
        val cur = gameState
        when (intent) {
            is PhomIntent.Toggle -> _state.update {
                val sel = if (intent.card in it.selected) it.selected - intent.card else it.selected + intent.card
                it.copy(selected = sel, message = null)
            }
            is PhomIntent.Draw -> submit { it is PhomAction.Draw }
            is PhomIntent.Eat -> {
                val sel = _state.value.selected
                // Chọn cách ăn khớp với các lá đang chọn; không chọn thì lấy cách đầu tiên
                submit(message = "Chọn các lá tạo phỏm với lá vừa đánh") {
                    it is PhomAction.Eat && (sel.isEmpty() || it.meldCards.containsAll(sel))
                }
            }
            is PhomIntent.Meld -> submit { it is PhomAction.Meld }
            is PhomIntent.LayOff -> submit { it is PhomAction.LayOff }
            is PhomIntent.PassLayOff -> submit { it is PhomAction.PassLayOff }
            is PhomIntent.Discard -> {
                val sel = _state.value.selected
                if (sel.size != 1) {
                    _state.update { it.copy(message = "Chọn đúng 1 lá để đánh") }
                    return
                }
                submit(message = "Lá này không được đánh (đã ăn hoặc nằm trong phỏm)") {
                    it is PhomAction.Discard && it.card == sel.first()
                }
            }
            is PhomIntent.DeclareU -> submit { it is PhomAction.DeclareU }
            is PhomIntent.DeclareUKhan -> submit { it is PhomAction.DeclareUKhan }
            is PhomIntent.ExitClicked -> {
                if (cur == null || cur.finished || _state.value.result != null) leave(forfeit = false)
                else _state.update { it.copy(exitConfirmOpen = true) }
            }
            is PhomIntent.ConfirmExit -> {
                _state.update { it.copy(exitConfirmOpen = false) }
                leave(forfeit = true)
            }
            is PhomIntent.DismissExit -> _state.update { it.copy(exitConfirmOpen = false) }
            is PhomIntent.OpenReview -> _state.update { it.copy(reviewOpen = true) }
            is PhomIntent.DismissReview -> _state.update { it.copy(reviewOpen = false) }
            is PhomIntent.OpenQuickGuide -> _state.update { it.copy(quickGuideOpen = true) }
            is PhomIntent.DismissQuickGuide -> _state.update { it.copy(quickGuideOpen = false) }
            is PhomIntent.PlayAgain -> {
                if (!_state.value.canPlayAgain) {
                    leave(forfeit = false)
                    return
                }
                _state.update { it.copy(result = null, reviewOpen = false, notes = emptyList()) }
                newGame.complete(Unit)
            }
        }
    }

    private fun submit(message: String = "Hành động không hợp lệ", match: (PhomAction) -> Boolean) {
        val st = gameState ?: return
        val d = humanDeferred ?: return
        val action = engine.legalActions(st, PlayerId(0)).firstOrNull(match)
        if (action != null) {
            d.complete(action)
        } else {
            _state.update { it.copy(message = message) }
        }
    }

    private fun leave(forfeit: Boolean) {
        viewModelScope.launch {
            loop?.cancel()
            val st = gameState
            val s = session
            if (forfeit && st != null && s != null && !st.finished) s.forfeit(forfeitSettlement(st))
            saved.clear(PhomRules.GAME_ID)
            effects.send(PhomEffect.NavigateBack)
        }
    }

    /** Thoát giữa ván = xử Móm: trả tiền móm cho người đang có ít điểm rác nhất. */
    private fun forfeitSettlement(st: PhomState): Settlement {
        val me = PlayerId(0)
        val leader = (1 until st.playerCount).minBy { st.inHand(it).sumOf { c -> c.phomPoint } }
        val line = SettlementLine(me, PlayerId(leader), st.rules.momPayCards * st.betUnit, "Thoát giữa ván (xử móm)")
        return Settlement.fromLines((0 until st.playerCount).map { PlayerId(it) }, listOf(line))
    }

    private fun TableSkin.toFelt(): FeltColor = when (this) {
        TableSkin.GREEN -> FeltColor.GREEN
        TableSkin.RED -> FeltColor.RED
        TableSkin.BLUE -> FeltColor.BLUE
    }

    private fun com.baiviet.core.data.AppSettings.toCardStyle() = CardStyle(
        fourColor = fourColorDeck,
        large = largeCards,
        back = when (cardBack) {
            CardBackStyle.CLASSIC_RED -> CardBackDesign.CLASSIC_RED
            CardBackStyle.ROYAL_BLUE -> CardBackDesign.ROYAL_BLUE
            CardBackStyle.JADE -> CardBackDesign.JADE
        },
    )
}

/** Bài còn trên tay (engine giữ cả lá đã hạ trong [PhomState.hands]; lá đã hạ nằm ở [PhomState.exposedMelds]). */
private fun PhomState.inHand(seat: Int): List<Card> {
    val melds = exposedMelds[seat]
    return hands[seat].filter { card -> melds.none { card in it.cards } }
}
