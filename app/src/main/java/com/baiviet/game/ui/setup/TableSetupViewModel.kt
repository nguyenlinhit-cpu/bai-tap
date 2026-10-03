package com.baiviet.game.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baiviet.core.data.EconomyConfig
import com.baiviet.core.data.SavedGameRepository
import com.baiviet.core.data.SavedTable
import com.baiviet.core.data.SettingsRepository
import com.baiviet.core.data.WalletRepository
import com.baiviet.game.bacay.rules.BaCayRules
import com.baiviet.game.lieng.rules.LiengRules
import com.baiviet.game.maubinh.rules.MauBinhRules
import com.baiviet.game.phom.rules.PhomRules
import com.baiviet.game.poker.rules.PokerRules
import com.baiviet.game.samloc.rules.SamLocRules
import com.baiviet.game.tienlen.rules.TienLenRules
import com.baiviet.game.xidach.rules.XiDachRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @param option vai trò cái (0 = Bạn, 1 = Máy, 2 = Luân phiên) hoặc số BB mua vào Poker
 */
data class SetupChoice(val players: Int = 4, val difficulty: Int = 1, val betIndex: Int = 0, val option: Int = 1)

data class SetupUi(
    val gameId: String = "tienlen",
    val balance: Long = 0L,
    val choice: SetupChoice = SetupChoice(),
    val minMultiplier: Long = 30,
    /** Ván đang dở của game này (vào bàn sẽ chơi tiếp). */
    val resume: SavedTable? = null,
    val guideSeen: Boolean = true,
) {
    fun minBalanceFor(bet: Long): Long = bet * minMultiplier
    fun isBetUnlocked(index: Int): Boolean = balance >= minBalanceFor(EconomyConfig.BET_LEVELS[index])
    val bet: Long get() = EconomyConfig.BET_LEVELS[choice.betIndex]
    val canEnter: Boolean get() = resume != null || isBetUnlocked(choice.betIndex)
}

@HiltViewModel
class TableSetupViewModel @Inject constructor(
    wallet: WalletRepository,
    private val saved: SavedGameRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val choice = MutableStateFlow(SetupChoice())
    private val gameId = MutableStateFlow("tienlen")
    private val resume = MutableStateFlow<SavedTable?>(null)

    val ui: StateFlow<SetupUi> = combine(wallet.balance, choice, gameId, resume, settings.settings) { b, c, id, r, st ->
        val mult = minMultiplier(id, c.option)
        // Tự hạ mức cược nếu không đủ xu cho mức đang chọn
        val unlocked = EconomyConfig.BET_LEVELS.indices.filter { b >= EconomyConfig.BET_LEVELS[it] * mult }
        val idx = if (c.betIndex in unlocked || unlocked.isEmpty()) c.betIndex else unlocked.last()
        SetupUi(id, b, c.copy(betIndex = idx), mult, r?.takeIf { it.gameId == id }, guideSeen = id in st.seenGuides)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetupUi())

    fun init(id: String) {
        if (gameId.value != id) {
            gameId.value = id
            choice.update { it.copy(option = defaultOption(id)) }
        } else if (choice.value.option == SetupChoice().option) {
            choice.update { it.copy(option = defaultOption(id)) }
        }
        refreshResume()
    }

    /** Đọc lại ván dở (gọi mỗi khi quay lại màn Chọn bàn). */
    fun refreshResume() {
        viewModelScope.launch { resume.value = saved.loadTable(gameId.value) }
    }

    /** Bỏ ván dở để mở bàn mới (khoản đang cược chưa bị trừ vì ván chưa kết thúc). */
    fun discardResume() {
        viewModelScope.launch {
            saved.clear(gameId.value)
            resume.value = null
        }
    }

    fun markGuideSeen() {
        viewModelScope.launch { settings.markGuideSeen(gameId.value) }
    }

    fun setPlayers(n: Int) = choice.update { it.copy(players = n) }
    fun setDifficulty(d: Int) = choice.update { it.copy(difficulty = d) }
    fun setBet(i: Int) = choice.update { it.copy(betIndex = i) }
    fun setOption(o: Int) = choice.update { it.copy(option = o) }

    private fun defaultOption(id: String) = if (id == PokerRules.GAME_ID) PokerRules.DEFAULT.startingChipsBB else 1

    companion object {
        /** Số xu tối thiểu vào bàn = hệ số × B, theo RuleConfig mặc định của từng game. */
        fun minMultiplier(gameId: String, option: Int): Long = when (gameId) {
            TienLenRules.GAME_ID -> TienLenRules.DEFAULT.minBalanceMultiplier.toLong()
            SamLocRules.GAME_ID -> SamLocRules.DEFAULT.minBalanceMultiplier.toLong()
            PhomRules.GAME_ID -> PhomRules.DEFAULT.minBalanceMultiplier.toLong()
            MauBinhRules.GAME_ID -> MauBinhRules.DEFAULT.minBalanceMultiplier
            XiDachRules.GAME_ID -> XiDachRules.DEFAULT.minBalanceMultiplier
            BaCayRules.GAME_ID -> BaCayRules.DEFAULT.minBalanceMultiplier
            LiengRules.GAME_ID -> LiengRules.DEFAULT.minBalanceMultiplier
            PokerRules.GAME_ID -> option.toLong().coerceAtLeast(1L)
            else -> 30L
        }
    }
}
