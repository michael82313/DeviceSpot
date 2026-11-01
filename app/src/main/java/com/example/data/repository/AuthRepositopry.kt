// Location: app/src/main/java/com/example/data/repository/AuthRepository.kt
package com.example.data.repository

import com.example.util.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.mfa.FactorType
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.mfa.MfaApi

class AuthRepository {
    private val auth = SupabaseClient.client.auth

    // ==========================================
    // 1. TWO-FACTOR AUTHENTICATION (TOTP)
    // ==========================================

    // Start enrollment: Returns a QR code URI string to display to your user
    suspend fun enrollInTOTP(factorName: String): String? {
        val factor = auth.mfa.enroll(
            factorType = FactorType.TOTP,
            friendlyName = factorName
        )
        // This URI contains the secret key layout to turn into a QR barcode visual
        return factor.totp?.qrCodeUri
    }

    // Confirm enrollment or verify step during a multi-factor login
    suspend fun verifyMfaToken(challengeId: String, code: String): Boolean {
        return try {
            auth.mfa.verify(
                challengeId = challengeId,
                code = code
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // 2. PASSKEY / WEBAUTHN WORKFLOWS
    // ==========================================

    // Registers a passkey mapped securely to the logged-in user profile
    suspend fun signUpWithDevicePasskey(email: String) {
        auth.signUpWithWebAuthn(email = email)
    }

    // Logs the user completely in using their stored device biometric key
    suspend fun signInWithDevicePasskey() {
        auth.signInWithWebAuthn()
    }
}

