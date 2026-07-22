package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.WidthNormal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.HslaColor

enum class PanelType { WIDTH, OPACITY }

@Composable
fun VerticalFloatingSidePanel(
    panelType: PanelType,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayText: String,
    currentColor: HslaColor,
    opacity: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .width(56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = when (panelType) {
                    PanelType.WIDTH -> Icons.Default.WidthNormal
                    PanelType.OPACITY -> Icons.Default.Opacity
                },
                contentDescription = when (panelType) {
                    PanelType.WIDTH -> "Товщина"
                    PanelType.OPACITY -> "Прозорість"
                },
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = displayText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (panelType == PanelType.WIDTH) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(value.dp.coerceIn(2.dp, 24.dp))
                            .clip(CircleShape)
                            .background(currentColor.copy(alpha = opacity).toColor())
                    )
                }
            }

            // Вертикальний слайдер через graphicsLayer rotation
            Box(
                modifier = Modifier
                    .height(180.dp)
                    .width(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    modifier = Modifier
                        .width(180.dp)
                        .graphicsLayer { rotationZ = -90f }
                )
            }
        }
    }
}
