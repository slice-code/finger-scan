package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FingerprintDao {
    @Query("SELECT * FROM fingerprints ORDER BY name ASC")
    fun getAllFingerprints(): Flow<List<FingerprintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFingerprint(fingerprint: FingerprintEntity): Long

    @Query("DELETE FROM fingerprints WHERE id = :id")
    suspend fun deleteFingerprintById(id: Int)

    @Query("SELECT COUNT(*) FROM fingerprints")
    suspend fun getFingerprintCount(): Int
}

@Dao
interface AttendanceLogDao {
    @Query("SELECT * FROM attendance_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AttendanceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AttendanceLogEntity): Long

    @Query("UPDATE attendance_logs SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateLogStatus(id: Int, status: String, errorMessage: String?)

    @Query("DELETE FROM attendance_logs")
    suspend fun clearAllLogs()
}
