package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AttendanceLogEntity
import com.example.security.BiometricEncryptionUtils
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val logs by viewModel.allLogs.collectAsState()
    val secretKey by viewModel.secretKey.collectAsState()

    // Keep track of which card details are expanded
    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }

    val totalLogs = logs.size
    val successCount = logs.count { it.status == "SUCCESS" }
    val failedCount = logs.count { it.status == "FAILED" }
    val pendingCount = logs.count { it.status == "PENDING" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Attendance Overview Statistics Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RINGKASAN PRESENSI BIOMETRIK",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (logs.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearAllLogs() },
                                modifier = Modifier.testTag("btn_clear_logs")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Logs",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatColumn(value = totalLogs.toString(), label = "Total Absen", color = MaterialTheme.colorScheme.onSurface)
                        StatColumn(value = successCount.toString(), label = "Terkirim", color = Color(0xFF388E3C))
                        StatColumn(value = failedCount.toString(), label = "Offline/Gagal", color = MaterialTheme.colorScheme.error)
                        StatColumn(value = pendingCount.toString(), label = "Pending", color = Color(0xFFF57C00))
                    }
                }
            }
        }

        item {
            Text(
                text = "LOG AKTIVITAS TERENKRIPSI",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Log Masih Kosong",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Lakukan presensi sidik jari pada tab absensi untuk mengisi log.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                val isExpanded = expandedStates[log.id] ?: false
                val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }
                val formattedTime = dateFormatter.format(Date(log.timestamp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("log_item_${log.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        when (log.status) {
                            "SUCCESS" -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                            "FAILED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header info of log card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedStates[log.id] = !isExpanded }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (log.status) {
                                                "SUCCESS" -> Color(0x1A10B981) // Glowing translucent green
                                                "FAILED" -> Color(0x1AEF4444)  // Glowing translucent red
                                                else -> Color(0x1AF57C00)       // Glowing translucent orange
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (log.status) {
                                            "SUCCESS" -> Icons.Default.CloudDone
                                            "FAILED" -> Icons.Default.CloudOff
                                            else -> Icons.Default.CloudQueue
                                        },
                                        contentDescription = null,
                                        tint = when (log.status) {
                                            "SUCCESS" -> Color(0xFF388E3C)
                                            "FAILED" -> MaterialTheme.colorScheme.error
                                            else -> Color(0xFFF57C00)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = log.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$formattedTime • ID: ${log.employeeId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (log.status == "FAILED") {
                                    IconButton(
                                        onClick = { viewModel.retrySync(log) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("btn_retry_sync_${log.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Sync Ulang",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Expanded Payload Encryption Details
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0CFFFFFF))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Decrypted Data (Decrypted live using symmetric key from settings)
                                val decryptedPayload = BiometricEncryptionUtils.decryptAES(log.encryptedPayload, secretKey)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0x12FFFFFF)),
                                    border = BorderStroke(1.dp, Color(0x10FFFFFF)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LockOpen,
                                                contentDescription = null,
                                                tint = Color(0xFF388E3C),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "DECRYPTED PAYLOAD (AES-256)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF388E3C)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = decryptedPayload,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Encrypted AES-256 Base64 String
                                Column {
                                    Text(
                                        text = "ENCRYPTED CIPHERTEXT (AES/ECB/PKCS5):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = log.encryptedPayload,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = Color(0x15FFFFFF),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    )
                                }

                                // SHA-256 HMAC Signature
                                Column {
                                    Text(
                                        text = "HMAC-SHA256 INTEGRITY SIGNATURE:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = log.signature,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = Color(0x15FFFFFF),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    )
                                }

                                // API target
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "API Gateway Target:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = log.apiUrl,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.End,
                                        maxLines = 1
                                    )
                                }

                                // Error messages if failed
                                if (log.status == "FAILED" && log.errorMessage != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SyncProblem,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Error: ${log.errorMessage}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatColumn(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
