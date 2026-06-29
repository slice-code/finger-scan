package com.example.security

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object BiometricEncryptionUtils {

    /**
     * Generates a unique SHA-256 fingerprint biometric signature based on name, nip, and dynamic coordinates.
     */
    fun generateFingerprintHash(name: String, employeeId: String, seed: String): String {
        val input = "FINGERPRINT_TEMPLATE_V1|$name|$employeeId|$seed"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculates HMAC-SHA256 signature for payload verification.
     */
    fun calculateHmac(payload: String, secretKey: String): String {
        return try {
            val hmacKey = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(hmacKey)
            val hmacBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            "SIGNATURE_ERROR: ${e.localizedMessage}"
        }
    }

    /**
     * Encrypts a string payload using AES-256 with the secret key as a passphrase.
     */
    fun encryptAES(payload: String, secretKey: String): String {
        return try {
            // Hash the secretKey with SHA-256 to ensure a proper 256-bit (32 bytes) key length
            val sha = MessageDigest.getInstance("SHA-256")
            val keyBytes = sha.digest(secretKey.toByteArray(Charsets.UTF_8))
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            val encryptedBytes = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            "ENCRYPTION_ERROR: ${e.localizedMessage}"
        }
    }

    /**
     * Decrypts a string payload using AES-256.
     */
    fun decryptAES(encryptedPayload: String, secretKey: String): String {
        return try {
            val sha = MessageDigest.getInstance("SHA-256")
            val keyBytes = sha.digest(secretKey.toByteArray(Charsets.UTF_8))
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
            val decodedBytes = Base64.decode(encryptedPayload, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "DECRYPTION_ERROR: ${e.localizedMessage}"
        }
    }
}
