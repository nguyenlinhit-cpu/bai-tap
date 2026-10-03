package com.baiviet.game.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baiviet.core.data.DailyGiftState
import com.baiviet.core.data.EconomyConfig
import com.baiviet.core.data.SavedGameRepository
import com.baiviet.core.data.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LobbyUi(
    val balance: Long = EconomyConfig.WELCOME_COINS,
    val gift: DailyGiftState = DailyGiftState(false, 0, 0),
    val reliefLeft: Int = 0,
    /** Các game đang có ván dở. */
    val savedGames: Set<String> = emptySet(),
) {
    /** Số xu tối thiểu của bàn nhỏ nhất — dưới mức này được cứu trợ. */
    val reliefThreshold: Long get() = EconomyConfig.BET_LEVELS.first() * RELIEF_MULTIPLIER
    val canRelief: Boolean get() = balance < reliefThreshold && reliefLeft > 0

    companion object {
        const val RELIEF_MULTIPLIER = 30L
    }
}

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val wallet: WalletRepository,
    saved: SavedGameRepository,
) : ViewModel() {

    val ui: StateFlow<LobbyUi> = combine(wallet.balance, wallet.dailyGift, wallet.reliefLeft, saved.savedGameIds) { b, g, r, s ->
        LobbyUi(b, g, r, s)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LobbyUi())

    private val _toast = MutableStateFlow<Long?>(null)
    /** Số xu vừa nhận (hiện thông báo), null = không có. */
    val toast: StateFlow<Long?> = _toast.asStateFlow()

    private val _welcome = MutableStateFlow(false)
    val welcome: StateFlow<Boolean> = _welcome.asStateFlow()

    init {
        viewModelScope.launch { _welcome.value = wallet.ensureProfile() }
    }

    fun claimGift() {
        viewModelScope.launch { wallet.claimDailyGift().takeIf { it > 0 }?.let { _toast.value = it } }
    }

    fun claimRelief() {
        viewModelScope.launch {
            wallet.claimRelief(ui.value.reliefThreshold).takeIf { it > 0 }?.let { _toast.value = it }
        }
    }

    fun dismissToast() {
        _toast.value = null
        _welcome.value = false
    }
}
