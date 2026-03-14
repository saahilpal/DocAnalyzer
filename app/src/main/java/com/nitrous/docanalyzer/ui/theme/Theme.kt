package com.nitrous.docanalyzer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// GPT-Style Monochrome Palette
val BackgroundBlack = Color(0xFF000000)
val ElevatedSurface = Color(0xFF111111)
val BubbleBackground = Color(0xFF1A1A1A)
val InputDockColor = Color(0xFF141414)
val StrokeColor = Color(0xFF2A2A2A)
val PrimaryText = Color(0xFFEDEDED)
val SecondaryText = Color(0xFFA0A0A0)
val HintText = Color(0xFF6E6E6E)
val DestructiveRed = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF10B981)

private val MonochromeColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = BackgroundBlack,
    primaryContainer = ElevatedSurface,
    onPrimaryContainer = PrimaryText,
    secondary = SecondaryText,
    onSecondary = BackgroundBlack,
    secondaryContainer = BubbleBackground,
    onSecondaryContainer = PrimaryText,
    tertiary = Color.LightGray,
    onTertiary = BackgroundBlack,
    background = BackgroundBlack,
    onBackground = PrimaryText,
    surface = BackgroundBlack,
    onSurface = PrimaryText,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = SecondaryText,
    outline = StrokeColor,
    outlineVariant = StrokeColor,
    error = DestructiveRed,
    onError = Color.White
)

@Composable
fun DocAnalyzerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MonochromeColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
