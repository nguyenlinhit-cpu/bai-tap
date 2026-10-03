package com.baiviet.game.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation routes */
sealed interface Route {

    @Serializable data object Splash : Route
    @Serializable data object Lobby : Route

    @Serializable data class TableSetup(val gameId: String) : Route
    /** @param option tùy chọn riêng: vai trò cái (Xì dách, Ba cây) hoặc số BB mua vào (Poker) */
    @Serializable data class GamePlay(val gameId: String, val players: Int, val difficulty: Int, val betLevel: Long, val option: Int = 0) : Route
    @Serializable data class GameResult(val gameId: String) : Route

    @Serializable data class Rules(val gameId: String) : Route
    @Serializable data class Tutorial(val gameId: String) : Route
    @Serializable data class HouseRules(val gameId: String) : Route

    @Serializable data object Profile : Route
    @Serializable data object Settings : Route
}
