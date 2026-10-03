package com.baiviet.core.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class Speed { SLOW, NORMAL, FAST }

enum class TableSkin { GREEN, RED, BLUE }

enum class CardBackStyle { CLASSIC_RED, ROYAL_BLUE, JADE }

/** Cài đặt người chơi. */
data class AppSettings(
    val music: Boolean = true,
    val sfx: Boolean = true,
    val vibration: Boolean = true,
    val botSpeed: Speed = Speed.NORMAL,
    val dealSpeed: Speed = Speed.NORMAL,
    val fourColorDeck: Boolean = false,
    val largeCards: Boolean = false,
    val tableSkin: TableSkin = TableSkin.GREEN,
    val cardBack: CardBackStyle = CardBackStyle.CLASSIC_RED,
    val autoSort: Boolean = true,
    /** Các game đã xem Hướng dẫn nhanh. */
    val seenGuides: Set<String> = emptySet(),
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.baiVietPrefs

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[MUSIC] = next.music
            prefs[SFX] = next.sfx
            prefs[VIBRATION] = next.vibration
            prefs[BOT_SPEED] = next.botSpeed.name
            prefs[DEAL_SPEED] = next.dealSpeed.name
            prefs[FOUR_COLOR] = next.fourColorDeck
            prefs[LARGE_CARDS] = next.largeCards
            prefs[TABLE_SKIN] = next.tableSkin.name
            prefs[CARD_BACK] = next.cardBack.name
            prefs[AUTO_SORT] = next.autoSort
            prefs[SEEN_GUIDES] = next.seenGuides
        }
    }

    suspend fun markGuideSeen(gameId: String) = update { it.copy(seenGuides = it.seenGuides + gameId) }

    private fun Preferences.toSettings() = AppSettings(
        music = this[MUSIC] ?: true,
        sfx = this[SFX] ?: true,
        vibration = this[VIBRATION] ?: true,
        botSpeed = enumOr(this[BOT_SPEED], Speed.NORMAL),
        dealSpeed = enumOr(this[DEAL_SPEED], Speed.NORMAL),
        fourColorDeck = this[FOUR_COLOR] ?: false,
        largeCards = this[LARGE_CARDS] ?: false,
        tableSkin = enumOr(this[TABLE_SKIN], TableSkin.GREEN),
        cardBack = enumOr(this[CARD_BACK], CardBackStyle.CLASSIC_RED),
        autoSort = this[AUTO_SORT] ?: true,
        seenGuides = this[SEEN_GUIDES] ?: emptySet(),
    )

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

    private companion object {
        val MUSIC = booleanPreferencesKey("music")
        val SFX = booleanPreferencesKey("sfx")
        val VIBRATION = booleanPreferencesKey("vibration")
        val BOT_SPEED = stringPreferencesKey("bot_speed")
        val DEAL_SPEED = stringPreferencesKey("deal_speed")
        val FOUR_COLOR = booleanPreferencesKey("four_color")
        val LARGE_CARDS = booleanPreferencesKey("large_cards")
        val TABLE_SKIN = stringPreferencesKey("table_skin")
        val CARD_BACK = stringPreferencesKey("card_back")
        val AUTO_SORT = booleanPreferencesKey("auto_sort")
        val SEEN_GUIDES = stringSetPreferencesKey("seen_guides")
    }
}
