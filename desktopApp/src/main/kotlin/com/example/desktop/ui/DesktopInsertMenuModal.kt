package com.example.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.desktop.theme.LocalThemeSpec
import com.example.shared.model.ShapeType
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private data class InsertAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun DesktopInsertMenuModal(
    onInsertImage: (File) -> Unit,
    onInsertText: () -> Unit,
    onInsertChart: () -> Unit,
    onInsertShape: (ShapeType) -> Unit,
    onPasteClipboard: () -> Unit,
    onVectorize: () -> Unit,
    onPlotFunction: () -> Unit,
    onLatexOcr: () -> Unit,
    onOpenCodeLab: () -> Unit,
    onOpenStudyDeck: () -> Unit,
    onAiSummary: () -> Unit,
    onAiQuiz: () -> Unit,
    onFindLinks: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current

    val mainActions = listOf(
        InsertAction("Зображення", Icons.Default.Image, Color(0xFF38BDF8)) {
            val chooser = JFileChooser().apply {
                dialogTitle = "Виберіть зображення"
                fileFilter = FileNameExtensionFilter("Зображення (PNG, JPG, WebP)", "png", "jpg", "jpeg", "webp")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                onInsertImage(chooser.selectedFile)
                onDismissRequest()
            }
        },
        InsertAction("Текст", Icons.Default.Title, Color(0xFF818CF8)) {
            onInsertText()
            onDismissRequest()
        },
        InsertAction("Графік", Icons.Default.ShowChart, Color(0xFF34D399)) {
            onInsertChart()
            onDismissRequest()
        },
        InsertAction("Вставити", Icons.Default.ContentPaste, Color(0xFFFBBF24)) {
            onPasteClipboard()
            onDismissRequest()
        }
    )

    val shapeActions = listOf(
        ShapeType.CIRCLE to "Коло",
        ShapeType.SQUARE to "Квадрат",
        ShapeType.TRIANGLE to "Трикутник",
        ShapeType.ARROW to "Стрілка",
        ShapeType.STAR to "Зірка",
        ShapeType.BOLD_ARROW to "Жирна стрілка",
        ShapeType.HEXAGON to "Шестикутник",
        ShapeType.PENTAGON to "П'ятикутник",
        ShapeType.CLOUD to "Хмарка",
        ShapeType.SPEECH_BUBBLE to "Виноска"
    )

    val studyTools = listOf(
        InsertAction("Векторизація", Icons.Default.AutoFixHigh, Color(0xFFA855F7), onVectorize),
        InsertAction("Графік f(x)", Icons.Default.MultilineChart, Color(0xFF06B6D4), onPlotFunction),
        InsertAction("LaTeX OCR", Icons.Default.Functions, Color(0xFFEC4899), onLatexOcr),
        InsertAction("Code Lab", Icons.Default.Code, Color(0xFF10B981), onOpenCodeLab),
        InsertAction("AI Конспект", Icons.Default.Summarize, Color(0xFFF59E0B), onAiSummary),
        InsertAction("AI Тест", Icons.Default.Quiz, Color(0xFF6366F1), onAiQuiz),
        InsertAction("Зв'язки", Icons.Default.Link, Color(0xFF3B82F6), onFindLinks),
        InsertAction("Картки SM-2", Icons.Default.School, Color(0xFF14B8A6), onOpenStudyDeck)
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = themeSpec.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.width(460.dp).padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = themeSpec.accentColor)
                        Text("Додати на полотно", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = themeSpec.colorScheme.onSurface)
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant)
                    }
                }

                // 1. Primary Elements
                Text("Основні елементи", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = themeSpec.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mainActions.forEach { action ->
                        Surface(
                            onClick = action.onClick,
                            shape = RoundedCornerShape(12.dp),
                            color = action.color.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f).height(64.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(action.icon, contentDescription = action.label, tint = action.color, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(action.label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = themeSpec.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // 2. Geometric Shapes
                Text("Геометричні фігури", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = themeSpec.colorScheme.onSurfaceVariant)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(110.dp)
                ) {
                    items(shapeActions) { (type, name) ->
                        Surface(
                            onClick = {
                                onInsertShape(type)
                                onDismissRequest()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = themeSpec.colorScheme.surfaceContainer,
                            modifier = Modifier.height(50.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = when (type) {
                                        ShapeType.CIRCLE -> Icons.Default.Circle
                                        ShapeType.SQUARE -> Icons.Default.CropSquare
                                        ShapeType.TRIANGLE -> Icons.Default.ChangeHistory
                                        ShapeType.ARROW -> Icons.Default.ArrowForward
                                        ShapeType.STAR -> Icons.Default.Star
                                        else -> Icons.Default.Category
                                    },
                                    contentDescription = name,
                                    tint = themeSpec.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(name, fontSize = 9.sp, color = themeSpec.colorScheme.onSurface, maxLines = 1)
                            }
                        }
                    }
                }

                // 3. Academic & AI Tools
                Text("Академічні та AI інструменти", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = themeSpec.colorScheme.onSurfaceVariant)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    items(studyTools) { tool ->
                        Surface(
                            onClick = {
                                tool.onClick()
                                onDismissRequest()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = tool.color.copy(alpha = 0.12f),
                            modifier = Modifier.height(58.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(tool.icon, contentDescription = tool.label, tint = tool.color, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(tool.label, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = themeSpec.colorScheme.onSurface, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
