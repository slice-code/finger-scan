# Absensi Finger Reader — Android (AI Studio)

## Quick start

```bash
# Copy env before building
cp .env.example .env   # set GEMINI_API_KEY inside
# For local debug builds, remove line from app/build.gradle.kts:
#   signingConfig = signingConfigs.getByName("debugConfig")
# Then build & run from Android Studio, or:
./gradlew assembleDebug
```

## Project structure

- Single-module (`:app`), Kotlin 2.2.10, AGP 9.1.1, Compose BOM 2024.09
- Jetpack Compose (Material3) + Room + Retrofit + Moshi + OkHttp
- KSP for Room and Moshi codegen
- Firebase AI + App Check (google-services config missing — passthrough enabled)
- Roborazzi + Robolectric for screenshot tests
- Secrets loaded from `.env` via `com.google.android.libraries.mapsplatform.secrets-gradle-plugin`

## Architecture

- `MainActivity` → `MainAppScreen()` — 4-tab bottom nav (Scan, Users, Logs, Admin)
- `MainViewModel` with custom `Factory(repository)`
- `AttendanceRepository` wraps `FingerprintDao` + `AttendanceLogDao` + `AttendanceApiService`
- Room DB: `absensi_fingerprint_db`, tables `fingerprints` / `attendance_logs`
  - Uses `fallbackToDestructiveMigration()` — schema changes lose data
- Retrofit API uses dynamic `@Url` (placeholder base URL `https://placeholder.api/`)
- Security: SHA-256 hash for simulated fingerprint, AES-256/ECB/PKCS5Padding for payload, HMAC-SHA256 signatures (in `BiometricEncryptionUtils`)
- **Biometric sensor**: `BiometricPrompt` (AndroidX) used for device-level fingerprint auth on Scan + Register screens. Falls back to simulation mode if no hardware detected. `BiometricPromptUtils` checks availability via `BiometricManager`.
- **AMOLED FOD Assist**: Visual toggle in Scan screen that switches scanner glow to bright green (#00FF88) to help locate under-display sensor — no driver interaction.

## Tests

```bash
# Unit + Robolectric + Roborazzi screenshot tests
./gradlew test

# Instrumented (connected device / emulator)
./gradlew connectedCheck
```

- Screenshot outputs go to `app/src/test/screenshots/`
- `testTag` conventions: `bottom_nav_bar`, `nav_tab_scan`, `nav_tab_register`, `nav_tab_history`, `nav_tab_settings`

## Release signing

Signing config reads from env vars: `KEYSTORE_PATH` (default `<root>/my-upload-key.jks`), `STORE_PASSWORD`, `KEY_PASSWORD`. Debug keystore at `<root>/debug.keystore` with well-known creds.

## Key conventions

- Keep unused dependencies commented out (not deleted) — see `app/build.gradle.kts`
- Room + Moshi codegen via KSP (`ksp(...)` syntax)
- `googleServices.missing.passthrough=true` in `gradle.properties` — no `google-services.json` needed
- Config uses Gradle version catalog (`gradle/libs.versions.toml`)
- Compose UI test manifest from `debugImplementation(libs.androidx.compose.ui.test.manifest)`
