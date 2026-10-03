package com.baiviet.core.data

import com.baiviet.core.data.db.SavedGameDao
import com.baiviet.core.data.db.SavedGameEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ảnh chụp một bàn chơi đang dở: tham số bàn, bot (tên, xu), vị trí xoay vòng
 * và trạng thái engine của ván đang chơi (JSON, null nếu đang nghỉ giữa hai ván).
 *
 * @param round chỉ số xoay vòng (ghế đi đầu / ghế làm cái / nút dealer) của ván hiện tại
 * @param extra dữ liệu riêng của game (ví dụ stack Poker), JSON tùy game
 */
@Serializable
data class SavedTable(
    val gameId: String,
    val players: Int,
    val difficulty: Int,
    val bet: Long,
    val option: Int = 0,
    val botNames: List<String>,
    val botBalances: List<Long>,
    val round: Int = 0,
    val stateJson: String? = null,
    val extra: String? = null,
)

/**
 * Lưu ván đang dở (khi app vào nền/bị tắt) để mở lại chơi tiếp.
 * Mỗi game lưu một bản ghi JSON của trạng thái cần thiết.
 */
@Singleton
class SavedGameRepository @Inject constructor(private val dao: SavedGameDao) {
    private val json = Json { ignoreUnknownKeys = true; allowStructuredMapKeys = true }

    /** Các game đang có ván dở. */
    val savedGameIds: Flow<Set<String>> = dao.savedIds().map { it.toSet() }

    suspend fun <T> save(gameId: String, serializer: KSerializer<T>, value: T) {
        dao.save(SavedGameEntity(gameId, json.encodeToString(serializer, value), System.currentTimeMillis()))
    }

    suspend fun <T> load(gameId: String, serializer: KSerializer<T>): T? =
        dao.get(gameId)?.let { runCatching { json.decodeFromString(serializer, it.json) }.getOrNull() }

    suspend fun clear(gameId: String) = dao.delete(gameId)

    suspend fun saveTable(table: SavedTable) = save(table.gameId, SavedTable.serializer(), table)

    suspend fun loadTable(gameId: String): SavedTable? = load(gameId, SavedTable.serializer())

    /** Mã hóa trạng thái engine để đặt vào [SavedTable.stateJson]. */
    fun <T> encode(serializer: KSerializer<T>, value: T): String = json.encodeToString(serializer, value)

    /** Giải mã trạng thái engine; null nếu bản lưu hỏng hoặc khác phiên bản. */
    fun <T> decode(serializer: KSerializer<T>, text: String?): T? =
        text?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
}
