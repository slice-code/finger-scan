package com.example.data.repository

import com.example.data.local.AttendanceLogDao
import com.example.data.local.AttendanceLogEntity
import com.example.data.local.FingerprintDao
import com.example.data.local.FingerprintEntity
import com.example.data.remote.AttendanceApiService
import com.example.security.BiometricEncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AttendanceRepository(
    private val fingerprintDao: FingerprintDao,
    private val attendanceLogDao: AttendanceLogDao,
    private val apiService: AttendanceApiService
) {
    val allFingerprints: Flow<List<FingerprintEntity>> = fingerprintDao.getAllFingerprints()
    val allLogs: Flow<List<AttendanceLogEntity>> = attendanceLogDao.getAllLogs()

    suspend fun registerFingerprint(
        employeeId: String,
        name: String,
        department: String,
        fingerprintHash: String
    ): Long = withContext(Dispatchers.IO) {
        val entity = FingerprintEntity(
            employeeId = employeeId,
            name = name,
            department = department,
            fingerprintHash = fingerprintHash
        )
        fingerprintDao.insertFingerprint(entity)
    }

    suspend fun deleteFingerprint(id: Int) = withContext(Dispatchers.IO) {
        fingerprintDao.deleteFingerprintById(id)
    }

    suspend fun submitAttendance(
        employeeId: String,
        name: String,
        department: String,
        fingerprintHash: String,
        apiUrl: String,
        secretKey: String,
        simulateOffline: Boolean
    ): AttendanceLogEntity = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()

        // 1. Formulate Raw Payload
        val payloadJson = """{"employeeId":"$employeeId","name":"$name","department":"$department","fingerprintHash":"$fingerprintHash","timestamp":$timestamp}"""

        // 2. Encrypt and Sign
        val encryptedPayload = BiometricEncryptionUtils.encryptAES(payloadJson, secretKey)
        val signature = BiometricEncryptionUtils.calculateHmac(encryptedPayload, secretKey)

        // 3. Create Log Entity in Room (Initial state: PENDING)
        val initialLog = AttendanceLogEntity(
            employeeId = employeeId,
            name = name,
            department = department,
            timestamp = timestamp,
            encryptedPayload = encryptedPayload,
            signature = signature,
            apiUrl = apiUrl,
            status = "PENDING"
        )
        val logId = attendanceLogDao.insertLog(initialLog).toInt()
        val savedLog = initialLog.copy(id = logId)

        // 4. Send or Fail
        if (simulateOffline) {
            val errorMsg = "Simulated Offline Mode"
            attendanceLogDao.updateLogStatus(logId, "FAILED", errorMsg)
            return@withContext savedLog.copy(status = "FAILED", errorMessage = errorMsg)
        }

        try {
            // Build request JSON
            val requestBodyJson = """{"encryptedPayload":"$encryptedPayload","signature":"$signature","timestamp":$timestamp}"""
            val requestBody = requestBodyJson.toRequestBody("application/json".toMediaType())

            val response = apiService.sendAttendance(
                url = apiUrl,
                signature = signature,
                body = requestBody
            )

            if (response.isSuccessful) {
                attendanceLogDao.updateLogStatus(logId, "SUCCESS", null)
                return@withContext savedLog.copy(status = "SUCCESS")
            } else {
                val errorMsg = "API Error: ${response.code()} ${response.message()}"
                attendanceLogDao.updateLogStatus(logId, "FAILED", errorMsg)
                return@withContext savedLog.copy(status = "FAILED", errorMessage = errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Connection Error: ${e.localizedMessage ?: "Unknown Error"}"
            attendanceLogDao.updateLogStatus(logId, "FAILED", errorMsg)
            return@withContext savedLog.copy(status = "FAILED", errorMessage = errorMsg)
        }
    }

    suspend fun retrySyncLog(
        log: AttendanceLogEntity,
        apiUrl: String,
        secretKey: String
    ): AttendanceLogEntity = withContext(Dispatchers.IO) {
        attendanceLogDao.updateLogStatus(log.id, "PENDING", null)

        try {
            val requestBodyJson = """{"encryptedPayload":"${log.encryptedPayload}","signature":"${log.signature}","timestamp":${log.timestamp}}"""
            val requestBody = requestBodyJson.toRequestBody("application/json".toMediaType())

            val response = apiService.sendAttendance(
                url = apiUrl,
                signature = log.signature,
                body = requestBody
            )

            if (response.isSuccessful) {
                attendanceLogDao.updateLogStatus(log.id, "SUCCESS", null)
                return@withContext log.copy(status = "SUCCESS", errorMessage = null)
            } else {
                val errorMsg = "API Error: ${response.code()} ${response.message()}"
                attendanceLogDao.updateLogStatus(log.id, "FAILED", errorMsg)
                return@withContext log.copy(status = "FAILED", errorMessage = errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Retry Connection Error: ${e.localizedMessage ?: "Unknown Error"}"
            attendanceLogDao.updateLogStatus(log.id, "FAILED", errorMsg)
            return@withContext log.copy(status = "FAILED", errorMessage = errorMsg)
        }
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        attendanceLogDao.clearAllLogs()
    }
}
