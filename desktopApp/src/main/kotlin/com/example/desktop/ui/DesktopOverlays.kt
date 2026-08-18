package com.example.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.theme.LocalThemeSpec
import com.example.desktop.theme.ThemedPanel
import com.example.shared.academic.LocalCodeAnalyzer
import com.example.shared.model.CodeBlockEntity
import kotlin.math.roundToInt

@Composable
fun DesktopBottomLeftOverlay(
    currentPageIndex: Int,
    totalPages: Int,
    zoomScale: Float,
    onPageIndicatorClick: () -> Unit,
    onZoomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current

    ThemedPanel(
        modifier = modifier.padding(16.dp),
        surfaceAlpha = 0.94f
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Page Indicator
            Surface(
                onClick = onPageIndicatorClick,
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = themeSpec.accentColor, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${currentPageIndex + 1} / $totalPages",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeSpec.colorScheme.onSurface
                    )
                }
            }

            VerticalDivider(modifier = Modifier.height(16.dp))

            // Zoom Indicator
            Surface(
                onClick = onZoomClick,
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = null, tint = themeSpec.accentColor, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeSpec.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopLassoOverlay(
    points: List<Offset>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
            close()
        }

        // Semi-transparent fill
        drawPath(path = path, color = Color(0xFF38BDF8).copy(alpha = 0.15f))

        // Dashed border
        drawPath(
            path = path,
            color = Color(0xFF38BDF8),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        )
    }
}

@Composable
fun DesktopCodeBlockCanvasCard(
    codeBlock: CodeBlockEntity,
    onUpdate: (CodeBlockEntity) -> Unit,
    onDelete: () -> Unit,
    zoomScale: Float,
    panOffset: Pair<Float, Float>,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    var isRunning by remember { mutableStateOf(false) }

    val screenX = codeBlock.x * zoomScale + panOffset.first
    val screenY = codeBlock.y * zoomScale + panOffset.second

    Box(
        modifier = modifier
            .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
            .width((codeBlock.width * zoomScale).dp.coerceAtLeast(260.dp))
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
    ) {
        Column {
            // Language Banner & Drag Handle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = themeSpec.accentColor, modifier = Modifier.size(16.dp))
                    Text(codeBlock.language.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFF8FAFC))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = {
                            val res = LocalCodeAnalyzer.analyze(codeBlock.code, codeBlock.language)
                            onUpdate(codeBlock.copy(consoleOutput = res.estimatedOutput))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Code Content
            Text(
                text = codeBlock.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.padding(10.dp)
            )

            // Output banner
            if (codeBlock.output.isNotBlank()) {
                HorizontalDivider(color = Color(0xFF334155))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "▶ ${codeBlock.output.trim()}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}
