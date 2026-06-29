package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AttendanceLogEntity
import com.example.data.local.FingerprintEntity
import com.example.data.repository.AttendanceRepository
import com.example.security.BiometricEncryptionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AttendanceRepository) : ViewModel() {

    // --- Data Streams ---
    val allFingerprints: StateFlow<List<FingerprintEntity>> = repository.allFingerprints
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLogs: StateFlow<List<AttendanceLogEntity>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Config / Settings ---
    private val _apiUrl = MutableStateFlow("https://api.absensi-online.com/v1/attendance")
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()

    private val _secretKey = MutableStateFlow("AbsensiSecureSecret2026!")
    val secretKey: StateFlow<String> = _secretKey.asStateFlow()

    private val _simulateOffline = MutableStateFlow(false)
    val simulateOffline: StateFlow<Boolean> = _simulateOffline.asStateFlow()

    fun updateSettings(url: String, key: String, offline: Boolean) {
        _apiUrl.value = url.trim()
        _secretKey.value = key
        _simulateOffline.value = offline
    }

    // --- Fingerprint Registration State ---
    private val _regEmployeeId = MutableStateFlow("")
    val regEmployeeId: StateFlow<String> = _regEmployeeId.asStateFlow()

    private val _regName = MutableStateFlow("")
    val regName: StateFlow<String> = _regName.asStateFlow()

    private val _regDepartment = MutableStateFlow("")
    val regDepartment: StateFlow<String> = _regDepartment.asStateFlow()

    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering.asStateFlow()

    private val _registerProgress = MutableStateFlow(0f)
    val registerProgress: StateFlow<Float> = _registerProgress.asStateFlow()

    private val _registrationMessage = MutableStateFlow("")
    val registrationMessage: StateFlow<String> = _registrationMessage.asStateFlow()

    fun onRegEmployeeIdChange(value: String) { _regEmployeeId.value = value }
    fun onRegNameChange(value: String) { _regName.value = value }
    fun onRegDepartmentChange(value: String) { _regDepartment.value = value }

    private var registrationJob: Job? = null

    fun startFingerprintRegistration() {
        if (_regEmployeeId.value.isBlank() || _regName.value.isBlank() || _regDepartment.value.isBlank()) {
            _registrationMessage.value = "Mohon lengkapi semua kolom input!"
            return
        }

        registrationJob?.cancel()
        registrationJob = viewModelScope.launch {
            _isRegistering.value = true
            _registerProgress.value = 0f
            _registrationMessage.value = "Silakan letakkan sidik jari Anda pada sensor..."

            // Simulate enrollment steps
            for (i in 1..100) {
                delay(30) // total 3 seconds
                _registerProgress.value = i / 100f
                when (i) {
                    20 -> _registrationMessage.value = "Memindai pola guratan sidik jari..."
                    50 -> _registrationMessage.value = "Memetakan titik-titik koordinat minutiae..."
                    80 -> _registrationMessage.value = "Mengenkripsi data biometrik..."
                }
            }

            val dynamicSeed = System.nanoTime().toString()
            val generatedHash = BiometricEncryptionUtils.generateFingerprintHash(
                name = _regName.value.trim(),
                employeeId = _regEmployeeId.value.trim(),
                seed = dynamicSeed
            )

            // Save in database
            repository.registerFingerprint(
                employeeId = _regEmployeeId.value.trim(),
                name = _regName.value.trim(),
                department = _regDepartment.value.trim(),
                fingerprintHash = generatedHash
            )

            _registrationMessage.value = "Pendaftaran Sidik Jari Berhasil!"
            _isRegistering.value = false

            // Clear registration inputs
            _regEmployeeId.value = ""
            _regName.value = ""
            _regDepartment.value = ""
        }
    }

    fun cancelRegistration() {
        registrationJob?.cancel()
        _isRegistering.value = false
        _registerProgress.value = 0f
        _registrationMessage.value = "Pendaftaran dibatalkan."
    }

    // --- Attendance Scanning (Check-In) State ---
    private val _selectedFingerprint = MutableStateFlow<FingerprintEntity?>(null)
    val selectedFingerprint: StateFlow<FingerprintEntity?> = _selectedFingerprint.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scanMessage = MutableStateFlow("Pilih sidik jari terdaftar untuk memulai absensi")
    val scanMessage: StateFlow<String> = _scanMessage.asStateFlow()

    private val _scanSuccess = MutableStateFlow<Boolean?>(null) // true, false, null (idle)
    val scanSuccess: StateFlow<Boolean?> = _scanSuccess.asStateFlow()

    fun selectFingerprint(fingerprint: FingerprintEntity?) {
        _selectedFingerprint.value = fingerprint
        if (fingerprint != null) {
            _scanMessage.value = "Letakkan jari ${fingerprint.name} di sensor untuk melakukan absensi"
            _scanSuccess.value = null
        } else {
            _scanMessage.value = "Pilih sidik jari terdaftar untuk memulai absensi"
        }
    }

    private var scanningJob: Job? = null

    fun startAttendanceScan() {
        val f = _selectedFingerprint.value
        if (f == null) {
            _scanMessage.value = "Pilih profil sidik jari terlebih dahulu!"
            return
        }

        scanningJob?.cancel()
        scanningJob = viewModelScope.launch {
            _isScanning.value = true
            _scanSuccess.value = null
            _scanProgress.value = 0f
            _scanMessage.value = "Memindai sidik jari..."

            // Simulate fingerprint scanning
            for (i in 1..100) {
                delay(25) // total 2.5 seconds
                _scanProgress.value = i / 100f
                when (i) {
                    20 -> _scanMessage.value = "Mencocokkan sidik jari dengan database..."
                    50 -> _scanMessage.value = "Sidik jari cocok: ${f.name} (${f.employeeId})"
                    80 -> _scanMessage.value = "Mengenkripsi payload data absensi..."
                }
            }

            _scanMessage.value = "Mengirim data absensi terenkripsi..."

            // Submit attendance log
            val logResult = repository.submitAttendance(
                employeeId = f.employeeId,
                name = f.name,
                department = f.department,
                fingerprintHash = f.fingerprintHash,
                apiUrl = _apiUrl.value,
                secretKey = _secretKey.value,
                simulateOffline = _simulateOffline.value
            )

            if (logResult.status == "SUCCESS") {
                _scanSuccess.value = true
                _scanMessage.value = "Presensi Berhasil Dikirim secara Real-time!"
            } else {
                _scanSuccess.value = false
                _scanMessage.value = "Gagal kirim real-time: ${logResult.errorMessage ?: "Koneksi terputus"}"
            }

            _isScanning.value = false
        }
    }

    fun cancelScanning() {
        scanningJob?.cancel()
        _isScanning.value = false
        _scanProgress.value = 0f
        _scanSuccess.value = null
        if (_selectedFingerprint.value != null) {
            _scanMessage.value = "Letakkan jari ${_selectedFingerprint.value?.name} di sensor untuk melakukan absensi"
        } else {
            _scanMessage.value = "Pilih sidik jari terdaftar untuk memulai absensi"
        }
    }

    fun setScanMessage(message: String) {
        _scanMessage.value = message
    }

    fun deleteFingerprint(f: FingerprintEntity) {
        viewModelScope.launch {
            if (_selectedFingerprint.value?.id == f.id) {
                selectFingerprint(null)
            }
            repository.deleteFingerprint(f.id)
        }
    }

    fun retrySync(log: AttendanceLogEntity) {
        viewModelScope.launch {
            repository.retrySyncLog(log, _apiUrl.value, _secretKey.value)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // --- Provider Factory ---
    class Factory(private val repository: AttendanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
