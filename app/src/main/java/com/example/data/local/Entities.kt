package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fingerprints")
data class FingerprintEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val name: String,
    val department: String,
    val fingerprintHash: String, // Simulated fingerprint biometric signature
    val registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_logs")
data class AttendanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val name: String,
    val department: String,
    val timestamp: Long = System.currentTimeMillis(),
    val encryptedPayload: String, // Simulated/real AES encrypted payload
    val signature: String, // HMAC-SHA256 signature of the payload
    val apiUrl: String, // Target API URL configured at the time
    val status: String, // SUCCESS, FAILED, PENDING
    val errorMessage: String? = null
)
