package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fingerprints")
data class FingerprintEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val name: String,
    val fingerprintHash: String,
    val registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_logs")
data class AttendanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val encryptedPayload: String,
    val signature: String,
    val apiUrl: String,
    val status: String,
    val errorMessage: String? = null
)
