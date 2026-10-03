package com.baiviet.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Trạng thái quà hằng ngày. */
data class DailyGiftState(
    val canClaim: Boolean,
    /** Ngày thứ mấy trong chuỗi 7 ngày (0..6) sẽ nhận nếu bấm nhận. */
    val dayIndex: Int,
    val amount: Long,
)

/**
 * Ví xu của người chơi.
 */
@Singleton
class WalletRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stats: StatsRepository,
) {
    private val store get() = context.baiVietPrefs

    val balance: Flow<Long> = store.data.map { it[KEY_BALANCE] ?: EconomyConfig.WELCOME_COINS }

    /** Tạo hồ sơ lần đầu: tặng xu chào mừng. Trả về true nếu vừa tạo. */
    suspend fun ensureProfile(): Boolean {
        var created = false
        store.edit { prefs ->
            if (prefs[KEY_BALANCE] == null) {
                prefs[KEY_BALANCE] = EconomyConfig.WELCOME_COINS
                created = true
            }
        }
        return created
    }

    suspend fun current(): Long = balance.first()

    /** Cộng/trừ xu sau ván; không để âm. Trả về số dư mới. */
    suspend fun applyDelta(delta: Long): Long {
        var result = 0L
        store.edit { prefs ->
            val now = prefs[KEY_BALANCE] ?: EconomyConfig.WELCOME_COINS
            result = (now + delta).coerceAtLeast(0L)
            prefs[KEY_BALANCE] = result
        }
        return result
    }

    /**
     * Ghi kết quả một ván: cộng/trừ ví và cập nhật thống kê, thành tích.
     * Trả về số dư mới.
     */
    suspend fun applyGameResult(gameId: String, delta: Long): Long {
        val balance = applyDelta(delta)
        stats.record(gameId, delta, balance)
        _results.tryEmit(delta)
        return balance
    }

    private val _results = MutableSharedFlow<Long>(extraBufferCapacity = 4)

    /** Tiền +/- của bạn sau mỗi ván (app phát âm thanh thắng/thua). */
    val results: SharedFlow<Long> = _results.asSharedFlow()

    val dailyGift: Flow<DailyGiftState> = store.data.map { prefs ->
        val today = LocalDate.now().toEpochDay()
        val last = prefs[KEY_DAILY_LAST] ?: Long.MIN_VALUE
        val streak = prefs[KEY_DAILY_STREAK] ?: 0
        val nextIndex = if (last == today - 1) streak % EconomyConfig.DAILY_GIFTS.size else 0
        DailyGiftState(
            canClaim = last != today,
            dayIndex = nextIndex,
            amount = EconomyConfig.DAILY_GIFTS[nextIndex],
        )
    }

    /** Nhận quà hằng ngày. Trả về số xu nhận được (0 nếu đã nhận hôm nay). */
    suspend fun claimDailyGift(): Long {
        var amount = 0L
        store.edit { prefs ->
            val today = LocalDate.now().toEpochDay()
            val last = prefs[KEY_DAILY_LAST] ?: Long.MIN_VALUE
            if (last == today) return@edit
            val streak = if (last == today - 1) (prefs[KEY_DAILY_STREAK] ?: 0) else 0
            val index = streak % EconomyConfig.DAILY_GIFTS.size
            amount = EconomyConfig.DAILY_GIFTS[index]
            prefs[KEY_DAILY_STREAK] = streak + 1
            prefs[KEY_DAILY_LAST] = today
            prefs[KEY_BALANCE] = (prefs[KEY_BALANCE] ?: EconomyConfig.WELCOME_COINS) + amount
        }
        return amount
    }

    /** Số lần cứu trợ còn lại hôm nay. */
    val reliefLeft: Flow<Int> = store.data.map { prefs ->
        val today = LocalDate.now().toEpochDay()
        if (prefs[KEY_RELIEF_DAY] == today) {
            EconomyConfig.RELIEF_PER_DAY - (prefs[KEY_RELIEF_COUNT] ?: 0)
        } else {
            EconomyConfig.RELIEF_PER_DAY
        }
    }

    /**
     * Nhận cứu trợ khi số xu < [threshold]. Tối đa [EconomyConfig.RELIEF_PER_DAY] lần/ngày.
     * Trả về số xu nhận được (0 nếu không đủ điều kiện).
     */
    suspend fun claimRelief(threshold: Long): Long {
        var amount = 0L
        store.edit { prefs ->
            val today = LocalDate.now().toEpochDay()
            val balance = prefs[KEY_BALANCE] ?: EconomyConfig.WELCOME_COINS
            if (balance >= threshold) return@edit
            val used = if (prefs[KEY_RELIEF_DAY] == today) prefs[KEY_RELIEF_COUNT] ?: 0 else 0
            if (used >= EconomyConfig.RELIEF_PER_DAY) return@edit
            amount = EconomyConfig.RELIEF_COINS
            prefs[KEY_RELIEF_DAY] = today
            prefs[KEY_RELIEF_COUNT] = used + 1
            prefs[KEY_BALANCE] = balance + amount
        }
        return amount
    }

    private companion object {
        val KEY_BALANCE = longPreferencesKey("wallet_balance")
        val KEY_DAILY_LAST = longPreferencesKey("daily_last_day")
        val KEY_DAILY_STREAK = intPreferencesKey("daily_streak")
        val KEY_RELIEF_DAY = longPreferencesKey("relief_day")
        val KEY_RELIEF_COUNT = intPreferencesKey("relief_count")
    }
}
