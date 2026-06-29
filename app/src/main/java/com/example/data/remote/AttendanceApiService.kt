package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

data class ApiEmployee(
    val id: String,
    val nama: String
)

data class VerifyResponse(
    val status: String,
    val employeeId: String? = null,
    val name: String? = null,
    val message: String? = null
)

interface AttendanceApiService {

    @GET
    suspend fun getEmployees(
        @Url url: String
    ): Response<List<ApiEmployee>>

    @POST
    suspend fun registerFingerprint(
        @Url url: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("X-Signature") signature: String,
        @Header("X-Encryption-Key") encryptionKeyId: String = "AES-256",
        @Body body: RequestBody
    ): Response<VerifyResponse>

    @POST
    suspend fun verifyAttendance(
        @Url url: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("X-Signature") signature: String,
        @Header("X-Encryption-Key") encryptionKeyId: String = "AES-256",
        @Body body: RequestBody
    ): Response<VerifyResponse>

    companion object {
        fun create(): AttendanceApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://placeholder.api/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(AttendanceApiService::class.java)
        }
    }
}
