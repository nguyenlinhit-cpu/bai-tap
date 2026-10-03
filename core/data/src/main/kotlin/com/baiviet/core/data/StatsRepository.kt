package com.baiviet.core.data

import com.baiviet.core.data.db.AchievementEntity
import com.baiviet.core.data.db.GameStatsEntity
import com.baiviet.core.data.db.StatsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Thành tích đơn giản. */
enum class Achievement(val title: String, val description: String) {
    FIRST_WIN("Khai trương", "Thắng ván đầu tiên"),
    TEN_WINS("Cao thủ nhập môn", "Thắng 10 ván"),
    FIFTY_WINS("Cao thủ", "Thắng 50 ván"),
    HUNDRED_GAMES("Chăm chỉ", "Chơi 100 ván"),
    BIG_WIN("Trúng lớn", "Thắng từ 20.000 xu trong một ván"),
    ALL_GAMES("Đa tài", "Chơi đủ 8 thể loại"),
    RICH("Đại gia", "Có từ 1.000.000 xu"),
}

/** Thống kê từng game + thành tích. */
@Singleton
class StatsRepository @Inject constructor(private val dao: StatsDao) {

    val stats: Flow<List<GameStatsEntity>> = dao.all()
    val achievements: Flow<List<AchievementEntity>> = dao.achievements()

    private val _unlocked = MutableSharedFlow<Achievement>(extraBufferCapacity = 8)

    /** Thành tích vừa mở khóa — app hiện thông báo. */
    val unlocked: SharedFlow<Achievement> = _unlocked.asSharedFlow()

    /**
     * Ghi kết quả một ván. Trả về các thành tích vừa mở khóa.
     */
    suspend fun record(gameId: String, delta: Long, balanceAfter: Long): List<Achievement> {
        val now = System.currentTimeMillis()
        val old = dao.get(gameId) ?: GameStatsEntity(gameId)
        dao.upsert(
            old.copy(
                played = old.played + 1,
                wins = old.wins + if (delta > 0) 1 else 0,
                biggestWin = maxOf(old.biggestWin, delta),
                totalDelta = old.totalDelta + delta,
                lastPlayedAt = now,
            ),
        )
        val all = dao.all().first()
        val wins = all.sumOf { it.wins }
        val played = all.sumOf { it.played }
        val earned = buildList {
            if (wins >= 1) add(Achievement.FIRST_WIN)
            if (wins >= 10) add(Achievement.TEN_WINS)
            if (wins >= 50) add(Achievement.FIFTY_WINS)
            if (played >= 100) add(Achievement.HUNDRED_GAMES)
            if (all.any { it.biggestWin >= 20_000 }) add(Achievement.BIG_WIN)
            if (all.count { it.played > 0 } >= 8) add(Achievement.ALL_GAMES)
            if (balanceAfter >= 1_000_000) add(Achievement.RICH)
        }
        return earned.filter { dao.unlock(AchievementEntity(it.name, now)) != -1L }.onEach { _unlocked.tryEmit(it) }
    }
}
