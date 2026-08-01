package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

data class TextFormatting(
    val fontSize: Float = 24f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontFamily: String = "SANS",
    val alignment: String = "LEFT",
    val width: Float = 280f
)

@Composable
fun TextInputDialog(
    initialText: String = "",
    initialFormatting: TextFormatting = TextFormatting(),
    onConfirm: (text: String, formatting: TextFormatting) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var fontSize by remember(initialFormatting) { mutableFloatStateOf(initialFormatting.fontSize) }
    var width by remember(initialFormatting) { mutableFloatStateOf(initialFormatting.width) }
    var isBold by remember(initialFormatting) { mutableStateOf(initialFormatting.isBold) }
    var isItalic by remember(initialFormatting) { mutableStateOf(initialFormatting.isItalic) }
    var isUnderline by remember(initialFormatting) { mutableStateOf(initialFormatting.isUnderline) }
    var fontFamily by remember(initialFormatting) { mutableStateOf(initialFormatting.fontFamily) }
    var alignment by remember(initialFormatting) { mutableStateOf(initialFormatting.alignment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.text_input_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.enter_your_text)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize.sp,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = when (fontFamily) {
                            "SERIF" -> FontFamily.Serif
                            "MONO" -> FontFamily.Monospace
                            else -> FontFamily.SansSerif
                        },
                        textAlign = when (alignment) {
                            "CENTER" -> TextAlign.Center
                            "RIGHT" -> TextAlign.Right
                            else -> TextAlign.Left
                        },
                        textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    minLines = 3,
                    maxLines = 8
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("SANS" to "Sans", "SERIF" to "Serif", "MONO" to "Mono").forEach { (id, label) ->
                        FilterChip(selected = fontFamily == id, onClick = { fontFamily = id }, label = { Text(label) })
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(modifier = Modifier.padding(horizontal = 2.dp)) {
                            IconButton(onClick = { isBold = !isBold }) {
                                Icon(Icons.Default.FormatBold, null, tint = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { isItalic = !isItalic }) {
                                Icon(Icons.Default.FormatItalic, null, tint = if (isItalic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { isUnderline = !isUnderline }) {
                                Icon(Icons.Default.FormatUnderlined, null, tint = if (isUnderline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "LEFT" to Icons.Default.FormatAlignLeft,
                        "CENTER" to Icons.Default.FormatAlignCenter,
                        "RIGHT" to Icons.Default.FormatAlignRight
                    ).forEach { (id, icon) ->
                        IconButton(onClick = { alignment = id }) {
                            Icon(icon, null, tint = if (alignment == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Text(stringResource(R.string.font_size_px, fontSize.toInt()), fontWeight = FontWeight.Bold)
                Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 10f..120f)
                Text("Ширина поля: ${width.toInt()} px", fontWeight = FontWeight.Bold)
                Slider(value = width, onValueChange = { width = it }, valueRange = 120f..900f)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (text.isNotBlank()) onConfirm(
                    text,
                    TextFormatting(fontSize, isBold, isItalic, isUnderline, fontFamily, alignment, width)
                )
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
