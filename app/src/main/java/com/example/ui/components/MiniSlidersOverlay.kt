package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.HslaColor

@Composable
fun MiniSlidersOverlay(
    width: Float,
    opacity: Float,
    currentColor: HslaColor,
    onWidthChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .width(220.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Товщина: ${width.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                // Live Preview Circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width.dp.coerceIn(2.dp, 22.dp))
                            .clip(CircleShape)
                            .background(currentColor.copy(alpha = opacity).toColor())
                    )
                }
            }

            Slider(
                value = width,
                onValueChange = onWidthChange,
                valueRange = 1f..22f,
                modifier = Modifier.height(30.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Прозорість: ${(opacity * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Slider(
                value = opacity,
                onValueChange = onOpacityChange,
                valueRange = 0.05f..1f,
                modifier = Modifier.height(30.dp)
            )
        }
    }
}
