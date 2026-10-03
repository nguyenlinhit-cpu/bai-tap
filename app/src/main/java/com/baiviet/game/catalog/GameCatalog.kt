package com.baiviet.game.catalog

import androidx.compose.ui.graphics.Color
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.game.R

/**
 * Thông tin 8 game trong sảnh.
 *
 * @param available đã chơi được (các game khác sẽ mở dần theo lộ trình)
 * @param showcase vài lá bài minh họa trên thẻ game
 */
data class GameInfo(
    val id: String,
    val nameRes: Int,
    val regionRes: Int,
    val accent: Color,
    val available: Boolean,
    val showcase: List<Card>,
)

object GameCatalog {
    private fun c(r: Rank, s: Suit) = Card(r, s)

    val GAMES = listOf(
        GameInfo("tienlen", R.string.game_tienlen, R.string.region_south, Color(0xFF22C55E), true,
            listOf(c(Rank.TWO, Suit.SPADE), c(Rank.TWO, Suit.HEART), c(Rank.ACE, Suit.DIAMOND))),
        GameInfo("samloc", R.string.game_samloc, R.string.region_south, Color(0xFF3B82F6), true,
            listOf(c(Rank.TEN, Suit.CLUB), c(Rank.JACK, Suit.HEART), c(Rank.QUEEN, Suit.SPADE))),
        GameInfo("phom", R.string.game_phom, R.string.region_north, Color(0xFFF59E0B), true,
            listOf(c(Rank.SEVEN, Suit.HEART), c(Rank.SEVEN, Suit.SPADE), c(Rank.SEVEN, Suit.DIAMOND))),
        GameInfo("maubinh", R.string.game_maubinh, R.string.region_south, Color(0xFFA855F7), true,
            listOf(c(Rank.KING, Suit.SPADE), c(Rank.KING, Suit.HEART), c(Rank.KING, Suit.CLUB))),
        GameInfo("xidach", R.string.game_xidach, R.string.region_south, Color(0xFFEF4444), true,
            listOf(c(Rank.ACE, Suit.SPADE), c(Rank.KING, Suit.HEART))),
        GameInfo("poker", R.string.game_poker, R.string.region_international, Color(0xFFEC4899), true,
            listOf(c(Rank.ACE, Suit.HEART), c(Rank.ACE, Suit.CLUB))),
        GameInfo("lieng", R.string.game_lieng, R.string.region_south, Color(0xFF06B6D4), true,
            listOf(c(Rank.QUEEN, Suit.DIAMOND), c(Rank.KING, Suit.DIAMOND), c(Rank.ACE, Suit.DIAMOND))),
        GameInfo("bacay", R.string.game_bacay, R.string.region_south, Color(0xFFF97316), true,
            listOf(c(Rank.THREE, Suit.HEART), c(Rank.FOUR, Suit.CLUB), c(Rank.TWO, Suit.DIAMOND))),
    )

    fun byId(id: String): GameInfo = GAMES.first { it.id == id }
}
