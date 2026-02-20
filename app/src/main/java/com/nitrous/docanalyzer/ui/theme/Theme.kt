package com.nitrous.docanalyzer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ChatGPT Monochrome Palette
private val PureBlack = Color(0xFF000000)
private val CardDark = Color(0xFF111111)
private val BorderDark = Color(0xFF222222)
private val InputDark = Color(0xFF1C1C1C)
private val PressedDark = Color(0xFF2A2A2A)
private val TextPrimaryDark = Color(0xFFFFFFFF)
private val TextSecondaryDark = Color(0xFF9AA0A6)
private val DeleteRed = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary = TextPrimaryDark,
    onPrimary = PureBlack,
    background = PureBlack,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = InputDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    error = DeleteRed,
    onError = TextPrimaryDark
)

@Composable
fun DocAnalyzerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Stick to Dark for ChatGPT style
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
