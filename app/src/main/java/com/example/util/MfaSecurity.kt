package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object MfaSecurity {
    const val SUPPORT_TICKET_URL = "https://docs.google.com/forms/d/e/1FAIpQLSfXMACZYpNXz10nZCllbkmcKkWJtlWA_RdNYstyPZS_ir7wzw/viewform?usp=publish-editor"
    private const val USER_COLLECTION = "users"
    private const val TOTP_SECRET_FIELD = "totpSecret"
    private const val TWO_FACTOR_ENABLED_FIELD = "twoFactorEnabled"
    private const val PASSKEY_ENABLED_FIELD = "passkeyEnabled"
    private const val PASSKEY_DEVICE_NAME_FIELD = "passkeyDeviceName"
    private const val PASSKEY_CREATED_AT_FIELD = "passkeyCreatedAt"
    private const val BACKUP_CODES_FIELD = "backupCodes"
    private const val USED_BACKUP_CODES_FIELD = "usedBackupCodes"

    suspend fun fetchTotpSecret(db: FirebaseFirestore, userId: String): String? {
        return try {
            val snapshot = db.collection(USER_COLLECTION)
                .document(userId)
                .get()
                .await()
            snapshot.getString(TOTP_SECRET_FIELD)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun is2FAEnabledInFirestore(db: FirebaseFirestore, userId: String): Boolean {
        return try {
            val snapshot = db.collection(USER_COLLECTION)
                .document(userId)
                .get()
                .await()
            snapshot.getBoolean(TWO_FACTOR_ENABLED_FIELD) ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun set2FAEnabledInFirestore(db: FirebaseFirestore, userId: String, enabled: Boolean) {
        try {
            db.collection(USER_COLLECTION)
                .document(userId)
                .set(mapOf(TWO_FACTOR_ENABLED_FIELD to enabled), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveTotpSecret(db: FirebaseFirestore, userId: String, secret: String) {
        try {
            db.collection(USER_COLLECTION)
                .document(userId)
                .set(
                    mapOf(
                        TOTP_SECRET_FIELD to secret,
                        TWO_FACTOR_ENABLED_FIELD to true
                    ),
                    SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeTotpSecret(db: FirebaseFirestore, userId: String) {
        try {
            db.collection(USER_COLLECTION)
                .document(userId)
                .set(
                    mapOf(
                        TWO_FACTOR_ENABLED_FIELD to false
                    ),
                    SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun registerPasskeyInFirestore(db: FirebaseFirestore, userId: String, deviceName: String) {
        try {
            db.collection(USER_COLLECTION)
                .document(userId)
                .set(
                    mapOf(
                        PASSKEY_ENABLED_FIELD to true,
                        PASSKEY_DEVICE_NAME_FIELD to deviceName,
                        PASSKEY_CREATED_AT_FIELD to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removePasskeyFromFirestore(db: FirebaseFirestore, userId: String) {
        try {
            db.collection(USER_COLLECTION)
                .document(userId)
                .set(
                    mapOf(
                        PASSKEY_ENABLED_FIELD to false
                    ),
                    SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateSecret(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING)
            .replace("=", "")
            .replace("+", "")
            .replace("/", "")
            .uppercase()
            .take(32)
    }

    fun getOtpAuthUri(email: String, secret: String, issuer: String = "DeviceSpot"): String {
        val encodedEmail = java.net.URLEncoder.encode(email.ifBlank { "User" }, "UTF-8")
        val encodedIssuer = java.net.URLEncoder.encode(issuer, "UTF-8")
        return "otpauth://totp/$encodedIssuer:$encodedEmail?secret=$secret&issuer=$encodedIssuer&algorithm=SHA1&digits=6&period=30"
    }

    fun verifyCode(secret: String, code: String): Boolean {
        val normalized = code.filter { it.isDigit() }.take(6)
        if (normalized.length != 6) return false

        return try {
            val secretBytes = Base64.decode(secret, Base64.NO_WRAP)
            val timeCounter = System.currentTimeMillis() / 30_000L

            listOf(0L, -1L, 1L).any { offset ->
                generateTotp(secretBytes, timeCounter + offset) == normalized
            }
        } catch (e: Exception) {
            false
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

        val truncated = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        val otp = truncated % 1_000_000
        return otp.toString().padStart(6, '0')
    }

    fun generateBackupCodes(count: Int = 8): List<String> {
        val random = SecureRandom()
        val codes = linkedSetOf<String>()
        while (codes.size < count) {
            val sb = StringBuilder(10)
            sb.append(random.nextInt(9) + 1)
            for (i in 1..9) {
                sb.append(random.nextInt(10))
            }
            codes.add(sb.toString())
        }
        return codes.toList()
    }

    suspend fun fetchBackupCodes(db: FirebaseFirestore, userId: String): List<String> {
        return try {
            val snapshot = db.collection(USER_COLLECTION)
                .document(userId)
                .get()
                .await()
            @Suppress("UNCHECKED_CAST")
            (snapshot.get(BACKUP_CODES_FIELD) as? List<String>)?.filter { it.isNotBlank() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchOrCreateBackupCodes(db: FirebaseFirestore, userId: String): List<String> {
        val existing = fetchBackupCodes(db, userId)
        if (existing.isNotEmpty()) {
            return existing
        }
        val newCodes = generateBackupCodes(8)
        saveBackupCodes(db, userId, newCodes)
        return newCodes
    }

    suspend fun saveBackupCodes(db: FirebaseFirestore, userId: String, codes: List<String>) {
        try {
            db.collection(USER_COLLECTION)
                .document(userId)
                .set(
                    mapOf(
                        BACKUP_CODES_FIELD to codes,
                        USED_BACKUP_CODES_FIELD to emptyList<String>()
                    ),
                    SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun regenerateBackupCodes(db: FirebaseFirestore, userId: String): List<String> {
        val newCodes = generateBackupCodes(8)
        saveBackupCodes(db, userId, newCodes)
        return newCodes
    }

    suspend fun useBackupCode(db: FirebaseFirestore, userId: String, inputCode: String): Result<Int> {
        val sanitized = inputCode.filter { it.isDigit() }
        if (sanitized.length != 10) {
            return Result.failure(IllegalArgumentException("Backup code must be exactly 10 digits"))
        }
        return try {
            val docRef = db.collection(USER_COLLECTION).document(userId)
            val snapshot = docRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val currentCodes = (snapshot.get(BACKUP_CODES_FIELD) as? List<String>)?.toMutableList() ?: mutableListOf()
            @Suppress("UNCHECKED_CAST")
            val usedCodes = (snapshot.get(USED_BACKUP_CODES_FIELD) as? List<String>)?.toMutableList() ?: mutableListOf()

            if (!currentCodes.contains(sanitized)) {
                if (usedCodes.contains(sanitized)) {
                    return Result.failure(IllegalStateException("This backup code has already been used and is no longer valid."))
                }
                return Result.failure(IllegalArgumentException("Invalid backup code. Please check and try again."))
            }

            currentCodes.remove(sanitized)
            usedCodes.add(sanitized)

            docRef.set(
                mapOf(
                    BACKUP_CODES_FIELD to currentCodes,
                    USED_BACKUP_CODES_FIELD to usedCodes
                ),
                SetOptions.merge()
            ).await()

            Result.success(currentCodes.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatBackupCode(code: String): String {
        val digits = code.filter { it.isDigit() }
        return if (digits.length == 10) {
            "${digits.substring(0, 5)} ${digits.substring(5)}"
        } else code
    }

    fun buildBackupCodesText(email: String, codes: List<String>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("       DEVICESPOT - 2FA BACKUP RECOVERY CODES     ")
        sb.appendLine("==================================================")
        sb.appendLine("Account: $email")
        sb.appendLine("Generated: $timestamp")
        sb.appendLine("Total Active Codes: ${codes.size}")
        sb.appendLine()
        sb.appendLine("IMPORTANT SECURITY NOTICE:")
        sb.appendLine("• Each 10-digit backup code can only be used ONCE.")
        sb.appendLine("• Keep these codes strictly confidential and store them safely.")
        sb.appendLine("• If you lose access to your authenticator app, enter one of")
        sb.appendLine("  these codes during sign-in to regain access to your account.")
        sb.appendLine()
        sb.appendLine("YOUR BACKUP CODES:")
        codes.forEachIndexed { index, code ->
            sb.appendLine("  ${index + 1}. ${formatBackupCode(code)}")
        }
        sb.appendLine()
        sb.appendLine("CANNOT SIGN IN / LOST BOTH 2FA AND BACKUP CODES?")
        sb.appendLine("Submit an official account recovery support ticket at:")
        sb.appendLine(SUPPORT_TICKET_URL)
        sb.appendLine("==================================================")
        return sb.toString()
    }

    fun downloadBackupCodesFile(context: Context, email: String, codes: List<String>): Boolean {
        val content = buildBackupCodesText(email, codes)
        val fileName = "DeviceSpot_2FA_Backup_Codes_${System.currentTimeMillis()}.txt"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
                    true
                } else {
                    false
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                file.writeText(content, Charsets.UTF_8)
                Toast.makeText(context, "Saved to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun shareBackupCodes(context: Context, email: String, codes: List<String>) {
        val text = buildBackupCodesText(email, codes)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DeviceSpot 2FA Backup Codes")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Save or Export 2FA Backup Codes").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun copyBackupCodesToClipboard(context: Context, codes: List<String>) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = codes.mapIndexed { idx, code -> "${idx + 1}. ${formatBackupCode(code)}" }.joinToString("\n")
        clipboard.setPrimaryClip(ClipData.newPlainText("DeviceSpot 2FA Backup Codes", text))
        Toast.makeText(context, "All ${codes.size} backup codes copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun openSupportTicket(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_TICKET_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open support ticket page: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

object SecuritySettings {
    private const val PREFS_NAME = "security_preferences"
    private const val KEY_REQUIRE_2FA = "require_2fa"
    private const val KEY_ENABLE_PASSKEYS = "enable_passkeys"
    private const val KEY_PASSKEY_REGISTERED = "passkey_registered"
    private const val KEY_PASSKEY_USER_EMAIL = "passkey_user_email"
    private const val KEY_PASSKEY_USER_UID = "passkey_user_uid"
    private const val KEY_PASSKEY_DEVICE_NAME = "passkey_device_name"

    fun is2FAEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REQUIRE_2FA, false)
    }

    fun set2FAEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REQUIRE_2FA, enabled)
            .apply()
    }

    fun isPasskeysEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLE_PASSKEYS, true)
    }

    fun setPasskeysEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLE_PASSKEYS, enabled)
            .apply()
    }

    fun isPasskeyRegistered(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PASSKEY_REGISTERED, false)
    }

    fun getPasskeyEmail(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PASSKEY_USER_EMAIL, null)
    }

    fun getPasskeyDeviceName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PASSKEY_DEVICE_NAME, null) ?: "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    fun registerPasskey(context: Context, email: String, uid: String) {
        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PASSKEY_REGISTERED, true)
            .putBoolean(KEY_ENABLE_PASSKEYS, true)
            .putString(KEY_PASSKEY_USER_EMAIL, email)
            .putString(KEY_PASSKEY_USER_UID, uid)
            .putString(KEY_PASSKEY_DEVICE_NAME, deviceName)
            .apply()
    }

    fun unregisterPasskey(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PASSKEY_REGISTERED, false)
            .remove(KEY_PASSKEY_USER_EMAIL)
            .remove(KEY_PASSKEY_USER_UID)
            .remove(KEY_PASSKEY_DEVICE_NAME)
            .apply()
    }
}
