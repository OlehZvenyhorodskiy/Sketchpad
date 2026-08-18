package com.example.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.desktop.ai.*
import com.example.desktop.theme.LocalThemeSpec
import com.example.desktop.theme.ThemedPanel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DesktopFloatingAiWindow(
    messages: List<ChatMessage>,
    onSendMessage: (String, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    var inputText by remember { mutableStateOf("") }
    var attachCanvasVision by remember { mutableStateOf(false) }
    var windowOffset by remember { mutableStateOf(Offset(200f, 100f)) }

    Box(
        modifier = modifier
            .offset { IntOffset(windowOffset.x.roundToInt(), windowOffset.y.roundToInt()) }
            .size(380.dp, 480.dp)
    ) {
        ThemedPanel(
            modifier = Modifier.fillMaxSize(),
            surfaceAlpha = 0.98f
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Draggable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeSpec.accentColor.copy(alpha = 0.15f))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                windowOffset = Offset(
                                    (windowOffset.x + dragAmount.x).coerceAtLeast(0f),
                                    (windowOffset.y + dragAmount.y).coerceAtLeast(0f)
                                )
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = themeSpec.accentColor, modifier = Modifier.size(18.dp))
                        Text("AI Assistant", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themeSpec.colorScheme.onSurface)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = "Провайдер", tint = themeSpec.accentColor, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Chat Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = themeSpec.accentColor.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Запитайте AI про конспект,", fontSize = 12.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                                    Text("формули або програмний код", fontSize = 11.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    items(messages) { msg ->
                        val isUser = msg.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isUser) 12.dp else 2.dp,
                                    bottomEnd = if (isUser) 2.dp else 12.dp
                                ),
                                color = if (isUser) themeSpec.accentColor else themeSpec.colorScheme.surfaceContainer,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = msg.text,
                                        fontSize = 12.sp,
                                        color = if (isUser) Color.White else themeSpec.colorScheme.onSurface,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = themeSpec.accentColor)
                                Text("AI думає...", fontSize = 11.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Vision screenshot toggle
                    IconButton(
                        onClick = { attachCanvasVision = !attachCanvasVision },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Додати скріншот полотна",
                            tint = if (attachCanvasVision) themeSpec.accentColor else themeSpec.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Запитати AI...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                onSendMessage(inputText, attachCanvasVision)
                                inputText = ""
                                attachCanvasVision = false
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(themeSpec.accentColor)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Надіслати", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopAiProviderPickerModal(
    preferences: DesktopAiPreferences,
    onDismissRequest: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    var selectedProviderId by remember { mutableStateOf(preferences.selectedProviderId) }
    var apiKey by remember { mutableStateOf(preferences.getKey(selectedProviderId)) }
    var endpoint by remember { mutableStateOf(preferences.getEndpoint(selectedProviderId)) }
    var selectedModel by remember { mutableStateOf(preferences.getModel(selectedProviderId)) }
    var showKey by remember { mutableStateOf(false) }

    val currentProvider = AiProviderRegistry.getProvider(selectedProviderId)

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
                        Icon(Icons.Default.Tune, contentDescription = null, tint = themeSpec.accentColor)
                        Text("Налаштування AI провайдера", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = themeSpec.colorScheme.onSurface)
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant)
                    }
                }

                // Provider Selection Chips
                Text("Оберіть сервіс", fontSize = 12.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AiProviderRegistry.providers.forEach { p ->
                        FilterChip(
                            selected = selectedProviderId == p.id,
                            onClick = {
                                selectedProviderId = p.id
                                apiKey = preferences.getKey(p.id)
                                endpoint = preferences.getEndpoint(p.id)
                                selectedModel = preferences.getModel(p.id).ifBlank { p.defaultModel }
                            },
                            label = { Text(p.displayName.take(8), fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeSpec.accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = themeSpec.accentColor
                            )
                        )
                    }
                }

                // API Key field
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("Вставте ключ...") },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Model Selection
                if (currentProvider.availableModels.isNotEmpty()) {
                    Text("Модель", fontSize = 12.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentProvider.availableModels.forEach { m ->
                            FilterChip(
                                selected = (selectedModel.ifBlank { currentProvider.defaultModel }) == m,
                                onClick = { selectedModel = m },
                                label = { Text(m.removePrefix("gemini-").removePrefix("claude-3-"), fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Custom Endpoint for Local LLM
                if (selectedProviderId == "CUSTOM" || selectedProviderId == "DEEPSEEK") {
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        label = { Text("Endpoint URL") },
                        placeholder = { Text("http://localhost:11434/v1/chat/completions") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Save button
                Button(
                    onClick = {
                        preferences.selectedProviderId = selectedProviderId
                        preferences.setKey(selectedProviderId, apiKey)
                        preferences.setEndpoint(selectedProviderId, endpoint)
                        preferences.setModel(selectedProviderId, selectedModel)
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor)
                ) {
                    Text("Зберегти налаштування", color = Color.White)
                }
            }
        }
    }
}
