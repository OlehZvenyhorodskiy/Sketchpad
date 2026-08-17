package com.example.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.DesktopViewModel
import com.example.desktop.theme.LocalThemeSpec

@Composable
fun DesktopPairingDialog(
    viewModel: DesktopViewModel,
    onClose: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val currentPin by viewModel.sketchLinkServer.currentPin.collectAsState()
    val connectedClients by viewModel.sketchLinkServer.connectedClientsCount.collectAsState()
    val localIps = remember { viewModel.sketchLinkServer.getLocalIpAddresses() }
    val primaryIp = localIps.firstOrNull() ?: "127.0.0.1"

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = themeSpec.accentColor)
            ) {
                Text("Готово", color = Color.Black)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TabletAndroid, contentDescription = null, tint = themeSpec.accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Підключення планшета (SketchLink)")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PIN Display Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, themeSpec.accentColor, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = themeSpec.colorScheme.surfaceContainerHighest
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("6-ЗНАЧНИЙ PIN-КОД", fontSize = 11.sp, color = themeSpec.accentColor, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            currentPin,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp,
                            color = themeSpec.colorScheme.onSurface
                        )
                    }
                }

                // Connection details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("IP-адреса ПК: $primaryIp", fontSize = 12.sp, color = themeSpec.colorScheme.onSurface)
                    Text("Порт WebSocket: 8765", fontSize = 12.sp, color = themeSpec.colorScheme.onSurface)
                    Text(
                        if (connectedClients > 0) "🟢 Підключено пристроїв: $connectedClients" else "🟡 Очікування підключення з планшета...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (connectedClients > 0) Color(0xFF22C55E) else Color(0xFFEAB308)
                    )
                }

                HorizontalDivider()

                // Instructions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Як підключитися:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("1. Wi-Fi LAN: відкрийте додаток на планшеті та введіть PIN або IP $primaryIp", fontSize = 11.sp)
                    Text("2. USB: виконайте 'adb reverse tcp:8765 tcp:8765' та підключіть localhost:8765", fontSize = 11.sp)
                    Text("3. Активуйте режим 'Біле полотно' на планшеті для малювання без затримок (<16ms).", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.sketchLinkServer.refreshPin() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Згенерувати новий PIN")
                }
            }
        }
    )
}
