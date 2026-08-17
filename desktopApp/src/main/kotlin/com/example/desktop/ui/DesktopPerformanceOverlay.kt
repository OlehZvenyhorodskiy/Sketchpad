package com.example.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.DesktopViewModel

@Composable
fun DesktopPerformanceOverlay(
    viewModel: DesktopViewModel,
    modifier: Modifier = Modifier
) {
    val fps by viewModel.fps.collectAsState()
    val latency by viewModel.strokeLatencyMs.collectAsState()
    val packets by viewModel.packetsPerSec.collectAsState()
    val runtime = Runtime.getRuntime()
    val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    val totalMemMb = runtime.totalMemory() / 1024 / 1024

    Surface(
        modifier = modifier
            .padding(16.dp)
            .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xCC0B0F19),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("⚡ PERFORMANCE HUD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
            Text("FPS: $fps (target: 60)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
            Text("Stroke Latency: <${latency}ms", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
            Text("SketchLink Stream: $packets pkt/s", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
            Text("RAM Usage: ${usedMemMb}MB / ${totalMemMb}MB", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
        }
    }
}
