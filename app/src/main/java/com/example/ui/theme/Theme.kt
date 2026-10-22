package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkAviationMonochromeColorScheme = darkColorScheme(
    primary = BrushedPlatinum,
    onPrimary = JetBlack,
    primaryContainer = DarkTitanium,
    onPrimaryContainer = TextWhite,
    secondary = MetallicSilver,
    onSecondary = JetBlack,
    secondaryContainer = DeepCharcoalLight,
    onSecondaryContainer = TextWhite,
    tertiary = DarkTitanium,
    onTertiary = TextWhite,
    background = JetBlack,
    onBackground = TextWhite,
    surface = DeepCharcoal,
    onSurface = TextWhite,
    surfaceVariant = DeepCharcoalCard,
    onSurfaceVariant = TextLightSteel,
    outline = DeepCharcoalBorder,
    outlineVariant = JetBlackElevated,
    error = AviationRedBadge,
    onError = Color.White,
    errorContainer = AviationRedBadgeBg,
    onErrorContainer = AviationRedText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkAviationMonochromeColorScheme,
        typography = Typography,
        content = content
    )
}
