package com.example.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.desktop.theme.LocalThemeSpec
import com.example.shared.model.AudioRecordingEntity
import com.example.shared.model.PageEntity

@Composable
fun DesktopPageStripModal(
    pages: List<PageEntity>,
    currentPageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = themeSpec.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.width(480.dp).padding(12.dp)
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
                        Icon(Icons.Default.AutoStories, contentDescription = null, tint = themeSpec.accentColor)
                        Text("Сторінки документа", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = themeSpec.colorScheme.onSurface)
                        Badge(containerColor = themeSpec.accentColor.copy(alpha = 0.2f)) {
                            Text("${pages.size}", color = themeSpec.accentColor, fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onAddPage, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Додати сторінку", tint = themeSpec.accentColor)
                        }
                        IconButton(onClick = onDismissRequest, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider()

                // Pages Strip
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(pages) { idx, page ->
                        val isSelected = idx == currentPageIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) themeSpec.accentColor.copy(alpha = 0.12f) else themeSpec.colorScheme.surfaceContainer,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, themeSpec.accentColor) else null,
                            modifier = Modifier
                                .width(100.dp)
                                .fillMaxHeight()
                                .clickable {
                                    onSelectPage(idx)
                                    onDismissRequest()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Стор. ${idx + 1}",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = themeSpec.colorScheme.onSurface
                                    )
                                    if (pages.size > 1) {
                                        IconButton(
                                            onClick = { onDeletePage(idx) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Видалити", tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }

                                // Mini Page representation
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.size(60.dp, 60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${page.getEffectiveLayers().sumOf { it.totalElements }} ел.",
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Text(
                                    text = if (isSelected) "Активна" else "Перейти",
                                    fontSize = 10.sp,
                                    color = if (isSelected) themeSpec.accentColor else themeSpec.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onAddPage,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Додати нову сторінку")
                }
            }
        }
    }
}

@Composable
fun DesktopTimelineSliderModal(
    totalVersions: Int,
    currentVersionIndex: Int,
    onVersionChanged: (Int) -> Unit,
    onRestoreVersion: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    var selectedVer by remember { mutableStateOf(currentVersionIndex.toFloat()) }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = themeSpec.accentColor)
                        Text("Історія версій полотна", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = themeSpec.colorScheme.onSurface)
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    text = "Версія ${selectedVer.toInt() + 1} з $totalVersions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = themeSpec.accentColor
                )

                Slider(
                    value = selectedVer,
                    onValueChange = {
                        selectedVer = it
                        onVersionChanged(it.toInt())
                    },
                    valueRange = 0f..(totalVersions - 1).coerceAtLeast(1).toFloat(),
                    steps = if (totalVersions > 2) totalVersions - 2 else 0,
                    colors = SliderDefaults.colors(thumbColor = themeSpec.accentColor, activeTrackColor = themeSpec.accentColor)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismissRequest, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Text("Скасувати")
                    }
                    Button(
                        onClick = {
                            onRestoreVersion()
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Відновити стан", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopAudioManagementModal(
    recordings: List<AudioRecordingEntity>,
    isPlaying: Boolean,
    playingProgress: Float,
    onPlayRecording: (AudioRecordingEntity) -> Unit,
    onStopRecording: () -> Unit,
    onDeleteRecording: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = themeSpec.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.width(440.dp).padding(12.dp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = themeSpec.accentColor)
                        Text("Аудіо-нотатки", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = themeSpec.colorScheme.onSurface)
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити", tint = themeSpec.colorScheme.onSurfaceVariant)
                    }
                }

                if (recordings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Немає збережених аудіозаписів", fontSize = 12.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recordings) { rec ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = themeSpec.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rec.displayName(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = themeSpec.colorScheme.onSurface)
                                        Text(rec.formattedDuration(), fontSize = 10.sp, color = themeSpec.colorScheme.onSurfaceVariant)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                if (isPlaying) onStopRecording() else onPlayRecording(rec)
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Відтворити",
                                                tint = themeSpec.accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteRecording(rec.id) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Видалити", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isPlaying) {
                    LinearProgressIndicator(
                        progress = { playingProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = themeSpec.accentColor
                    )
                }
            }
        }
    }
}
