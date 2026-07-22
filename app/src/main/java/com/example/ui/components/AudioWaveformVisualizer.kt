package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioWaveformVisualizer(
    isRecording: Boolean,
    recordingTimeText: String,
    modifier: Modifier = Modifier
) {
    if (!isRecording) return

    val transition = rememberInfiniteTransition(label = "audio_bars")
    val phase1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "p1"
    )
    val phase2 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "p2"
    )
    val phase3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "p3"
    )

    Row(
        modifier = modifier.height(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.width(36.dp).fillMaxHeight()) {
            val barW = 4f
            val spacing = 4f
            val h = size.height

            val heights = listOf(phase1 * h, phase2 * h, phase3 * h, phase1 * 0.7f * h)
            heights.forEachIndexed { i, barH ->
                val x = i * (barW + spacing) + 4f
                val yTop = (h - barH) / 2f
                drawLine(
                    color = Color(0xFFEF4444),
                    start = Offset(x, yTop),
                    end = Offset(x, yTop + barH),
                    strokeWidth = barW
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = recordingTimeText,
            color = Color(0xFFEF4444),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
