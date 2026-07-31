package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.academic.code.CodeDiagnostic
import com.example.R
import com.example.academic.code.CodeRunResult
import com.example.academic.code.LocalCodeAnalyzer
import com.example.data.models.CodeLanguage
import kotlinx.coroutines.delay

@Composable
fun CodeLabDialog(
    onDismiss: () -> Unit,
    onAddToCanvas: ((CodeLanguage, String, CodeRunResult) -> Unit)? = null,
    initialLanguage: CodeLanguage = CodeLanguage.PYTHON,
    initialSource: String = defaultSnippet(initialLanguage)
) {
    var language by remember { mutableStateOf(initialLanguage) }
    var source by remember { mutableStateOf(initialSource) }
    var result by remember { mutableStateOf<CodeRunResult?>(null) }
    val editorFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(180)
        editorFocusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.local_code_lab), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.code_lab_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }

                Spacer(Modifier.height(12.dp))

                TabRow(selectedTabIndex = language.ordinal) {
                    CodeLanguage.entries.forEach { candidate ->
                        Tab(
                            selected = language == candidate,
                            onClick = {
                                if (language != candidate) {
                                    language = candidate
                                    source = defaultSnippet(candidate)
                                    result = null
                                }
                            },
                            text = { Text(candidate.displayName) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = source,
                    onValueChange = {
                        source = it
                        result = null
                    },
                    label = { Text(stringResource(R.string.code)) },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.15f)
                        .focusRequester(editorFocusRequester)
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { result = LocalCodeAnalyzer.run(source, language) },
                        enabled = source.isNotBlank()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.run))
                    }

                    if (onAddToCanvas != null) {
                        OutlinedButton(
                            onClick = {
                                val currentResult = result ?: LocalCodeAnalyzer.run(source, language).also { result = it }
                                onAddToCanvas(language, source, currentResult)
                            },
                            enabled = source.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.add_to_canvas))
                        }
                    }

                    Text(
                        stringResource(R.string.code_sandbox_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                ConsolePanel(
                    result = result,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.85f)
                )
            }
        }
    }
}

@Composable
private fun ConsolePanel(result: CodeRunResult?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xFF111827),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(R.string.console),
            color = Color(0xFF93C5FD),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        if (result == null) {
            Text(
                stringResource(R.string.press_run_hint),
                color = Color(0xFF9CA3AF),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            return@Column
        }

        SelectionContainer {
            Column {
                if (result.output.isNotBlank()) {
                    Text(
                        result.output,
                        color = Color(0xFFF3F4F6),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                } else if (result.diagnostics.isEmpty()) {
                    Text(
                        stringResource(R.string.program_no_output),
                        color = Color(0xFF9CA3AF),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }

                result.diagnostics.forEach { diagnostic ->
                    val color = when (diagnostic.severity) {
                        CodeDiagnostic.Severity.ERROR -> Color(0xFFFCA5A5)
                        CodeDiagnostic.Severity.WARNING -> Color(0xFFFDE68A)
                        CodeDiagnostic.Severity.INFO -> Color(0xFF93C5FD)
                    }
                    Text(
                        "line ${diagnostic.line}: ${diagnostic.message}",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    }
}

private fun defaultSnippet(language: CodeLanguage): String = when (language) {
    CodeLanguage.PYTHON -> """
        topic = "Thermodynamics"
        temperature = 20 + 5
        print(topic, temperature)
    """.trimIndent()

    CodeLanguage.C -> """
        #include <stdio.h>
        int main() {
            double energy = 12.5;
            printf("E = %.1f J\\n", energy);
            return 0;
        }
    """.trimIndent()

    CodeLanguage.CPP -> """
        #include <iostream>
        int main() {
            int a = 4;
            int b = 3;
            std::cout << "sum=" << a + b << std::endl;
            return 0;
        }
    """.trimIndent()
}
