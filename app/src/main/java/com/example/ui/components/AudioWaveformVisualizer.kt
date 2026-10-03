package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.VoiceCyanAccent
import com.example.ui.theme.VoiceVioletLight
import kotlin.random.Random

@Composable
fun AudioWaveformVisualizer(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    maxHeight: Dp = 64.dp
) {
    val barAmplitudes = remember {
        List(barCount) { Animatable(0.2f) }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            barAmplitudes.forEachIndexed { index, animatable ->
                val delay = (index * 60) % 300
                val duration = 400 + (index % 5) * 80
                val targetHeight = 0.35f + Random.nextFloat() * 0.65f
                animatable.animateTo(
                    targetValue = targetHeight,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = duration, delayMillis = delay, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        } else {
            barAmplitudes.forEach { it.animateTo(0.15f, tween(300)) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight)
            .testTag("audio_waveform_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val barWidth = (totalWidth / (barCount * 2)).coerceIn(4f, 14f)
            val spacing = barWidth

            val startX = (totalWidth - (barCount * (barWidth + spacing) - spacing)) / 2

            val brush = Brush.verticalGradient(
                colors = listOf(
                    VoiceCyanAccent,
                    VoiceVioletLight,
                    VoiceVioletLight.copy(alpha = 0.6f)
                )
            )

            for (i in 0 until barCount) {
                val amplitude = barAmplitudes[i].value
                val barHeight = (totalHeight * amplitude).coerceAtLeast(6f)
                val x = startX + i * (barWidth + spacing)
                val y = (totalHeight - barHeight) / 2

                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
    }
}

@Composable
fun CircularRadarPulse(
    isSearching: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val pulse1 = remember { Animatable(0.2f) }
    val pulse2 = remember { Animatable(0.2f) }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            pulse1.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            pulse1.snapTo(0.2f)
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            pulse2.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, delayMillis = 600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            pulse2.snapTo(0.2f)
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val maxRadius = this.size.minDimension / 2

            if (isSearching) {
                // Wave 1
                val r1 = maxRadius * pulse1.value
                val alpha1 = (1f - pulse1.value).coerceIn(0f, 1f)
                drawCircle(
                    color = VoiceVioletLight.copy(alpha = alpha1 * 0.4f),
                    radius = r1,
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )

                // Wave 2
                val r2 = maxRadius * pulse2.value
                val alpha2 = (1f - pulse2.value).coerceIn(0f, 1f)
                drawCircle(
                    color = VoiceCyanAccent.copy(alpha = alpha2 * 0.4f),
                    radius = r2,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}
