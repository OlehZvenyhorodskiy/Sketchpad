package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CodeBlockEntity
import com.example.data.models.CodeLanguage
import kotlin.math.roundToInt

@Composable
fun CodeBlockCanvasCard(
    codeBlock: CodeBlockEntity,
    scale: Float,
    panOffset: Offset,
    onEdit: () -> Unit,
    onRun: () -> Unit,
    onDelete: () -> Unit,
    isSelected: Boolean = false,
    isInteractive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val safeScale = scale.coerceIn(0.1f, 10f)
    val widthDp = with(density) { (codeBlock.width * safeScale).toDp() }
    val heightDp = with(density) { (codeBlock.height * safeScale).toDp() }
    val isCompact = widthDp < 260.dp || heightDp < 170.dp
    val languageColor = when (codeBlock.language) {
        CodeLanguage.PYTHON -> Color(0xFF3776AB)
        CodeLanguage.C -> Color(0xFF5C6BC0)
        CodeLanguage.CPP -> Color(0xFF00599C)
    }

    Surface(
        onClick = onEdit,
        enabled = isInteractive,
        modifier = modifier
            .offset {
                IntOffset(
                    (codeBlock.x * safeScale + panOffset.x).roundToInt(),
                    (codeBlock.y * safeScale + panOffset.y).roundToInt()
                )
            }
            .width(widthDp.coerceAtLeast(80.dp))
            .height(heightDp.coerceAtLeast(56.dp))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF111827).copy(alpha = 0.98f),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(languageColor.copy(alpha = 0.92f))
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = codeBlock.language.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (isCompact) 10.sp else 12.sp,
                    modifier = Modifier.weight(1f)
                )
                if (!isCompact && isInteractive) {
                    IconButton(onClick = onRun, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.PlayArrow, "Run", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (isCompact) {
                Text(
                    text = codeBlock.source.lineSequence().firstOrNull().orEmpty(),
                    color = Color(0xFFE5E7EB),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
                return@Column
            }

            Text(
                text = codeBlock.source,
                color = Color(0xFFE5E7EB),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                maxLines = 7,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )

            if (codeBlock.consoleOutput.isNotBlank() || codeBlock.diagnostics.isNotEmpty()) {
                Spacer(Modifier.height(1.dp).fillMaxWidth().background(Color(0xFF374151)))
                Text(
                    text = buildString {
                        if (codeBlock.consoleOutput.isNotBlank()) append(codeBlock.consoleOutput)
                        if (codeBlock.diagnostics.isNotEmpty()) {
                            if (isNotEmpty()) append('\n')
                            append(codeBlock.diagnostics.joinToString("\n"))
                        }
                    },
                    color = if (codeBlock.diagnostics.isEmpty()) Color(0xFF86EFAC) else Color(0xFFFDE68A),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1220))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}
