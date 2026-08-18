package com.example.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(
    onLoaded: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableStateOf(0.0f) }
    var statusText by remember { mutableStateOf("Ініціалізація векторного рушія...") }

    val infiniteTransition = rememberInfiniteTransition(label = "loading_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(250)
        progress = 0.25f
        statusText = "Завантаження пресетів та пензлів..."
        delay(300)
        progress = 0.55f
        statusText = "Ініціалізація тем та неоморфного дизайну..."
        delay(300)
        progress = 0.85f
        statusText = "Перевірка та відновлення автосейву..."
        delay(250)
        progress = 1.0f
        statusText = "Готово!"
        delay(200)
        onLoaded()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated Logo Box
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Spinning gradient ring
                Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF38BDF8),
                                Color(0xFF818CF8),
                                Color(0xFFC084FC),
                                Color(0xFFF472B6),
                                Color(0xFF38BDF8)
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 320f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner Glow Circle
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B).copy(alpha = 0.9f),
                    modifier = Modifier.size(96.dp),
                    shadowElevation = 12.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = "Sketchpad Logo",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title
            Text(
                text = "Sketchpad Pro",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Text(
                text = "v2.0.0 Windows Desktop Edition",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Progress Bar
            Column(
                modifier = Modifier.width(320.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF334155),
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}
