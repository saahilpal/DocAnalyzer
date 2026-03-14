package com.nitrous.docanalyzer.ui.motion

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings

object AppMotion {
    // Section 7 - Global Motion System Constants
    const val NavTransitionDuration = 280
    val NavEasing = FastOutSlowInEasing
    
    val ScreenTransitionSpec = tween<Float>(durationMillis = NavTransitionDuration, easing = NavEasing)
    
    const val PressDuration = 200
    val MicroInteractionSpec = tween<Float>(durationMillis = PressDuration, easing = NavEasing)
    
    val ButtonPressSpec = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    val ReleaseSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    val FadeSpec = tween<Float>(durationMillis = 260)

    // Section 3 — LAUNCH ANIMATION POLISH (Refined)
    const val LaunchEntranceDuration = 420
    const val LaunchEntranceScaleInitial = 0.94f
    
    const val LaunchInternalMotionDuration = 500
    val LaunchInternalSpring = spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)
    
    const val LaunchSettleDuration = 300
    val LaunchSettleEasing = LinearOutSlowInEasing
    
    const val AppTransitionDuration = 260
    
    const val LineFadeDuration = 200
    const val LineStaggerDelay = 60

    // Section 6 - Universal Transitions
    val EnterTransition = fadeIn(animationSpec = ScreenTransitionSpec) + 
                          scaleIn(initialScale = 0.98f, animationSpec = ScreenTransitionSpec)
    
    val ExitTransition = fadeOut(animationSpec = ScreenTransitionSpec) + 
                         scaleOut(targetScale = 0.99f, animationSpec = ScreenTransitionSpec)
    
    // Section 8 - Microinteractions
    const val PressScaleTarget = 0.97f

    // 2 — STAR MICRO ANIMATION
    const val StarPulseDuration = 900
    const val StarScaleMin = 0.92f
    const val StarScaleMax = 1.0f
    const val StarAlphaMin = 0.7f
    const val StarAlphaMax = 1.0f
    
    // Long Press refinement
    const val LongPressMenuDelay = 120L
}

@Composable
@ReadOnlyComposable
fun useReducedMotion(): Boolean {
    val context = LocalContext.current
    return try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        ) == 0f
    } catch (e: Exception) {
        false
    }
}

@Composable
@ReadOnlyComposable
fun isAnimationEnabled(): Boolean = !useReducedMotion()
