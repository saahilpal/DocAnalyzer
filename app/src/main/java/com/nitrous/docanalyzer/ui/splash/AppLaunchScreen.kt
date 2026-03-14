package com.nitrous.docanalyzer.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nitrous.docanalyzer.ui.components.DocAnalyzerLogo
import com.nitrous.docanalyzer.ui.motion.AppMotion
import com.nitrous.docanalyzer.ui.motion.isAnimationEnabled
import com.nitrous.docanalyzer.ui.theme.BackgroundBlack
import kotlinx.coroutines.delay

@Composable
fun AppLaunchScreen(
    onAnimationComplete: () -> Unit
) {
    if (!isAnimationEnabled()) {
        SideEffect { onAnimationComplete() }
        return
    }

    var phase by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    // Maintain the official launch timing logic (Step 6)
    LaunchedEffect(Unit) {
        phase = 1 // Entrance
        delay(AppMotion.LaunchEntranceDuration.toLong())
        phase = 2 // Internal motion (Staggered lines)
        delay(AppMotion.LaunchInternalMotionDuration.toLong())
        phase = 3 // Settle (Breathing only)
        delay(AppMotion.LaunchSettleDuration.toLong())
        phase = 4 // Exit
        delay(AppMotion.AppTransitionDuration.toLong())
        onAnimationComplete()
    }

    // Entrance/Exit Alpha (Step 3: Only the components themselves should fade)
    val logoAlpha by animateFloatAsState(
        targetValue = if (phase >= 1 && phase < 4) 1f else 0f,
        animationSpec = tween(AppMotion.LaunchEntranceDuration),
        label = "LogoAlpha"
    )

    // Staggered line entrance (Phase 2)
    val line1Alpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(AppMotion.LineFadeDuration),
        label = "Line1Alpha"
    )
    val line2Alpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(AppMotion.LineFadeDuration, delayMillis = AppMotion.LineStaggerDelay),
        label = "Line2Alpha"
    )
    val line3Alpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(AppMotion.LineFadeDuration, delayMillis = AppMotion.LineStaggerDelay * 2),
        label = "Line3Alpha"
    )
    
    // Subtle slide-up for lines only
    val lineTranslateY by animateDpAsState(
        targetValue = if (phase >= 2) 0.dp else 8.dp,
        animationSpec = tween(500, easing = AppMotion.NavEasing),
        label = "LineTranslateY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentAlignment = Alignment.Center
    ) {
        DocAnalyzerLogo(
            size = 130.dp,
            backgroundAlpha = logoAlpha,
            line1Alpha = line1Alpha,
            line2Alpha = line2Alpha,
            line3Alpha = line3Alpha,
            lineTranslateY = with(density) { lineTranslateY.toPx() }
        )
    }
}
