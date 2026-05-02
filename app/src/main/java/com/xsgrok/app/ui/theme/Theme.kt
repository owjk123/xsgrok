package com.xsgrok.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 文学色系主题配置
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = WarmGold,
    onSecondary = DeepBrown,
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = DeepBrown,
    tertiary = LightBrown,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEFEBE9),
    onTertiaryContainer = DeepBrown,
    background = PaperWhite,
    onBackground = DeepBrown,
    surface = CreamWhite,
    onSurface = DeepBrown,
    surfaceVariant = Color(0xFFF5F0E8),
    onSurfaceVariant = Color(0xFF5D4037),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = MutedGold,
    onSecondary = DarkBrown,
    secondaryContainer = Color(0xFF4A3F35),
    onSecondaryContainer = LightTextDark,
    tertiary = WarmGold,
    onTertiary = DarkBrown,
    tertiaryContainer = Color(0xFF3E3025),
    onTertiaryContainer = LightTextDark,
    background = DarkSurface,
    onBackground = LightTextDark,
    surface = DarkCard,
    onSurface = LightTextDark,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB8A89A),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFF4A2020),
    onErrorContainer = Color(0xFFFF6B6B)
)

@Composable
fun XSGrokTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
