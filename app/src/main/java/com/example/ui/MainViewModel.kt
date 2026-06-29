package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AttendanceLogEntity
import com.example.data.local.FingerprintEntity
import com.example.data.remote.ApiEmployee
import com.example.data.repository.AttendanceRepository
import com.example.security.BiometricEncryptionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AttendanceRepository) : ViewModel() {

    val allFingerprints: StateFlow<List<FingerprintEntity>> = repository.allFingerprints
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val allLogs: StateFlow<List<AttendanceLogEntity>> = repository.allLogs
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    // --- Settings ---
    private val _apiBaseUrl = MutableStateFlow("https://api.absensi-online.com/v1")
    val apiBaseUrl: StateFlow<String> = _apiBaseUrl.asStateFlow()

    private val _apiUrl = MutableStateFlow("https://api.absensi-online.com/v1/attendance")
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()

    private val _secretKey = MutableStateFlow("AbsensiSecureSecret2026!")
    val secretKey: StateFlow<String> = _secretKey.asStateFlow()

    private val _simulateOffline = MutableStateFlow(false)
    val simulateOffline: StateFlow<Boolean> = _simulateOffline.asStateFlow()

    fun updateSettings(baseUrl: String, url: String, key: String, offline: Boolean) {
        _apiBaseUrl.value = baseUrl.trim().trimEnd('/')
        _apiUrl.value = url.trim()
        _secretKey.value = key
        _simulateOffline.value = offline
    }

    // --- Employee list from API ---
    private val _employees = MutableStateFlow<List<ApiEmployee>>(emptyList())
    val employees: StateFlow<List<ApiEmployee>> = _employees.asStateFlow()

    private val _isLoadingEmployees = MutableStateFlow(false)
    val isLoadingEmployees: StateFlow<Boolean> = _isLoadingEmployees.asStateFlow()

    private val _employeeError = MutableStateFlow<String?>(null)
    val employeeError: StateFlow<String?> = _employeeError.asStateFlow()

    fun fetchEmployees() {
        viewModelScope.launch {
            _isLoadingEmployees.value = true
            _employeeError.value = null
            try {
                val response = repository.fetchEmployees("${_apiBaseUrl.value}/employees")
                if (response.isSuccessful) {
                    _employees.value = response.body() ?: emptyList()
                } else {
                    _employeeError.value = "Gagal memuat data: ${response.code()} ${response.message()}"
                }
            } catch (e: Exception) {
                _employeeError.value = "Koneksi error: ${e.localizedMessage ?: "Unknown"}"
            }
            _isLoadingEmployees.value = false
        }
    }

    // --- Registration ---
    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering.asStateFlow()

    private val _registerProgress = MutableStateFlow(0f)
    val registerProgress: StateFlow<Float> = _registerProgress.asStateFlow()

    private val _registrationMessage = MutableStateFlow("")
    val registrationMessage: StateFlow<String> = _registrationMessage.asStateFlow()

    private var registrationJob: Job? = null

    fun registerFingerprint(employee: ApiEmployee) {
        registrationJob?.cancel()
        registrationJob = viewModelScope.launch {
            _isRegistering.value = true
            _registerProgress.value = 0f
            _registrationMessage.value = "Memproses pendaftaran ${employee.nama}..."

            _registerProgress.value = 0.5f

            val generatedHash = BiometricEncryptionUtils.generateFingerprintHash(employee.id)

            val result = repository.registerFingerprint(
                employeeId = employee.id,
                name = employee.nama,
                fingerprintHash = generatedHash,
                apiBaseUrl = _apiBaseUrl.value,
                secretKey = _secretKey.value
            )

            _registerProgress.value = 1f
            if (result.isSuccess) {
                _registrationMessage.value = "Pendaftaran ${employee.nama} Berhasil!"
            } else {
                _registrationMessage.value = "Gagal: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}"
            }
            _isRegistering.value = false
        }
    }

    fun setRegistrationMessage(msg: String) {
        _registrationMessage.value = msg
    }

    // --- Attendance ---
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scanMessage = MutableStateFlow("Tekan tombol sidik jari untuk absensi")
    val scanMessage: StateFlow<String> = _scanMessage.asStateFlow()

    private val _scanSuccess = MutableStateFlow<Boolean?>(null)
    val scanSuccess: StateFlow<Boolean?> = _scanSuccess.asStateFlow()

    private var scanningJob: Job? = null

    fun startAttendanceScan(fingerprintHash: String) {
        scanningJob?.cancel()
        scanningJob = viewModelScope.launch {
            _isScanning.value = true
            _scanSuccess.value = null
            _scanProgress.value = 0f
            _scanMessage.value = "Mencocokkan sidik jari dengan server..."

            _scanProgress.value = 0.5f

            val result = repository.verifyAndAttend(
                fingerprintHash = fingerprintHash,
                apiUrl = _apiUrl.value,
                secretKey = _secretKey.value
            )

            _scanProgress.value = 1f
            if (result.isSuccess) {
                val data = result.getOrNull()
                _scanSuccess.value = true
                _scanMessage.value = "Absensi ${data?.name ?: ""} Berhasil!"
            } else {
                _scanSuccess.value = false
                _scanMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Gagal"
            }
            _isScanning.value = false
        }
    }

    fun cancelScanning() {
        scanningJob?.cancel()
        _isScanning.value = false
        _scanProgress.value = 0f
        _scanSuccess.value = null
        _scanMessage.value = "Tekan tombol sidik jari untuk absensi"
    }

    fun setScanMessage(message: String) {
        _scanMessage.value = message
    }

    fun deleteFingerprint(id: Int) {
        viewModelScope.launch { repository.deleteFingerprint(id) }
    }

    fun retrySync(log: AttendanceLogEntity) {
        viewModelScope.launch {
            repository.retrySyncLog(log, _apiUrl.value, _secretKey.value)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch { repository.clearLogs() }
    }

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
