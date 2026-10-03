package com.baiviet.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baiviet.core.data.Achievement
import com.baiviet.core.data.AppSettings
import com.baiviet.core.data.CardBackStyle
import com.baiviet.core.data.SettingsRepository
import com.baiviet.core.data.StatsRepository
import com.baiviet.core.data.WalletRepository
import com.baiviet.core.ui.audio.GameAudio
import com.baiviet.core.ui.audio.Sfx
import com.baiviet.core.ui.card.CardBackDesign
import com.baiviet.core.ui.card.CardStyle
import com.baiviet.core.ui.card.LocalCardStyle
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.game.navigation.BaiVietNavHost
import com.baiviet.game.ui.theme.BaiVietTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepo: SettingsRepository

    @Inject lateinit var stats: StatsRepository

    @Inject lateinit var wallet: WalletRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            LaunchedEffect(settings.sfx, settings.music) {
                GameAudio.sfxEnabled = settings.sfx
                GameAudio.musicEnabled = settings.music
            }
            // Âm thanh thắng/thua sau mỗi ván (mọi game ghi kết quả qua ví)
            LaunchedEffect(Unit) {
                wallet.results.collect { delta ->
                    when {
                        delta > 0 -> GameAudio.play(Sfx.WIN)
                        delta < 0 -> GameAudio.play(Sfx.LOSE)
                    }
                }
            }
            val systemHaptic = LocalHapticFeedback.current
            val haptic = remember(settings.vibration, systemHaptic) {
                if (settings.vibration) systemHaptic else NoHaptic
            }
            BaiVietTheme {
                CompositionLocalProvider(
                    LocalHapticFeedback provides haptic,
                    LocalCardStyle provides settings.toCardStyle(),
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize()) {
                            BaiVietNavHost()
                            AchievementToast(stats)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        GameAudio.onForeground()
    }

    override fun onStop() {
        GameAudio.onBackground()
        super.onStop()
    }
}

/** Tắt rung theo Cài đặt. */
private object NoHaptic : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

private fun AppSettings.toCardStyle() = CardStyle(
    fourColor = fourColorDeck,
    large = largeCards,
    back = when (cardBack) {
        CardBackStyle.CLASSIC_RED -> CardBackDesign.CLASSIC_RED
        CardBackStyle.ROYAL_BLUE -> CardBackDesign.ROYAL_BLUE
        CardBackStyle.JADE -> CardBackDesign.JADE
    },
)

/** Thông báo trượt xuống khi vừa mở khóa thành tích. */
@Composable
private fun AchievementToast(stats: StatsRepository) {
    var current by remember { mutableStateOf<Achievement?>(null) }
    LaunchedEffect(Unit) {
        stats.unlocked.collect { a ->
            current = a
            GameAudio.play(Sfx.BIG_EVENT)
            delay(3000)
            current = null
            delay(400)
        }
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(visible = current != null, enter = slideInVertically { -it }, exit = fadeOut()) {
            val a = current
            if (a != null) {
                GlassPanel(strong = true, modifier = Modifier.padding(top = 12.dp)) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆", fontSize = 26.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Thành tích mới: ${a.title}", color = BvColors.Gold, fontWeight = FontWeight.Bold)
                            Text(a.description, color = BvColors.Ivory, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
