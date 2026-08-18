package com.example.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.desktop.theme.LocalThemeSpec

@Composable
fun ExitProtectionDialog(
    onSaveAndExit: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onCancel: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Warning",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Незбережені зміни",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = themeSpec.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "Ви маєте незбережені зміни у поточному документі. Якщо закрити програму без збереження, останні зміни можуть бути втрачені.\n\nБажаєте зберегти проект перед виходом?",
                fontSize = 14.sp,
                color = themeSpec.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onSaveAndExit,
                colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Зберегти і закрити", color = Color.White)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Скасувати", color = themeSpec.colorScheme.onSurface)
                }
                FilledTonalButton(
                    onClick = onExitWithoutSaving,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Закрити без збереження", color = Color(0xFFEF4444))
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = themeSpec.colorScheme.surface
    )
}
