package com.nitrous.docanalyzer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AppLaunchScreen(
    onAnimationComplete: () -> Unit
) {
    var startExitTransition by remember { mutableStateOf(false) }
    
    val alpha by animateFloatAsState(
        targetValue = if (startExitTransition) 0f else 1f,
        animationSpec = tween(durationMillis = 300, easing = EaseInOutQuart),
        label = "ExitTransition"
    )

    LaunchedEffect(Unit) {
        delay(2200) // Allow animation to play
        startExitTransition = true
        delay(300) 
        onAnimationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        DocAnalyzerLaunchAnimation()
    }
}

@Composable
fun DocAnalyzerLaunchAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PremiumAnimation")

    // 1. Entry Fade
    var isStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isStarted = true }
    
    val entryAlpha by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0f,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "EntryAlpha"
    )

    // 2. Star Breathing (scale & opacity) - 900ms
    val starPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StarPulse"
    )
    
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StarAlpha"
    )

    // 3. Magnifying Glass horizontal scan movement - 1200ms
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanMove"
    )

    // 4. Document Lines alpha pulse 0.5 to 0.9
    val linesAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LinesScan"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .alpha(entryAlpha)
    ) {
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2

        // Scale factors to maintain proportions relative to 200px base
        val baseScale = this.size.width / 200f

        // --- Layer 1: Document (Positioned Left) ---
        val docWidth = 100f * baseScale
        val docHeight = 130f * baseScale
        val docLeft = centerX - docWidth * 0.65f
        val docTop = centerY - docHeight * 0.55f
        
        val cornerRadius = 10f * baseScale
        val foldSize = 22f * baseScale

        val docPath = Path().apply {
            moveTo(docLeft + cornerRadius, docTop)
            lineTo(docLeft + docWidth - foldSize, docTop)
            // Fold corner
            lineTo(docLeft + docWidth, docTop + foldSize)
            lineTo(docLeft + docWidth, docTop + docHeight - cornerRadius)
            arcTo(Rect(docLeft + docWidth - 2 * cornerRadius, docTop + docHeight - 2 * cornerRadius, docLeft + docWidth, docTop + docHeight), 0f, 90f, false)
            lineTo(docLeft + cornerRadius, docTop + docHeight)
            arcTo(Rect(docLeft, docTop + docHeight - 2 * cornerRadius, docLeft + 2 * cornerRadius, docTop + docHeight), 90f, 90f, false)
            lineTo(docLeft, docTop + cornerRadius)
            arcTo(Rect(docLeft, docTop, docLeft + 2 * cornerRadius, docTop + 2 * cornerRadius), 180f, 90f, false)
            close()
        }
        drawPath(docPath, Color.White)

        // Fold detail
        val foldPath = Path().apply {
            moveTo(docLeft + docWidth - foldSize, docTop)
            lineTo(docLeft + docWidth - foldSize, docTop + foldSize)
            lineTo(docLeft + docWidth, docTop + foldSize)
            close()
        }
        drawPath(foldPath, Color(0xFFE5E5E5))

        // --- Layer 2: Text Lines ---
        val lineStartX = docLeft + 15f * baseScale
        val lineTop = docTop + 35f * baseScale
        val lineWidth = 45f * baseScale
        val lineHeight = 5f * baseScale
        val lineSpacing = 12f * baseScale

        repeat(3) { i ->
            val widthFactor = if (i == 2) 0.7f else 1f
            drawRoundRect(
                color = Color.Black.copy(alpha = linesAlpha),
                topLeft = Offset(lineStartX, lineTop + i * (lineHeight + lineSpacing)),
                size = Size(lineWidth * widthFactor, lineHeight),
                cornerRadius = CornerRadius(lineHeight / 2)
            )
        }

        // --- Layer 3: Magnifying Glass (Positioned Right, Overlapping) ---
        val magRadius = 35f * baseScale
        // Scanning horizontally across the document's right edge
        val magCenterX = docLeft + docWidth * 0.95f + (scanOffset * baseScale)
        val magCenterY = docTop + docHeight * 0.85f
        
        // Handle - bottom-right direction (~45 deg)
        // 90 deg (down) - 45 deg = 45 deg (bottom-right)
        rotate(-45f, Offset(magCenterX, magCenterY)) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(magCenterX - 4f * baseScale, magCenterY + magRadius - 2f * baseScale),
                size = Size(8f * baseScale, 28f * baseScale),
                cornerRadius = CornerRadius(4f * baseScale)
            )
        }

        // Rim
        drawCircle(
            color = Color.White,
            radius = magRadius,
            center = Offset(magCenterX, magCenterY),
            style = Stroke(width = 6f * baseScale)
        )
        
        // Lens
        drawCircle(
            color = Color.Black,
            radius = magRadius - 3f * baseScale,
            center = Offset(magCenterX, magCenterY)
        )

        // --- Layer 4: Star (Centered inside lens) ---
        val starBaseSize = 14f * baseScale
        val currentStarSize = starBaseSize * starPulse
        
        val starPath = Path().apply {
            moveTo(magCenterX, magCenterY - currentStarSize)
            quadraticTo(magCenterX + currentStarSize * 0.2f, magCenterY - currentStarSize * 0.2f, magCenterX + currentStarSize, magCenterY)
            quadraticTo(magCenterX + currentStarSize * 0.2f, magCenterY + currentStarSize * 0.2f, magCenterX, magCenterY + currentStarSize)
            quadraticTo(magCenterX - currentStarSize * 0.2f, magCenterY + currentStarSize * 0.2f, magCenterX - currentStarSize, magCenterY)
            quadraticTo(magCenterX - currentStarSize * 0.2f, magCenterY - currentStarSize * 0.2f, magCenterX, magCenterY - currentStarSize)
            close()
        }

        // Star Glow
        drawPath(
            path = starPath,
            color = Color(0xFFBDDFFF).copy(alpha = starAlpha * 0.4f),
            style = Stroke(width = 4f * baseScale)
        )

        // Main Star
        drawPath(
            path = starPath,
            color = Color.White.copy(alpha = starAlpha)
        )
    }
}
