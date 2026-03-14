package com.nitrous.docanalyzer.ui.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

@Composable
fun AnimatedSplashScreen(
    onAnimationComplete: () -> Unit,
    @Suppress("UNUSED_PARAMETER") skipSplash: Boolean = false,
    @Suppress("UNUSED_PARAMETER") splashDurationMs: Long = 1500L
) {
    SideEffect { onAnimationComplete() }
}
