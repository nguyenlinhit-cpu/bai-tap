package com.baiviet.game.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.baiviet.core.data.Achievement
import com.baiviet.core.data.StatsRepository
import com.baiviet.core.data.WalletRepository
import com.baiviet.core.data.db.GameStatsEntity
import com.baiviet.core.ui.table.CoinCounter
import com.baiviet.core.ui.table.CoinIcon
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoins
import com.baiviet.core.ui.theme.formatSignedCoins
import com.baiviet.game.R
import com.baiviet.game.catalog.GameCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileUi(
    val balance: Long = 0L,
    val stats: Map<String, GameStatsEntity> = emptyMap(),
    val unlocked: Map<String, Long> = emptyMap(),
) {
    val played: Int get() = stats.values.sumOf { it.played }
    val wins: Int get() = stats.values.sumOf { it.wins }
    val winRate: Int get() = if (played == 0) 0 else wins * 100 / played
    val biggestWin: Long get() = stats.values.maxOfOrNull { it.biggestWin } ?: 0L

    /** Cấp độ: mỗi 10 ván lên 1 cấp. */
    val level: Int get() = 1 + played / 10
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    wallet: WalletRepository,
    stats: StatsRepository,
) : ViewModel() {
    val ui: StateFlow<ProfileUi> = combine(wallet.balance, stats.stats, stats.achievements) { b, s, a ->
        ProfileUi(b, s.associateBy { it.gameId }, a.associate { it.id to it.unlockedAt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUi())
}

/** Hồ sơ & thống kê: số ván, thắng, tỉ lệ thắng, ván thắng lớn nhất từng game, thành tích. */
@Composable
fun ProfileScreen(onBack: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BvColors.NavyDark, BvColors.PurpleDark))),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 260.dp),
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton("←", stringResource(R.string.back), onClick = onBack)
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(BvColors.Gold).border(2.dp, BvColors.Ivory, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("B", color = BvColors.TextOnGold, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Hồ sơ của bạn", color = BvColors.Gold, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("Cấp ${ui.level}", color = BvColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
                    }
                    CoinIcon(20.dp)
                    Spacer(Modifier.width(4.dp))
                    CoinCounter(ui.balance, style = MaterialTheme.typography.titleMedium)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatBox("Số ván", ui.played.toString(), Modifier.weight(1f))
                        StatBox("Thắng", ui.wins.toString(), Modifier.weight(1f))
                        StatBox("Tỉ lệ thắng", "${ui.winRate}%", Modifier.weight(1f))
                        StatBox("Thắng lớn nhất", formatCoins(ui.biggestWin), Modifier.weight(1f))
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Thống kê từng game") }
            items(GameCatalog.GAMES, key = { it.id }) { game ->
                val s = ui.stats[game.id]
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(game.accent))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(game.nameRes), color = BvColors.Ivory, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        if (s == null || s.played == 0) {
                            Text("Chưa chơi ván nào", color = BvColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                MiniStat("Ván", s.played.toString())
                                MiniStat("Thắng", "${s.wins} (${s.wins * 100 / s.played}%)")
                                MiniStat("Lớn nhất", formatCoins(s.biggestWin))
                            }
                            Text(
                                "Tổng: ${formatSignedCoins(s.totalDelta)} xu",
                                color = if (s.totalDelta >= 0) BvColors.Teal else BvColors.Red,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Thành tích (${ui.unlocked.size}/${Achievement.entries.size})")
            }
            items(Achievement.entries, key = { it.name }) { a ->
                val done = a.name in ui.unlocked
                GlassPanel(Modifier.fillMaxWidth().alpha(if (done) 1f else 0.5f)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (done) BvColors.Gold else BvColors.GlassBgStrong),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (done) "🏆" else "🔒", fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(a.title, color = if (done) BvColors.Gold else BvColors.Ivory, fontWeight = FontWeight.Bold)
                            Text(a.description, color = BvColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.disclaimer),
                    color = BvColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = BvColors.Teal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = BvColors.Gold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(label, color = BvColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(value, color = BvColors.Ivory, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(label, color = BvColors.TextMuted, style = MaterialTheme.typography.labelSmall)
    }
}
