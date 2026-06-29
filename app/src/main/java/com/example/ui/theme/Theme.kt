package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FrostedGlassColorScheme = darkColorScheme(
    primary = IndigoAccent,
    onPrimary = Color.White,
    primaryContainer = TranslucentIndigo,
    onPrimaryContainer = SlateTextPrimary,
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0x1514B8A6),
    onSecondaryContainer = SlateTextPrimary,
    background = DeepSlateBg,
    onBackground = SlateTextPrimary,
    surface = FrostedGlassBg,
    onSurface = SlateTextPrimary,
    surfaceVariant = DeepSlateCard,
    onSurfaceVariant = SlateTextSecondary,
    outline = FrostedGlassBorder,
    outlineVariant = Color(0x1FFFFFFF), // 12% white
    error = RedAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force high-tech dark mode by default for Frosted Glass styling
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve the custom Frosted Glass palette
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FrostedGlassColorScheme,
        typography = Typography,
        content = content
    )
}
