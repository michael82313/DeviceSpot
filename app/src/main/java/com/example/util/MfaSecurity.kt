package com.example.util

import android.content.Context
import android.util.Base64
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object MfaSecurity {
    private const val USER_MFA_COLLECTION = "users"
    private const val TOTP_SECRET_FIELD = "totpSecret"

    suspend fun fetchTotpSecret(db: FirebaseFirestore, userId: String): String? {
        val snapshot = db.collection(USER_MFA_COLLECTION)
            .document(userId)
            .get()
            .await()
        return snapshot.getString(TOTP_SECRET_FIELD)
    }

    suspend fun saveTotpSecret(db: FirebaseFirestore, userId: String, secret: String) {
        db.collection(USER_MFA_COLLECTION)
            .document(userId)
            .set(mapOf(TOTP_SECRET_FIELD to secret), SetOptions.merge())
            .await()
    }

    fun generateSecret(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING)
            .replace("=", "")
            .replace("+", "")
            .replace("/", "")
    }

    fun verifyCode(secret: String, code: String): Boolean {
        val normalized = code.filter { it.isDigit() }.take(6)
        if (normalized.length != 6) return false

        val secretBytes = Base64.decode(secret, Base64.NO_WRAP)
        val timeCounter = System.currentTimeMillis() / 30_000L

        return listOf(0L, -1L, 1L).any { offset ->
            generateTotp(secretBytes, timeCounter + offset) == normalized
        }
    }

    private fun generateTotp(secretBytes: ByteArray, counter: Long): String {
        val data = ByteArray(8)
        var value = counter
        for (i in 7 downTo 0) {
            data[i] = (value and 0xFFL).toByte()
            value = value ushr 8
        }

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secretBytes, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = hash.last().toInt() and 0x0F

        var truncated = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        val otp = truncated % 1_000_000
        return otp.toString().padStart(6, '0')
    }
}

object SecuritySettings {
    private const val PREFS_NAME = "security_preferences"
    private const val KEY_REQUIRE_2FA = "require_2fa"
    private const val KEY_ENABLE_PASSKEYS = "enable_passkeys"

    fun is2FAEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REQUIRE_2FA, true)
    }

    fun set2FAEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REQUIRE_2FA, enabled)
            .apply()
    }

    fun isPasskeysEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLE_PASSKEYS, false)
    }

    fun setPasskeysEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLE_PASSKEYS, enabled)
            .apply()
    }
}
