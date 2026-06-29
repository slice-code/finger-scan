package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface AttendanceApiService {

    @POST
    suspend fun sendAttendance(
        @Url url: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("X-Signature") signature: String,
        @Header("X-Encryption-Key") encryptionKeyId: String = "AES-256",
        @Body body: RequestBody
    ): Response<ResponseBody>

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

            // Retrofit requires a placeholder base URL even when using dynamic @Url
            return Retrofit.Builder()
                .baseUrl("https://placeholder.api/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(AttendanceApiService::class.java)
        }
    }
}
