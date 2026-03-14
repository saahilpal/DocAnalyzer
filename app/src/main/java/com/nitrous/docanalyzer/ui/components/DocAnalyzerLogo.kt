package com.nitrous.docanalyzer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nitrous.docanalyzer.R

@Composable
fun DocAnalyzerLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    // Parameters kept for compatibility with AppLaunchScreen
    magnifierRotation: Float = 0f,
    magnifierScale: Float = 1f,
    line1Alpha: Float = 1f,
    line2Alpha: Float = 1f,
    line3Alpha: Float = 1f,
    lineTranslateY: Float = 0f,
    magnifierTranslationX: Float = 0f,
    magnifierTranslationY: Float = 0f,
    backgroundAlpha: Float = 1f
) {
    // Breathing animation for document lines area
    val infiniteTransition = rememberInfiniteTransition(label = "ScanningEffect")
    val scanAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                alpha = backgroundAlpha
            },
        contentAlignment = Alignment.Center
    ) {
        // Render the official PNG directly for perfect geometry
        Image(
            painter = painterResource(id = R.drawable.docanalyzer),
            contentDescription = "DocAnalyzer Logo",
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    
                    // Area for document lines (relative to Canvas size)
                    val canvasWidth = this.size.width
                    val canvasHeight = this.size.height
                    
                    val scanAreaWidth = canvasWidth * 0.35f
                    val scanAreaHeight = canvasHeight * 0.25f
                    val centerX = canvasWidth * 0.45f
                    val centerY = canvasHeight * 0.5f + lineTranslateY
                    
                    // Only draw if lines are supposed to be visible (Phase 2+)
                    if (line1Alpha > 0.1f) {
                        drawRect(
                            color = Color.White.copy(alpha = scanAlpha * line1Alpha * 0.2f),
                            topLeft = Offset(centerX - scanAreaWidth / 2f, centerY - scanAreaHeight / 2f),
                            size = Size(scanAreaWidth, scanAreaHeight),
                            blendMode = BlendMode.Screen
                        )
                    }
                }
        )
    }
}
