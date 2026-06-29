package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ApiEmployee
import com.example.security.BiometricPromptUtils
import com.example.ui.MainViewModel

@Composable
fun RegisterScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsState()
    val isLoading by viewModel.isLoadingEmployees.collectAsState()
    val employeeError by viewModel.employeeError.collectAsState()
    val isRegistering by viewModel.isRegistering.collectAsState()
    val registerProgress by viewModel.registerProgress.collectAsState()
    val registrationMessage by viewModel.registrationMessage.collectAsState()

    LaunchedEffect(Unit) { viewModel.fetchEmployees() }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "REGISTRASI SIDIK JARI BARU",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pilih karyawan dari daftar untuk mendaftarkan sidik jari biometrik.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Memuat data karyawan...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else if (employeeError != null) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(employeeError!!, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.fetchEmployees() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Coba Lagi")
                        }
                    }
                }
            }
        } else if (employees.isEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tidak ada data karyawan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Pastikan server API aktif dan URL sudah benar di Settings.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(employees, key = { it.id }) { employee ->
                EmployeeCard(
                    employee = employee,
                    enabled = !isRegistering,
                    onClick = {
                        val bioStatus = BiometricPromptUtils.isBiometricAvailable(context)
                        if (bioStatus == "SUCCESS" && context is androidx.fragment.app.FragmentActivity) {
                            BiometricPromptUtils.showBiometricPrompt(
                                activity = context,
                                title = "Daftarkan Sidik Jari",
                                subtitle = "Verifikasi untuk ${employee.nama}",
                                description = "Letakkan jari pada sensor biometrik untuk mendaftarkan ${employee.nama}.",
                                onSuccess = { viewModel.registerFingerprint(employee) },
                                onError = { }
                            )
                        } else {
                            viewModel.registerFingerprint(employee)
                        }
                    }
                )
            }
        }

        if (isRegistering) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(registrationMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { registerProgress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
                    }
                }
            }
        }

        if (registrationMessage.isNotEmpty() && !isRegistering) {
            item {
                Text(
                    text = registrationMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (registrationMessage.contains("Berhasil")) Color(0xFF388E3C) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EmployeeCard(employee: ApiEmployee, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .testTag("employee_${employee.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(employee.nama, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("ID: ${employee.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.Fingerprint, contentDescription = "Daftarkan", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
    }
}
