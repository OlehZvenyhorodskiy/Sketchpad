package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import com.example.ui.theme.ThemedPanel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.models.HslaColor

@Composable
fun MiniSlidersOverlay(
    width: Float,
    opacity: Float,
    currentColor: HslaColor,
    onWidthChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false
) {
    ThemedPanel(
        modifier = modifier,
        tonalElevation = 8.dp,
        shadowElevation = 6.dp
    ) {
        if (vertical) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(120.dp)
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Width Slider
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${width.toInt()}px",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                    Spacer(modifier = Modifier.height(8.dp))
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .width(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val trackLength = maxHeight
                        Slider(
                            value = width,
                            onValueChange = onWidthChange,
                            valueRange = 1f..22f,
                            modifier = Modifier
                                .requiredWidth(trackLength)
                                .requiredHeight(48.dp)
                                .graphicsLayer { rotationZ = -90f }
                        )
                    }
                }

                // Column 2: Opacity Slider
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${(opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .width(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val trackLength = maxHeight
                        Slider(
                            value = opacity,
                            onValueChange = onOpacityChange,
                            valueRange = 0.05f..1f,
                            modifier = Modifier
                                .requiredWidth(trackLength)
                                .requiredHeight(48.dp)
                                .graphicsLayer { rotationZ = -90f }
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .widthIn(min = 200.dp, max = 400.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.stroke_width_value, width.toInt()),
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
                    modifier = Modifier.fillMaxWidth().height(30.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.opacity_percent, (opacity * 100).toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Slider(
                    value = opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.05f..1f,
                    modifier = Modifier.fillMaxWidth().height(30.dp)
                )
            }
        }
    }
}
