package com.example.data.repository

import com.example.data.local.AttendanceLogDao
import com.example.data.local.AttendanceLogEntity
import com.example.data.local.FingerprintDao
import com.example.data.local.FingerprintEntity
import com.example.data.remote.ApiEmployee
import com.example.data.remote.AttendanceApiService
import com.example.data.remote.VerifyResponse
import com.example.security.BiometricEncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

class AttendanceRepository(
    private val fingerprintDao: FingerprintDao,
    private val attendanceLogDao: AttendanceLogDao,
    private val apiService: AttendanceApiService
) {
    val allFingerprints: Flow<List<FingerprintEntity>> = fingerprintDao.getAllFingerprints()
    val allLogs: Flow<List<AttendanceLogEntity>> = attendanceLogDao.getAllLogs()

    suspend fun fetchEmployees(url: String): Response<List<ApiEmployee>> =
        apiService.getEmployees(url)

    suspend fun registerFingerprint(
        employeeId: String,
        name: String,
        fingerprintHash: String,
        apiBaseUrl: String,
        secretKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val payloadJson = """{"employeeId":"$employeeId","fingerprintHash":"$fingerprintHash"}"""
            val encryptedPayload = BiometricEncryptionUtils.encryptAES(payloadJson, secretKey)
            val signature = BiometricEncryptionUtils.calculateHmac(encryptedPayload, secretKey)
            val requestBody = """{"encryptedPayload":"$encryptedPayload","signature":"$signature","timestamp":$timestamp}"""
                .toRequestBody("application/json".toMediaType())

            val response = apiService.registerFingerprint(
                url = "$apiBaseUrl/register",
                signature = signature,
                body = requestBody
            )

            if (response.isSuccessful) {
                val localEntity = FingerprintEntity(
                    employeeId = employeeId,
                    name = name,
                    fingerprintHash = fingerprintHash
                )
                fingerprintDao.insertFingerprint(localEntity)
                Result.success(response.body()?.message ?: "OK")
            } else {
                Result.failure(Exception("Server error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyAndAttend(
        fingerprintHash: String,
        apiUrl: String,
        secretKey: String
    ): Result<VerifyResponse> = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val payloadJson = """{"fingerprintHash":"$fingerprintHash"}"""
            val encryptedPayload = BiometricEncryptionUtils.encryptAES(payloadJson, secretKey)
            val signature = BiometricEncryptionUtils.calculateHmac(encryptedPayload, secretKey)
            val requestBody = """{"encryptedPayload":"$encryptedPayload","signature":"$signature","timestamp":$timestamp}"""
                .toRequestBody("application/json".toMediaType())

            val response = apiService.verifyAttendance(
                url = apiUrl,
                signature = signature,
                body = requestBody
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.status == "matched") {
                    val log = AttendanceLogEntity(
                        employeeId = body.employeeId ?: "",
                        name = body.name ?: "",
                        timestamp = timestamp,
                        encryptedPayload = encryptedPayload,
                        signature = signature,
                        apiUrl = apiUrl,
                        status = "SUCCESS"
                    )
                    attendanceLogDao.insertLog(log)
                    Result.success(body)
                } else {
                    Result.failure(Exception(body?.message ?: "Sidik jari tidak dikenal"))
                }
            } else {
                Result.failure(Exception("Server error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFingerprint(id: Int) = withContext(Dispatchers.IO) {
        fingerprintDao.deleteFingerprintById(id)
    }

    suspend fun retrySyncLog(log: AttendanceLogEntity, apiUrl: String, secretKey: String): AttendanceLogEntity =
        withContext(Dispatchers.IO) {
            attendanceLogDao.updateLogStatus(log.id, "PENDING", null)
            try {
                val requestBodyJson =
                    """{"encryptedPayload":"${log.encryptedPayload}","signature":"${log.signature}","timestamp":${log.timestamp}}"""
                val requestBody = requestBodyJson.toRequestBody("application/json".toMediaType())
                val response = apiService.verifyAttendance(
                    url = apiUrl,
                    signature = log.signature,
                    body = requestBody
                )
                if (response.isSuccessful) {
                    attendanceLogDao.updateLogStatus(log.id, "SUCCESS", null)
                    log.copy(status = "SUCCESS", errorMessage = null)
                } else {
                    val errorMsg = "API Error: ${response.code()} ${response.message()}"
                    attendanceLogDao.updateLogStatus(log.id, "FAILED", errorMsg)
                    log.copy(status = "FAILED", errorMessage = errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Retry Connection Error: ${e.localizedMessage ?: "Unknown"}"
                attendanceLogDao.updateLogStatus(log.id, "FAILED", errorMsg)
                log.copy(status = "FAILED", errorMessage = errorMsg)
            }
        }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        attendanceLogDao.clearAllLogs()
    }
}
