package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.WidthNormal
import androidx.compose.material3.*
import com.example.ui.theme.ThemedPanel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.HslaColor
import com.example.R

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
    isEraser: Boolean = false,
    modifier: Modifier = Modifier
) {
    ThemedPanel(
        modifier = modifier.fillMaxHeight(0.65f),
        shadowElevation = 8.dp,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
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
                    PanelType.WIDTH -> stringResource(R.string.thickness)
                    PanelType.OPACITY -> stringResource(R.string.opacity_percent, (opacity * 100).toInt())
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
                val previewOutline = if (currentColor.lightness > 0.82f) androidx.compose.ui.graphics.Color(0xFF334155)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, previewOutline.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isEraser) {
                        Box(
                            modifier = Modifier
                                .size(value.dp.coerceIn(2.dp, 36.dp))
                                .border(2.dp, previewOutline, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(value.dp.coerceIn(3.dp, 36.dp))
                                .clip(CircleShape)
                                .background(currentColor.copy(alpha = opacity).toColor())
                                .border(1.dp, previewOutline, CircleShape)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val presets = if (isEraser) {
                        listOf(2f to 2, 6f to 6, 12f to 12)
                    } else {
                        listOf(2f to 2, 5f to 5, 12f to 12)
                    }
                    presets.forEach { (preset, label) ->
                        Surface(
                            onClick = { onValueChange(preset) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (kotlin.math.abs(value - preset) < 0.25f)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = label.toString(),
                                fontSize = 9.sp,
                                color = if (kotlin.math.abs(value - preset) < 0.25f)
                                    MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .width(48.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackLength = maxHeight
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    modifier = Modifier
                        .requiredWidth(trackLength)
                        .requiredHeight(48.dp)
                        .graphicsLayer { rotationZ = -90f }
                )
            }
        }
    }
}
