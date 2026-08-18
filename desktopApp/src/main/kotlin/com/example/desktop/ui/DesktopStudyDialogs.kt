package com.example.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.desktop.theme.LocalThemeSpec
import com.example.shared.academic.LocalCodeAnalyzer
import com.example.shared.academic.SpacedRepetitionScheduler
import com.example.shared.model.CodeBlockEntity
import com.example.shared.model.CodeLanguage
import com.example.shared.model.FlashcardEntity
import com.example.shared.model.TextBlockEntity
import java.util.UUID

@Composable
fun DesktopCodeLabDialog(
    initialSource: String = "print(\"Hello from Sketchpad!\")",
    initialLanguage: CodeLanguage = CodeLanguage.PYTHON,
    onInsertCodeBlock: (CodeBlockEntity) -> Unit,
    onDismissRequest: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    var source by remember { mutableStateOf(initialSource) }
    var language by remember { mutableStateOf(initialLanguage) }
    var outputText by remember { mutableStateOf("") }
    var diagnostics by remember { mutableStateOf<List<String>>(emptyList()) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = themeSpec.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.width(520.dp).padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header & Language selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = themeSpec.accentColor)
                        Text("Code Lab", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = themeSpec.colorScheme.onSurface)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(CodeLanguage.PYTHON, CodeLanguage.C, CodeLanguage.CPP).forEach { lang ->
                            FilterChip(
                                selected = language == lang,
                                onClick = { language = lang },
                                label = { Text(lang.name, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeSpec.accentColor.copy(alpha = 0.2f), selectedLabelColor = themeSpec.accentColor)
                            )
                        }
                        IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Code Editor Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                ) {
                    BasicTextField(
                        value = source,
                        onValueChange = { source = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFF8FAFC)
                        ),
                        cursorBrush = SolidColor(themeSpec.accentColor),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }

                // Action Buttons: Run & Insert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val res = LocalCodeAnalyzer.analyze(source, language)
                            diagnostics = res.diagnostics
                            outputText = res.estimatedOutput
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Виконати", color = Color.White)
                    }

                    Button(
                        onClick = {
                            val card = CodeBlockEntity(
                                id = UUID.randomUUID().toString(),
                                x = 100f,
                                y = 100f,
                                language = language,
                                source = source,
                                consoleOutput = outputText.ifBlank { "Executed" }
                            )
                            onInsertCodeBlock(card)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddCard, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Додати картку", color = Color.White)
                    }
                }

                // Output Console
                if (outputText.isNotEmpty() || diagnostics.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth().height(90.dp).padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                            Text("Термінал виводу:", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            if (outputText.isNotEmpty()) {
                                Text(outputText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF38BDF8))
                            }
                            diagnostics.forEach { diag ->
                                Text(diag, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFF87171))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopStudyDeckDialog(
    cards: List<FlashcardEntity>,
    onSaveCard: (FlashcardEntity) -> Unit,
    onDismissRequest: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    var isReviewMode by remember { mutableStateOf(false) }
    var currentCardIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    // New card fields
    var newFront by remember { mutableStateOf("") }
    var newBack by remember { mutableStateOf("") }

    val activeCard = cards.getOrNull(currentCardIndex)

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = themeSpec.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.width(460.dp).padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Icon(Icons.Default.School, contentDescription = null, tint = themeSpec.accentColor)
                        Text(if (isReviewMode) "Повторення SM-2" else "Картки для навчання", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = themeSpec.colorScheme.onSurface)
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant)
                    }
                }

                if (isReviewMode && activeCard != null) {
                    // Flashcard Card View
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = themeSpec.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().height(160.dp).clickable { showAnswer = !showAnswer }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (showAnswer) activeCard.back else activeCard.front,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = if (showAnswer) themeSpec.accentColor else themeSpec.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (showAnswer) "Відповідь (клікніть щоб приховати)" else "Клікніть щоб показати відповідь",
                                    fontSize = 10.sp,
                                    color = themeSpec.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Rating Buttons (SM-2)
                    if (showAnswer) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                1 to "Again",
                                3 to "Hard",
                                4 to "Good",
                                5 to "Easy"
                            ).forEach { (score, label) ->
                                Button(
                                    onClick = {
                                        val next = SpacedRepetitionScheduler.calculateNextReview(
                                            rating = score,
                                            currentRepetitions = activeCard.repetitions,
                                            currentIntervalDays = activeCard.intervalDays,
                                            currentEaseFactor = activeCard.easeFactor
                                        )
                                        val updated = activeCard.copy(
                                            repetitions = next.repetitions,
                                            intervalDays = next.intervalDays,
                                            easeFactor = next.easeFactor,
                                            dueAt = next.nextReviewTimestampMs
                                        )
                                        onSaveCard(updated)
                                        showAnswer = false
                                        if (currentCardIndex < cards.size - 1) currentCardIndex++
                                        else isReviewMode = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when (score) {
                                            1 -> Color(0xFFEF4444)
                                            3 -> Color(0xFFF59E0B)
                                            4 -> Color(0xFF3B82F6)
                                            else -> Color(0xFF10B981)
                                        }
                                    )
                                ) {
                                    Text(label, fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    // Create card section
                    OutlinedTextField(
                        value = newFront,
                        onValueChange = { newFront = it },
                        label = { Text("Питання (Лицьова сторона)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newBack,
                        onValueChange = { newBack = it },
                        label = { Text("Відповідь (Зворотна сторона)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (newFront.isNotBlank() && newBack.isNotBlank()) {
                                    val card = FlashcardEntity(
                                        id = UUID.randomUUID().toString(),
                                        deckId = "default",
                                        front = newFront,
                                        back = newBack
                                    )
                                    onSaveCard(card)
                                    newFront = ""
                                    newBack = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Додати картку")
                        }

                        if (cards.isNotEmpty()) {
                            Button(
                                onClick = {
                                    isReviewMode = true
                                    currentCardIndex = 0
                                    showAnswer = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Повторювати (${cards.size})")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopTextInputDialog(
    initialText: String = "",
    onConfirm: (TextBlockEntity) -> Unit,
    onDismissRequest: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    var text by remember { mutableStateOf(initialText) }
    var fontSize by remember { mutableStateOf(18f) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var fontFamily by remember { mutableStateOf("SANS") }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = themeSpec.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.width(420.dp).padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Вставка тексту", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = themeSpec.colorScheme.onSurface)
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Введіть текст нотатки...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                // Formatting toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = isBold,
                            onClick = { isBold = !isBold },
                            label = { Text("B", fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = isItalic,
                            onClick = { isItalic = !isItalic },
                            label = { Text("I", fontStyle = FontStyle.Italic) }
                        )
                        FilterChip(
                            selected = fontFamily == "MONO",
                            onClick = { fontFamily = if (fontFamily == "MONO") "SANS" else "MONO" },
                            label = { Text("Mono") }
                        )
                    }

                    Text("Розмір: ${fontSize.toInt()}px", fontSize = 12.sp, color = themeSpec.accentColor, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 12f..48f,
                    colors = SliderDefaults.colors(thumbColor = themeSpec.accentColor, activeTrackColor = themeSpec.accentColor)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismissRequest, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Text("Скасувати")
                    }
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onConfirm(
                                    TextBlockEntity(
                                        id = UUID.randomUUID().toString(),
                                        text = text,
                                        x = 120f,
                                        y = 120f,
                                        fontSize = fontSize,
                                        isBold = isBold,
                                        isItalic = isItalic,
                                        isUnderline = isUnderline,
                                        fontFamily = fontFamily
                                    )
                                )
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Додати", color = Color.White)
                    }
                }
            }
        }
    }
}
