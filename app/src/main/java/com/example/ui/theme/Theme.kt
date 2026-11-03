package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricBluePrimary,
    onPrimary = PureWhiteText,
    secondary = NeonSkyAccent,
    onSecondary = DeepDarkBackground,
    tertiary = VibrantEmeraldReady,
    background = DeepDarkBackground,
    onBackground = PureWhiteText,
    surface = SlateGraySurface,
    onSurface = PureWhiteText,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = MutedSlateSubtext,
    error = ErrorCrimson,
    onError = PureWhiteText
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = Typography,
        content = content
    )
}
