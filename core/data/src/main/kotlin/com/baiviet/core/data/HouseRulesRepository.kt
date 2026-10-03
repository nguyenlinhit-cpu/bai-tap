package com.baiviet.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.baiviet.core.engine.RuleConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Luật nhà của từng game — lưu RuleConfig dạng JSON theo gameId.
 * JSON hỏng hoặc thiếu trường → dùng giá trị mặc định.
 */
@Singleton
class HouseRulesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.baiVietPrefs
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun <R : RuleConfig> rules(gameId: String, serializer: KSerializer<R>, default: R): Flow<R> =
        store.data.map { prefs ->
            prefs[key(gameId)]?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: default
        }

    suspend fun <R : RuleConfig> current(gameId: String, serializer: KSerializer<R>, default: R): R =
        rules(gameId, serializer, default).first()

    suspend fun <R : RuleConfig> save(gameId: String, serializer: KSerializer<R>, rules: R) {
        store.edit { it[key(gameId)] = json.encodeToString(serializer, rules) }
    }

    suspend fun reset(gameId: String) {
        store.edit { it.remove(key(gameId)) }
    }

    private fun key(gameId: String) = stringPreferencesKey("house_rules_$gameId")
}
