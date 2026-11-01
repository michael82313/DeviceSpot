package com.example.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var isResetLoading by remember { mutableStateOf(false) }
    var forgotPasswordError by remember { mutableStateOf<String?>(null) }
    var forgotPasswordSuccess by remember { mutableStateOf(false) }

    fun handlePasswordReset() {
        val trimmedEmail = forgotPasswordEmail.trim()
        if (trimmedEmail.isEmpty()) {
            forgotPasswordError = "Please enter your email address."
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            forgotPasswordError = "Please enter a valid email address."
            return
        }

        focusManager.clearFocus()
        isResetLoading = true
        forgotPasswordError = null

        coroutineScope.launch {
            try {
                auth.sendPasswordResetEmail(trimmedEmail).await()
                forgotPasswordSuccess = true
                statusMessage = "Password reset instructions sent to $trimmedEmail"
            } catch (e: Exception) {
                Log.e("SignInScreen", "Password reset failed", e)
                forgotPasswordError = e.localizedMessage ?: "Failed to send reset email. Please try again."
            } finally {
                isResetLoading = false
            }
        }
    }

    fun handleEmailAuth() {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isEmpty()) {
            errorMessage = "Please enter your email address."
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            errorMessage = "Please enter a valid email address."
            return
        }
        if (trimmedPassword.isEmpty()) {
            errorMessage = "Please enter your password."
            return
        }
        if (trimmedPassword.length < 6) {
            errorMessage = "Password must be at least 6 characters long."
            return
        }

        focusManager.clearFocus()
        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                if (isSignUpMode) {
                    auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
                } else {
                    auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
                }
                onSignInSuccess()
            } catch (e: Exception) {
                Log.e("SignInScreen", "Email auth failed", e)
                errorMessage = e.localizedMessage ?: "Authentication failed. Please verify your credentials."
            } finally {
                isLoading = false
            }
        }
    }

    fun handlePasskeySignIn() {
        focusManager.clearFocus()
        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                
                // Configure request query parameter strings for passkeys
                val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
                    requestJson = "{\"challenge\": \"sample_challenge\", \"rpId\": \"com.aistudio.devicespot.kqztv\"}"
                )

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(getPublicKeyCredentialOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                
                // Handle your authenticated passkey server response mapping logic here
                statusMessage = "Passkey authenticated successfully!"
                onSignInSuccess()

            } catch (e: GetCredentialCancellationException) {
                Log.d("SignInScreen", "Passkey verification canceled by user")
            } catch (e: GetCredentialException) {
                Log.e("SignInScreen", "Passkey authentication error", e)
                errorMessage = e.localizedMessage ?: "Passkey authentication failed."
            } catch (e: Exception) {
                Log.e("SignInScreen", "Unexpected passkey structural issue", e)
                errorMessage = e.localizedMessage ?: "Failed to authenticate device biometric."
            } finally {
                isLoading = false
            }
        }
    }

    fun handleGoogleSignIn() {
        focusManager.clearFocus()
        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val serverClientId = context.getString(R.string.default_web_client_id)
                val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
