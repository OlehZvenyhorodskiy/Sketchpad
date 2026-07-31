package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.academic.MathExpressionEvaluator
import com.example.data.models.CanvasWidgetEntity
import com.example.data.models.WidgetType
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CanvasWidgetRenderer(
    widget: CanvasWidgetEntity,
    onContentChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .offset { IntOffset(widget.x.roundToInt(), widget.y.roundToInt()) }
            .width(widget.width.dp)
            .height(widget.height.dp),
        shape = RoundedCornerShape(16.dp),
        color = when (widget.type) {
            WidgetType.STICKY_NOTE -> Color(0xFFFEF08A) // Yellow sticky
            WidgetType.TIMER -> MaterialTheme.colorScheme.surfaceContainerHigh
            WidgetType.CALCULATOR -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            // Close button in top-right corner
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.delete),
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            when (widget.type) {
                WidgetType.STICKY_NOTE -> {
                    var textState by remember { mutableStateOf(widget.content) }
                    Column {
                        Text("📌 ${stringResource(R.string.sticker)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF854D0E))
                        Spacer(Modifier.height(6.dp))
                        BasicTextField(
                            value = textState,
                            onValueChange = {
                                textState = it
                                onContentChange(it)
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                WidgetType.TIMER -> {
                    var secondsLeft by remember { mutableIntStateOf(widget.content.toIntOrNull() ?: 60) }
                    var isRunning by remember { mutableStateOf(false) }

                    LaunchedEffect(isRunning) {
                        while (isRunning && secondsLeft > 0) {
                            delay(1000L)
                            secondsLeft--
                        }
                        if (secondsLeft == 0) isRunning = false
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏱️ ${stringResource(R.string.timer)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Button(
                                onClick = { isRunning = !isRunning },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(stringResource(if (isRunning) R.string.pause else R.string.start))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    isRunning = false
                                    secondsLeft = 60
                                },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(stringResource(R.string.reset))
                            }
                        }
                    }
                }
                WidgetType.CALCULATOR -> {
                    var expression by remember { mutableStateOf(widget.content) }
                    var resultText by remember { mutableStateOf("") }

                    Column {
                        Text("🧮 ${stringResource(R.string.calculator)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        BasicTextField(
                            value = expression,
                            onValueChange = { input ->
                                expression = input
                                onContentChange(input)
                                resultText = try {
                                    val res = MathExpressionEvaluator.eval(input, 0.0)
                                    "= $res"
                                } catch (_: Exception) { "" }
                            },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = resultText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
