package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brush.BrushProfile
import com.example.brush.PressureCurve
import com.example.data.models.ToolType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrushEditorScreen(
    initialProfile: BrushProfile? = null,
    onSave: (BrushProfile) -> Unit,
    onBack: () -> Unit
) {
    var brushName by remember { mutableStateOf(initialProfile?.name ?: "Кастомний пензель") }
    var baseWidth by remember { mutableFloatStateOf(initialProfile?.baseWidth ?: 8f) }
    var pressureSensitivity by remember { mutableFloatStateOf(initialProfile?.pressureSensitivity ?: 0.8f) }
    var tiltSensitivity by remember { mutableFloatStateOf(initialProfile?.tiltSensitivity ?: 0.2f) }
    var spacing by remember { mutableFloatStateOf(initialProfile?.spacing ?: 0.15f) }
    var jitter by remember { mutableFloatStateOf(initialProfile?.jitter ?: 0f) }
    var scatter by remember { mutableFloatStateOf(initialProfile?.scatter ?: 0f) }
    var opacity by remember { mutableFloatStateOf(initialProfile?.opacity ?: 1f) }
    var flow by remember { mutableFloatStateOf(initialProfile?.flow ?: 1f) }
    var smoothing by remember { mutableFloatStateOf(initialProfile?.smoothing ?: 0.5f) }
    var selectedCurve by remember { mutableStateOf(initialProfile?.pressureCurve ?: PressureCurve.LINEAR) }
    var isDashed by remember { mutableStateOf(initialProfile?.isDashed ?: false) }

    val currentProfile = remember(brushName, baseWidth, pressureSensitivity, tiltSensitivity, spacing, jitter, scatter, opacity, flow, smoothing, selectedCurve, isDashed) {
        BrushProfile(
            id = initialProfile?.id ?: UUID.randomUUID().toString(),
            name = brushName,
            toolType = ToolType.PEN,
            baseWidth = baseWidth,
            pressureCurve = selectedCurve,
            pressureSensitivity = pressureSensitivity,
            tiltSensitivity = tiltSensitivity,
            spacing = spacing,
            jitter = jitter,
            scatter = scatter,
            opacity = opacity,
            flow = flow,
            smoothing = smoothing,
            isDashed = isDashed
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактор пензлів") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(currentProfile) }) {
                        Icon(Icons.Default.Check, contentDescription = "Зберегти")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview Canvas
            Text("Попередній перегляд:", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path()
                    val w = size.width
                    val h = size.height
                    path.moveTo(w * 0.1f, h * 0.5f)
                    path.cubicTo(w * 0.3f, h * 0.1f, w * 0.7f, h * 0.9f, w * 0.9f, h * 0.5f)
                    drawPath(
                        path = path,
                        color = Color(0xFF0F172A).copy(alpha = opacity),
                        style = Stroke(
                            width = baseWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            OutlinedTextField(
                value = brushName,
                onValueChange = { brushName = it },
                label = { Text("Назва пензля") },
                modifier = Modifier.fillMaxWidth()
            )

            // Sliders
            Text("Базова ширина: ${baseWidth.toInt()} px")
            Slider(value = baseWidth, onValueChange = { baseWidth = it }, valueRange = 1f..60f)

            Text("Чутливість до натиску: ${(pressureSensitivity * 100).toInt()}%")
            Slider(value = pressureSensitivity, onValueChange = { pressureSensitivity = it }, valueRange = 0f..1f)

            Text("Чутливість до нахилу: ${(tiltSensitivity * 100).toInt()}%")
            Slider(value = tiltSensitivity, onValueChange = { tiltSensitivity = it }, valueRange = 0f..1f)

            Text("Прозорість: ${(opacity * 100).toInt()}%")
            Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.05f..1f)

            Text("Потік фарби (Flow): ${(flow * 100).toInt()}%")
            Slider(value = flow, onValueChange = { flow = it }, valueRange = 0.05f..1f)

            Text("Згладжування: ${(smoothing * 100).toInt()}%")
            Slider(value = smoothing, onValueChange = { smoothing = it }, valueRange = 0f..1f)

            Text("Крива натиску:", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PressureCurve.entries.take(4).forEach { curve ->
                    FilterChip(
                        selected = curve == selectedCurve,
                        onClick = { selectedCurve = curve },
                        label = { Text(curve.name, fontSize = 10.sp) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Пунктирний штрих")
                Switch(checked = isDashed, onCheckedChange = { isDashed = it })
            }

            Button(
                onClick = { onSave(currentProfile) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Зберегти пензель")
            }
        }
    }
}
