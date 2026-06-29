package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FingerprintEntity
import com.example.security.BiometricPromptUtils
import androidx.compose.ui.platform.LocalContext
import com.example.ui.MainViewModel

@Composable
fun ScanScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val currentContext = LocalContext.current
    val fingerprints by viewModel.allFingerprints.collectAsState()
    val selectedFingerprint by viewModel.selectedFingerprint.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val scanMessage by viewModel.scanMessage.collectAsState()
    val scanSuccess by viewModel.scanSuccess.collectAsState()
    val apiUrl by viewModel.apiUrl.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }
    var amoledFodAssist by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Simulation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "STATUS TERMINAL ABSENSI",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Real-time Gateway: $apiUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val bioStatus = remember(currentContext) { BiometricPromptUtils.isBiometricAvailable(currentContext) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = when (bioStatus) {
                            "SUCCESS" -> Color(0xFF00FF88)
                            "NONE_ENROLLED" -> Color(0xFFFFB300)
                            else -> Color(0xFFEF4444)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when (bioStatus) {
                            "SUCCESS" -> "Sensor Biometrik Perangkat: AKTIF (Aman)"
                            "NONE_ENROLLED" -> "Sensor Biometrik: Ada (Belum didaftarkan di sistem OS)"
                            "NO_HARDWARE" -> "Sensor Biometrik: Tidak Ada Hardware (Mode Emulasi Aktif)"
                            "HW_UNAVAILABLE" -> "Sensor Biometrik: Sedang Sibuk / Tidak Tersedia"
                            else -> "Sensor Biometrik: Status Tidak Dikenal"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        // Dropdown Selector for Registered Fingerprints
        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dropdownExpanded = true }
                    .testTag("select_fingerprint_card"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Profil Sidik Jari Absensi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedFingerprint?.let { "${it.name} - ${it.employeeId}" }
                                ?: "Pilih Sidik Jari...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedFingerprint != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown"
                    )
                }
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                if (fingerprints.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Belum ada sidik jari terdaftar. Daftar dulu di tab registrasi!") },
                        onClick = { dropdownExpanded = false }
                    )
                } else {
                    fingerprints.forEach { f ->
                        DropdownMenuItem(
                            text = { Text("${f.name} (${f.employeeId}) - ${f.department}") },
                            onClick = {
                                viewModel.selectFingerprint(f)
                                dropdownExpanded = false
                            },
                            modifier = Modifier.testTag("fingerprint_item_${f.employeeId}")
                        )
                    }
                }
            }
        }

        // AMOLED FOD Screen Assist Mode Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (amoledFodAssist) Color(0x2600FF88) else Color(0x1AFFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (amoledFodAssist) Color(0xFF00FF88) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Mode AMOLED FOD Assist",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Kecerahan & cahaya hijau neon intens untuk sensor bawah layar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
                Switch(
                    checked = amoledFodAssist,
                    onCheckedChange = { amoledFodAssist = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0F172A),
                        checkedTrackColor = Color(0xFF00FF88),
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = Color(0x1AFFFFFF)
                    ),
                    modifier = Modifier.testTag("amoled_fod_switch")
                )
            }
        }

        // Main Scanning Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                1.dp,
                if (isScanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Biometric Scanner Pad inside an ambient glow overlay
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    if (amoledFodAssist) Color(0x4000FF88) else Color(0x3B6366F1), // Glowing aura
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ScannerPadWidget(
                        isScanning = isScanning,
                        progress = scanProgress,
                        success = scanSuccess,
                        amoledFodAssist = amoledFodAssist,
                        onStartScan = {
                            if (selectedFingerprint == null) {
                                viewModel.setScanMessage("Pilih profil sidik jari terlebih dahulu!")
                            } else {
                                val isAvailable = BiometricPromptUtils.isBiometricAvailable(currentContext)
                                if (isAvailable == "SUCCESS" && currentContext is androidx.fragment.app.FragmentActivity) {
                                    BiometricPromptUtils.showBiometricPrompt(
                                        activity = currentContext,
                                        title = "Absensi Sidik Jari Secure",
                                        subtitle = "Verifikasi Sidik Jari untuk ${selectedFingerprint?.name}",
                                        description = "Letakkan jari Anda pada sensor biometrik perangkat untuk memverifikasi dan mengirim absensi terenkripsi.",
                                        onSuccess = {
                                            viewModel.startAttendanceScan()
                                        },
                                        onError = { errorString ->
                                            viewModel.setScanMessage("Gagal Biometrik Perangkat: $errorString")
                                        }
                                    )
                                } else {
                                    // Fallback to secure emulation/simulation mode
                                    viewModel.startAttendanceScan()
                                }
                            }
                        },
                        onCancelScan = { viewModel.cancelScanning() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Information Card
                Text(
                    text = scanMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (scanSuccess) {
                        true -> Color(0xFF4CAF50)
                        false -> MaterialTheme.colorScheme.error
                        null -> if (isScanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = isScanning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Memproses: ${(scanProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Security notice inside the reader
                AnimatedVisibility(visible = scanSuccess == true && selectedFingerprint != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(
                                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF388E3C),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "DATA TERENKRIPSI AES-256 & DI-SIGN HMAC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF388E3C),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerPadWidget(
    isScanning: Boolean,
    progress: Float,
    success: Boolean?,
    amoledFodAssist: Boolean = true,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    val padColor by animateColorAsState(
        targetValue = when (success) {
            true -> Color(0xFF4CAF50)
            false -> MaterialTheme.colorScheme.error
            null -> if (isScanning) {
                if (amoledFodAssist) Color(0xFF00FF88) else MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
        },
        animationSpec = tween(300),
        label = "padColor"
    )

    val glowOpacity by animateFloatAsState(
        targetValue = if (isScanning) 0.6f else 0.15f,
        animationSpec = tween(500),
        label = "glowOpacity"
    )

    Box(
        modifier = Modifier
            .size(180.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onStartScan()
                        tryAwaitRelease()
                        onCancelScan()
                    }
                )
            }
            .testTag("fingerprint_scanner_pad"),
        contentAlignment = Alignment.Center
    ) {
        // Outer Pulsing Radiating Rings (drawn only when active scanning)
        if (isScanning) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = padColor.copy(alpha = 0.15f * (1f - progress)),
                    radius = size.minDimension / 2f * pulseScale,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = padColor.copy(alpha = 0.25f * (1f - progress)),
                    radius = size.minDimension / 2.3f * pulseScale,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Circular background container
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    if (isScanning && amoledFodAssist && success == null) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00FF88).copy(alpha = 0.5f),
                                Color.Black
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                padColor.copy(alpha = 0.35f),
                                padColor.copy(alpha = 0.05f)
                            )
                        )
                    }
                )
                .border(2.dp, padColor.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Scanner Canvas Drawing (Fingerprint pattern + grid + moving scan bar)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background futuristic grid lines
                val gridCount = 6
                val stepX = size.width / gridCount
                val stepY = size.height / gridCount
                for (i in 1 until gridCount) {
                    drawLine(
                        color = padColor.copy(alpha = 0.08f),
                        start = Offset(i * stepX, 0f),
                        end = Offset(i * stepX, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = padColor.copy(alpha = 0.08f),
                        start = Offset(0f, i * stepY),
                        end = Offset(size.width, i * stepY),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // If scanning, draw scanning horizontal line sliding down
                if (isScanning) {
                    val currentY = size.height * scanLineY
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                padColor.copy(alpha = 0.8f),
                                padColor,
                                padColor.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(0f, currentY),
                        end = Offset(size.width, currentY),
                        strokeWidth = 4.dp.toPx()
                    )
                }
            }

            // Fingerprint Icon Overlay
            Icon(
                imageVector = when (success) {
                    true -> Icons.Default.CheckCircle
                    false -> Icons.Default.Warning
                    null -> Icons.Default.Fingerprint
                },
                contentDescription = "Scanner Icon",
                modifier = Modifier.size(76.dp),
                tint = padColor
            )
        }

        // Instruction badge at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 12.dp)
                .background(
                    color = padColor,
                    shape = RoundedCornerShape(20.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isScanning) "MEMINDAI..." else if (success == true) "BERHASIL" else if (success == false) "GAGAL" else "TEKAN & TAHAN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (success == null && !isScanning) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                fontSize = 11.sp
            )
        }
    }
}

// Simple helper to avoid import issue for border stroke
@Composable
fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
