package com.baiviet.core.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Thống kê theo từng game. */
@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey val gameId: String,
    val played: Int = 0,
    val wins: Int = 0,
    val biggestWin: Long = 0L,
    val totalDelta: Long = 0L,
    val lastPlayedAt: Long = 0L,
)

/** Thành tích đã mở khóa. */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long,
)

/** Ván đang dở — trạng thái engine dạng JSON để chơi tiếp sau khi app bị tắt. */
@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey val gameId: String,
    val json: String,
    val savedAt: Long,
)

@Dao
interface StatsDao {
    @Query("SELECT * FROM game_stats")
    fun all(): Flow<List<GameStatsEntity>>

    @Query("SELECT * FROM game_stats WHERE gameId = :gameId")
    suspend fun get(gameId: String): GameStatsEntity?

    @Upsert
    suspend fun upsert(entity: GameStatsEntity)

    @Query("SELECT * FROM achievements")
    fun achievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(entity: AchievementEntity): Long
}

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_games WHERE gameId = :gameId")
    suspend fun get(gameId: String): SavedGameEntity?

    @Upsert
    suspend fun save(entity: SavedGameEntity)

    @Query("SELECT gameId FROM saved_games")
    fun savedIds(): Flow<List<String>>

    @Query("DELETE FROM saved_games WHERE gameId = :gameId")
    suspend fun delete(gameId: String)
}

@Database(
    entities = [GameStatsEntity::class, AchievementEntity::class, SavedGameEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class BaiVietDatabase : RoomDatabase() {
    abstract fun stats(): StatsDao
    abstract fun savedGames(): SavedGameDao
}
