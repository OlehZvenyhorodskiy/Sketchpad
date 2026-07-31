package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ai.ChatMessage
import com.example.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FloatingAiWindow(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onClose: () -> Unit,
    onSaveApiKey: ((String) -> Unit)? = null,
    initialApiKey: String = "",
    selectedProviderDisplayName: String = "Google Gemini",
    onChangeProvider: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    var offsetX by rememberSaveable { mutableFloatStateOf(40f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(200f) }
    var expandedWidthDp by rememberSaveable { mutableFloatStateOf(420f) }
    var expandedHeightDp by rememberSaveable { mutableFloatStateOf(560f) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val scope = rememberCoroutineScope()
    val windowWidthDp = if (isExpanded) expandedWidthDp else 200f
    val windowHeightDp = if (isExpanded) expandedHeightDp else 52f
    val windowWidthPx = with(density) { windowWidthDp.dp.toPx() }
    val windowHeightPx = with(density) { windowHeightDp.dp.toPx() }

    fun clampWindowPosition() {
        val currentWidthPx = with(density) { (if (isExpanded) expandedWidthDp else 200f).dp.toPx() }
        val currentHeightPx = with(density) { (if (isExpanded) expandedHeightDp else 52f).dp.toPx() }
        offsetX = offsetX.coerceIn(0f, (screenWidthPx - currentWidthPx).coerceAtLeast(0f))
        offsetY = offsetY.coerceIn(0f, (screenHeightPx - currentHeightPx).coerceAtLeast(0f))
    }

    val dragHandleModifier = Modifier.pointerInput(isExpanded, screenWidthPx, screenHeightPx) {
        detectDragGestures(
            onDragEnd = {
                val targetX = if (offsetX + windowWidthPx / 2f < screenWidthPx / 2f) {
                    16f
                } else {
                    (screenWidthPx - windowWidthPx - 16f).coerceAtLeast(16f)
                }
                scope.launch {
                    val anim = Animatable(offsetX)
                    anim.animateTo(targetX, spring()) {
                        offsetX = value
                    }
                }
            },
            onDrag = { change, dragAmount ->
                change.consume()
                offsetX = (offsetX + dragAmount.x).coerceIn(
                    0f,
                    (screenWidthPx - windowWidthPx).coerceAtLeast(0f)
                )
                offsetY = (offsetY + dragAmount.y).coerceIn(
                    0f,
                    (screenHeightPx - windowHeightPx).coerceAtLeast(0f)
                )
            }
        )
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
            tonalElevation = 12.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(windowWidthDp.dp)
                .height(windowHeightDp.dp)
        ) {
            Box {
                Column {
                // Header / Drag handle bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .then(dragHandleModifier)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ai_assistant),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            isExpanded = !isExpanded
                            clampWindowPosition()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.OpenInFull,
                            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Window Body
                AnimatedVisibility(visible = isExpanded) {
                    Box(modifier = Modifier.height((expandedHeightDp - 52f).coerceAtLeast(280f).dp)) {
                        AiChatContent(
                            messages = messages,
                            isLoading = isLoading,
                            onSendMessage = onSendMessage,
                            onSaveApiKey = onSaveApiKey,
                            initialApiKey = initialApiKey,
                            selectedProviderDisplayName = selectedProviderDisplayName,
                            onChangeProvider = onChangeProvider,
                            autoFocusInput = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                }

                if (isExpanded) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = stringResource(R.string.resize_window),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f))
                            .padding(7.dp)
                            .pointerInput(screenWidthPx, screenHeightPx) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaWidthDp = with(density) { dragAmount.x.toDp().value }
                                    val deltaHeightDp = with(density) { dragAmount.y.toDp().value }
                                    val maximumWidthDp = (configuration.screenWidthDp - 24).coerceAtLeast(300).toFloat()
                                    val maximumHeightDp = (configuration.screenHeightDp - 24).coerceAtLeast(340).toFloat()
                                    expandedWidthDp = (expandedWidthDp + deltaWidthDp).coerceIn(300f, maximumWidthDp)
                                    expandedHeightDp = (expandedHeightDp + deltaHeightDp).coerceIn(340f, maximumHeightDp)
                                    clampWindowPosition()
                                }
                            }
                    )
                }
            }
        }
    }
}
