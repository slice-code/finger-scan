# Absensi Finger Reader

Aplikasi absensi berbasis sidik jari biometrik (simulasi) dengan enkripsi AES-256 dan integrasi server-side matching.

Dibangun dengan Android Jetpack Compose, memanfaatkan `BiometricPrompt` Android untuk autentikasi perangkat, dan mengirim data terenkripsi ke server untuk pencocokan sidik jari.

## Fitur

- **Scan** — verifikasi biometrik perangkat → hash sidik jari dikirim ke server → server mencocokkan & mencatat absensi
- **Registrasi** — daftar karyawan dari API → pilih → verifikasi biometrik → hash terdaftar di server
- **Log** — riwayat absensi dengan payload terdekripsi (AES) dan signature HMAC
- **Settings** — konfigurasi URL API, secret key AES/HMAC, mode offline
- **AMOLED FOD Assist** — visual aid untuk sensor sidik jari bawah layar

## Tech stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin 2.2.10 |
| UI | Jetpack Compose (Material3) |
| Database lokal | Room (cache) |
| Network | Retrofit + OkHttp + Moshi |
| Biometrik | AndroidX BiometricPrompt |
| Keamanan | AES-256/ECB/PKCS5Padding, HMAC-SHA256 |
| Build | AGP 9.1.1, Gradle 9.3.1 |
| Codegen | KSP (Room + Moshi) |

## Prasyarat

- Android Studio (recommended) atau Gradle CLI
- Android SDK 36
- Java 17+ (JBR di Android Studio)
- Device Android dengan biometric sensor (opsional — fallback mode tersedia)
- **Server API** — lihat [API_SERVER.md](API_SERVER.md)

## Build & Install

```bash
# Copy environment
cp .env.example .env   # isi GEMINI_API_KEY

# Untuk debug build lokal, hapus baris ini dari app/build.gradle.kts:
#   signingConfig = signingConfigs.getByName("debugConfig")

# Build
./gradlew assembleDebug

# Install ke device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Arsitektur

```
MainActivity
 └─ MainAppScreen() — 4 tab (Scan, Users, Logs, Admin)
     ├─ ScanScreen     → BiometricPrompt → hash → POST /attendance
     ├─ RegisterScreen → GET /employees → pilih → BiometricPrompt → POST /register
     ├─ HistoryScreen  → lihat log lokal (Room)
     └─ SettingsScreen → konfigurasi API URL & secret key

AttendanceRepository
 ├─ fetchEmployees()   → GET  {base}/employees
 ├─ registerFingerprint() → POST {base}/register   (AES encrypted)
 └─ verifyAndAttend()  → POST {attendance_url}     (AES encrypted)
```

Data sidik jari **tidak disimpan/matching di lokal**. Semua pencocokan dilakukan di server. Room DB lokal hanya sebagai cache untuk riwayat log dan daftar fingerprint terdaftar.

## API Server

Dokumentasi lengkap endpoint, enkripsi/dekripsi, skema PostgreSQL, dan contoh implementasi (Node.js, Python):

➡️ [API_SERVER.md](API_SERVER.md)

## Test

```bash
# Unit test + Roborazzi screenshot
./gradlew test

# Instrumented test
./gradlew connectedCheck
```

## Test tags

`bottom_nav_bar`, `nav_tab_scan`, `nav_tab_register`, `nav_tab_history`, `nav_tab_settings`

## Lisensi

Proyek ini dibuat dengan Google AI Studio. Lihat [metadata.json](metadata.json).
