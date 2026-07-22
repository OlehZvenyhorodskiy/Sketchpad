package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import java.util.Locale

fun formatDurationMs(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

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

@Composable
fun AudioPlayerPill(
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onPlayPauseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
        shadowElevation = 6.dp,
        tonalElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play / Pause Button
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Пауза" else "Відтворити",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Current Time
            Text(
                text = formatDurationMs(currentPositionMs),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Audio Scrubber Slider
            val maxRange = totalDurationMs.toFloat().coerceAtLeast(1f)
            val sliderValue = currentPositionMs.toFloat().coerceIn(0f, maxRange)

            Slider(
                value = sliderValue,
                onValueChange = onSeek,
                valueRange = 0f..maxRange,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
            )

            // Total Time
            Text(
                text = formatDurationMs(totalDurationMs),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Delete Recording Button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Видалити аудіо",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


