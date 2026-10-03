package com.baiviet.game.ui.lobby

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.CoinCounter
import com.baiviet.core.ui.table.CoinIcon
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.table.Scrim
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.core.ui.theme.formatCoins
import com.baiviet.game.R
import com.baiviet.game.catalog.GameCatalog
import com.baiviet.game.catalog.GameInfo

@Composable
fun LobbyScreen(
    onGameSelected: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    viewModel: LobbyViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val welcome by viewModel.welcome.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BvColors.NavyDark, BvColors.PurpleDark))),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LobbyTopBar(
                    ui = ui,
                    onGift = viewModel::claimGift,
                    onRelief = viewModel::claimRelief,
                    onSettingsClick = onSettingsClick,
                    onProfileClick = onProfileClick,
                )
            }
            items(GameCatalog.GAMES, key = { it.id }) { game ->
                GameCard(game = game, hasSavedGame = game.id in ui.savedGames, onClick = { onGameSelected(game.id) })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = BvColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }

        val amount = toast
        if (amount != null || welcome) {
            Scrim(onClick = viewModel::dismissToast) {
                GlassPanel(strong = true, modifier = Modifier.widthIn(max = 360.dp)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CoinIcon(48.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (amount != null) {
                                stringResource(R.string.received_coins, formatCoins(amount))
                            } else {
                                stringResource(R.string.welcome_bonus, formatCoins(ui.balance))
                            },
                            color = BvColors.Ivory,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        ActionButton(stringResource(R.string.confirm), viewModel::dismissToast)
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyTopBar(
    ui: LobbyUi,
    onGift: () -> Unit,
    onRelief: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        // 2 hàng: điện thoại dọc không đủ chỗ cho tên + xu + 3–4 nút trên cùng một hàng
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Hồ sơ", onClick = onProfileClick)
                        .background(Brush.linearGradient(listOf(BvColors.Gold, BvColors.GoldDeep)))
                        .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("B", color = BvColors.TextOnGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.you), style = MaterialTheme.typography.titleMedium, color = BvColors.Ivory, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoinIcon(16.dp)
                        Spacer(Modifier.width(4.dp))
                        CoinCounter(ui.balance, style = MaterialTheme.typography.titleMedium)
                        Text(" ${stringResource(R.string.coins)}", style = MaterialTheme.typography.labelMedium, color = BvColors.Gold.copy(alpha = 0.7f), maxLines = 1)
                    }
                }
                RoundIconButton("🏆", "Hồ sơ & thống kê", onClick = onProfileClick)
                Spacer(Modifier.width(8.dp))
                RoundIconButton("⚙", stringResource(R.string.settings), onClick = onSettingsClick)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ui.canRelief) {
                    ActionButton(stringResource(R.string.relief), onRelief, style = ActionStyle.DANGER, modifier = Modifier.padding(end = 8.dp))
                }
                Box {
                    ActionButton(
                        text = "🎁 " + stringResource(R.string.daily_gift),
                        onClick = onGift,
                        enabled = ui.gift.canClaim,
                        pulse = ui.gift.canClaim,
                    )
                    if (ui.gift.canClaim) {
                        Text(
                            stringResource(R.string.daily_day, ui.gift.dayIndex + 1),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-8).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BvColors.Red)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: GameInfo, hasSavedGame: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "cardScale",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .scale(cardScale)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(game.accent.copy(alpha = 0.38f), BvColors.GlassBg, BvColors.NavyDark.copy(alpha = 0.6f))))
            .border(1.dp, BvColors.GlassBorder, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(14.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(game.regionRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = game.accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(game.accent.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Spacer(Modifier.weight(1f))
                if (hasSavedGame) {
                    Text(
                        text = "Ván dở",
                        style = MaterialTheme.typography.labelSmall,
                        color = BvColors.TextOnGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BvColors.Gold)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                if (!game.available) {
                    Text(
                        text = stringResource(R.string.coming_soon),
                        style = MaterialTheme.typography.labelSmall,
                        color = BvColors.TextSecondary,
                    )
                }
            }
            // Minh họa: các lá bài xòe
            Box(Modifier.fillMaxWidth().height(78.dp), contentAlignment = Alignment.Center) {
                val n = game.showcase.size
                game.showcase.forEachIndexed { i, card ->
                    val angle = (i - (n - 1) / 2f) * 14f
                    PlayingCard(
                        card = card,
                        width = 44.dp,
                        modifier = Modifier
                            .offset(x = ((i - (n - 1) / 2f) * 20).dp)
                            .graphicsLayer {
                                rotationZ = angle
                                alpha = if (game.available) 1f else 0.7f
                            },
                    )
                }
            }
            Text(
                text = stringResource(game.nameRes),
                style = MaterialTheme.typography.titleMedium,
                color = BvColors.Ivory,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
