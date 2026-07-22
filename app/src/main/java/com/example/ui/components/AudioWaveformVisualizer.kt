package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioWaveformVisualizer(
    isRecording: Boolean,
    recordingTimeText: String,
    amplitudes: List<Float> = emptyList(),
    strokeWidth: Float = 4f,
    modifier: Modifier = Modifier
) {
    if (!isRecording) return

    val waveColor = Color(0xFFEF4444)

    Row(
        modifier = modifier.height(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red recording indicator dot
        Box(
            modifier = Modifier
                .padding(end = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(waveColor)
        )

        // Real-time Audio Waveform Canvas
        Canvas(
            modifier = Modifier
                .width(84.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val centerY = canvasH / 2f

            val effStrokeWidth = (strokeWidth * 0.45f).coerceIn(2f, 7f)

            if (amplitudes.isEmpty()) {
                drawLine(
                    color = waveColor.copy(alpha = 0.4f),
                    start = Offset(0f, centerY),
                    end = Offset(canvasW, centerY),
                    strokeWidth = effStrokeWidth,
                    cap = StrokeCap.Round
                )
            } else {
                val count = amplitudes.size
                val stepX = if (count > 1) canvasW / (count - 1) else canvasW

                amplitudes.forEachIndexed { i, amp ->
                    val x = i * stepX
                    val amplitudeH = (amp * (canvasH - 4f)).coerceAtLeast(effStrokeWidth)
                    val yTop = centerY - amplitudeH / 2f
                    val yBottom = centerY + amplitudeH / 2f

                    drawLine(
                        color = waveColor,
                        start = Offset(x, yTop),
                        end = Offset(x, yBottom),
                        strokeWidth = effStrokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Dynamic Real-time Recording Timer Text
        Text(
            text = recordingTimeText,
            color = waveColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

