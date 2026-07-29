package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineSliderBottomSheet(
    totalVersions: Int,
    currentVersionIndex: Int,
    onVersionSelected: (Int) -> Unit,
    onRestoreVersion: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(currentVersionIndex.coerceIn(0, maxOf(0, totalVersions - 1))) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Історія версій полотна", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Версія ${selectedIndex + 1} з $totalVersions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (totalVersions > 1) {
                Slider(
                    value = selectedIndex.toFloat(),
                    onValueChange = {
                        selectedIndex = it.toInt()
                        onVersionSelected(selectedIndex)
                    },
                    valueRange = 0f..(totalVersions - 1).toFloat(),
                    steps = maxOf(0, totalVersions - 2)
                )
            } else {
                Text("Поки немає збережених знімків історії.")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onRestoreVersion(selectedIndex)
                        onDismiss()
                    },
                    enabled = totalVersions > 1
                ) {
                    Text("Відновити цю версію")
                }
            }
        }
    }
}
