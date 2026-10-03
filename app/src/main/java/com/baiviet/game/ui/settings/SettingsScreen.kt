package com.baiviet.game.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Suit
import com.baiviet.core.data.AppSettings
import com.baiviet.core.data.CardBackStyle
import com.baiviet.core.data.SettingsRepository
import com.baiviet.core.data.Speed
import com.baiviet.core.data.TableSkin
import com.baiviet.core.ui.card.CardBackDesign
import com.baiviet.core.ui.card.CardStyle
import com.baiviet.core.ui.card.LocalCardStyle
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.game.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.update(transform) }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BvColors.NavyDark, BvColors.PurpleDark)))
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton("←", stringResource(R.string.back), onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, color = BvColors.Gold, fontWeight = FontWeight.Bold)
        }

        Section(stringResource(R.string.settings_sound)) {
            Toggle(stringResource(R.string.settings_music), s.music) { v -> viewModel.update { it.copy(music = v) } }
            Toggle(stringResource(R.string.settings_sfx), s.sfx) { v -> viewModel.update { it.copy(sfx = v) } }
            Toggle(stringResource(R.string.settings_vibration), s.vibration) { v -> viewModel.update { it.copy(vibration = v) } }
        }

        Section(stringResource(R.string.settings_gameplay)) {
            Choice(stringResource(R.string.settings_bot_speed), Speed.entries, s.botSpeed, { speedLabel(it) }) { v -> viewModel.update { it.copy(botSpeed = v) } }
            Choice(stringResource(R.string.settings_deal_speed), Speed.entries, s.dealSpeed, { speedLabel(it) }) { v -> viewModel.update { it.copy(dealSpeed = v) } }
            Toggle(stringResource(R.string.settings_auto_sort), s.autoSort) { v -> viewModel.update { it.copy(autoSort = v) } }
        }

        Section(stringResource(R.string.settings_display)) {
            Toggle(stringResource(R.string.settings_four_color), s.fourColorDeck) { v -> viewModel.update { it.copy(fourColorDeck = v) } }
            Toggle(stringResource(R.string.settings_large_cards), s.largeCards) { v -> viewModel.update { it.copy(largeCards = v) } }
            Choice(stringResource(R.string.settings_table_skin), TableSkin.entries, s.tableSkin, { skinLabel(it) }) { v -> viewModel.update { it.copy(tableSkin = v) } }
            Choice(stringResource(R.string.settings_card_back), CardBackStyle.entries, s.cardBack, { backLabel(it) }) { v -> viewModel.update { it.copy(cardBack = v) } }
            // Xem trước
            val preview = CardStyle(
                fourColor = s.fourColorDeck,
                large = s.largeCards,
                back = CardBackDesign.entries[s.cardBack.ordinal],
            )
            CompositionLocalProvider(LocalCardStyle provides preview) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    val w = if (s.largeCards) 56.dp else 46.dp
                    listOf(Card(Rank.ACE, Suit.SPADE), Card(Rank.KING, Suit.HEART), Card(Rank.TEN, Suit.DIAMOND), Card(Rank.SEVEN, Suit.CLUB))
                        .forEach { PlayingCard(it, w) }
                    PlayingCard(null, w, faceUp = false)
                }
            }
        }
    }
}

@Composable
private fun speedLabel(s: Speed): String = stringResource(
    when (s) {
        Speed.SLOW -> R.string.speed_slow
        Speed.NORMAL -> R.string.speed_normal
        Speed.FAST -> R.string.speed_fast
    },
)

@Composable
private fun skinLabel(s: TableSkin): String = stringResource(
    when (s) {
        TableSkin.GREEN -> R.string.skin_green
        TableSkin.RED -> R.string.skin_red
        TableSkin.BLUE -> R.string.skin_blue
    },
)

@Composable
private fun backLabel(s: CardBackStyle): String = stringResource(
    when (s) {
        CardBackStyle.CLASSIC_RED -> R.string.back_red
        CardBackStyle.ROYAL_BLUE -> R.string.back_blue
        CardBackStyle.JADE -> R.string.back_jade
    },
)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = BvColors.Teal, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = BvColors.Ivory, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = BvColors.Gold, checkedTrackColor = BvColors.Gold.copy(alpha = 0.3f)),
        )
    }
}

@Composable
private fun <T> Choice(label: String, options: List<T>, value: T, name: @Composable (T) -> String, onChange: (T) -> Unit) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = BvColors.Ivory)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            options.forEach { opt ->
                val selected = opt == value
                Text(
                    name(opt),
                    color = if (selected) BvColors.TextOnGold else BvColors.Ivory,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) BvColors.Gold else BvColors.GlassBgStrong)
                        .clickable(role = Role.RadioButton) { onChange(opt) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}
