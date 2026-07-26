package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiProvider
import com.example.ai.AiProviderRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProviderPickerSheet(
    onPick: (providerId: String, apiKey: String, endpoint: String?, model: String?) -> Unit,
    onDismiss: () -> Unit,
    currentProviderId: String = "GEMINI",
    getKeyForProvider: (String) -> String = { "" },
    getEndpointForProvider: (String) -> String = { "" },
    getModelForProvider: (String) -> String = { "" }
) {
    var selectedId by remember { mutableStateOf(currentProviderId.ifBlank { "GEMINI" }) }
    var apiKeyText by remember { mutableStateOf(getKeyForProvider(selectedId)) }
    var endpointText by remember { mutableStateOf(getEndpointForProvider(selectedId)) }
    var modelText by remember { mutableStateOf(getModelForProvider(selectedId)) }
    var isKeyVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Вибір AI-провайдера",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Оберіть модель для роботи асистента конспекту",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(AiProviderRegistry.providers) { provider ->
                    val isSelected = provider.id.equals(selectedId, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedId = provider.id
                                apiKeyText = getKeyForProvider(provider.id)
                                endpointText = getEndpointForProvider(provider.id)
                                modelText = getModelForProvider(provider.id)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedId = provider.id
                                    apiKeyText = getKeyForProvider(provider.id)
                                    endpointText = getEndpointForProvider(provider.id)
                                    modelText = getModelForProvider(provider.id)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = provider.displayName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (provider.supportsVision) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "Vision",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                val storedKey = getKeyForProvider(provider.id)
                                if (storedKey.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ключ збережено", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inputs for selected provider
            val currentProvider = AiProviderRegistry.getProvider(selectedId)
            Text(
                text = "Налаштування ключа (${currentProvider.displayName})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = apiKeyText,
                onValueChange = { apiKeyText = it },
                label = { Text("API Key (${currentProvider.displayName})") },
                singleLine = true,
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isKeyVisible) "Сховати" else "Показати"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedId.equals("CUSTOM", ignoreCase = true) || selectedId.equals("OPENAI", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endpointText,
                    onValueChange = { endpointText = it },
                    label = { Text("Custom Endpoint (опційно)") },
                    placeholder = { Text("https://api.openai.com/v1/chat/completions") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelText,
                    onValueChange = { modelText = it },
                    label = { Text("Model Name (опційно)") },
                    placeholder = { Text("gpt-4o-mini") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Скасувати")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onPick(
                            selectedId,
                            apiKeyText.trim(),
                            endpointText.trim().ifBlank { null },
                            modelText.trim().ifBlank { null }
                        )
                    },
                    enabled = apiKeyText.isNotBlank() || getKeyForProvider(selectedId).isNotBlank()
                ) {
                    Text("Зберегти та обрати")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
