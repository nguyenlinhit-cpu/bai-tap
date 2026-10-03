package com.baiviet.game.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Game bài luôn dùng dark theme
private val BaiVietColorScheme = darkColorScheme(
    primary          = GoldAccent,
    onPrimary        = TextOnGold,
    primaryContainer = ChipGold,

    secondary          = TealAccent,
    onSecondary        = NavyDark,
    secondaryContainer = FeltGreen,

    tertiary           = RedAccent,

    background       = NavyDark,
    onBackground     = TextPrimary,
    surface          = PurpleDark,
    onSurface        = TextPrimary,
    surfaceVariant   = NavyMid,
    onSurfaceVariant = TextSecondary,

    error            = RedAccent,
    onError          = IvoryWhite,

    outline          = GlassBorder,
    outlineVariant   = GlassBg,
)

@Composable
fun BaiVietTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NavyDark.toArgb()
            window.navigationBarColor = NavyDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = BaiVietColorScheme,
        typography = BaiVietTypography,
        content = content,
    )
}
