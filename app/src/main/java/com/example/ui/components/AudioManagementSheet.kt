package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.models.AudioRecordingEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioManagementSheet(
    recordings: List<AudioRecordingEntity>,
    currentlyPlayingPath: String?,
    isPlaying: Boolean,
    onPlayClick: (AudioRecordingEntity) -> Unit,
    onPauseClick: () -> Unit,
    onRenameClick: (AudioRecordingEntity, String) -> Unit,
    onDeleteClick: (AudioRecordingEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.audio_notes_count, recordings.size), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recordings.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_audio_notes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recordings, key = { it.id }) { recording ->
                        val isThisPlaying = currentlyPlayingPath == recording.filePath && isPlaying
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isThisPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = { if (isThisPlaying) onPauseClick() else onPlayClick(recording) },
                                    modifier = Modifier.size(42.dp).clip(CircleShape)
                                        .background(if (isThisPlaying) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(
                                        if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        stringResource(R.string.play),
                                        tint = if (isThisPlaying) MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    if (renamingId == recording.id) {
                                        OutlinedTextField(
                                            value = renameText, onValueChange = { renameText = it },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp), singleLine = true
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            TextButton(onClick = { onRenameClick(recording, renameText.trim()); renamingId = null }) {
                                                Text(stringResource(R.string.save), fontSize = 12.sp)
                                            }
                                            TextButton(onClick = { renamingId = null }) { Text(stringResource(R.string.cancel), fontSize = 12.sp) }
                                        }
                                    } else {
                                        Text(recording.displayName(), style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${recording.formattedDuration()} • ${recording.formattedDate()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(onClick = { renamingId = recording.id; renameText = recording.displayName() }) {
                                    Icon(Icons.Default.Edit, stringResource(R.string.rename), modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onDeleteClick(recording) }) {
                                    Icon(Icons.Default.Delete, stringResource(R.string.delete), modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
