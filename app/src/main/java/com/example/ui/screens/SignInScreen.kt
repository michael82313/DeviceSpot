package com.example.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.util.MfaSecurity
import com.example.util.SecuritySettings
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
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.TotpMultiFactorGenerator
import com.google.firebase.auth.TotpMultiFactorInfo
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

    var showNoPasskeyDialog by remember { mutableStateOf(false) }

    var showMfaChallengeDialog by remember { mutableStateOf(false) }
    var pendingMfaResolver by remember { mutableStateOf<MultiFactorResolver?>(null) }
    var mfaChallengeCode by remember { mutableStateOf("") }
    var mfaChallengeError by remember { mutableStateOf<String?>(null) }
    var isResolvingMfa by remember { mutableStateOf(false) }

    val passkeyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val registeredEmail = SecuritySettings.getPasskeyEmail(context)
            val isRegistered = SecuritySettings.isPasskeyRegistered(context)
            if (isRegistered && !registeredEmail.isNullOrBlank()) {
                statusMessage = "Passkey verified for $registeredEmail! Signing in..."
                onSignInSuccess()
            } else {
                errorMessage = "No passkey found on this device."
            }
        } else {
            errorMessage = "Passkey verification was cancelled."
        }
    }

    fun handlePasskeySignIn() {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
            errorMessage = "Screen lock or biometrics (fingerprint/PIN) not configured on this device."
            return
        }

        val isRegistered = SecuritySettings.isPasskeyRegistered(context)
        val registeredEmail = SecuritySettings.getPasskeyEmail(context)

        // Strict security: Passkey must be pre-enrolled on this device from User Settings
        if (!isRegistered || registeredEmail.isNullOrBlank()) {
            showNoPasskeyDialog = true
            return
        }

        // If the user typed an email, verify it matches the device's registered passkey account
        val typedEmail = email.trim()
        if (typedEmail.isNotBlank() && !typedEmail.equals(registeredEmail, ignoreCase = true)) {
            errorMessage = "This device has a passkey enrolled for $registeredEmail. To sign in as $typedEmail, please enter your password or sign in with Google."
            return
        }

        try {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "Sign In with Passkey",
                "Verify fingerprint, face, or device PIN for $registeredEmail"
            )
            if (intent != null) {
                passkeyLauncher.launch(intent)
            } else {
                errorMessage = "Device credential prompt unavailable."
            }
        } catch (e: Exception) {
            errorMessage = "Passkey error: ${e.localizedMessage}"
        }
    }

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
            } catch (e: FirebaseAuthMultiFactorException) {
                Log.w("SignInScreen", "Second factor challenge required for email auth", e)
                pendingMfaResolver = e.resolver
                mfaChallengeCode = ""
                mfaChallengeError = null
                showMfaChallengeDialog = true
            } catch (e: Exception) {
                Log.e("SignInScreen", "Email auth failed", e)
                errorMessage = e.localizedMessage ?: "Authentication failed. Please verify your credentials."
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
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onSignInSuccess()
                } else {
                    errorMessage = "Unexpected credential received."
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d("SignInScreen", "Google Sign-In cancelled by user")
            } catch (e: GetCredentialException) {
                Log.e("SignInScreen", "CredentialManager exception", e)
                errorMessage = e.localizedMessage ?: "Google Sign-In failed."
            } catch (e: FirebaseAuthMultiFactorException) {
                Log.w("SignInScreen", "Second factor challenge required for Google auth", e)
                pendingMfaResolver = e.resolver
                mfaChallengeCode = ""
                mfaChallengeError = null
                showMfaChallengeDialog = true
            } catch (e: Exception) {
                Log.e("SignInScreen", "Sign-in error", e)
                errorMessage = e.localizedMessage ?: "Failed to sign in. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // App Brand Logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "DeviceSpot Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DeviceSpot",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Cloud Hardware & Location Tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isSignUpMode)
                    "Create an account to sync your devices, QR tags, and spots across all your screens."
                else
                    "Sign in to keep all your hardware devices and storage locations synced in real-time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Mode Tabs (Sign In vs Create Account)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = if (isSignUpMode) 1 else 0,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = !isSignUpMode,
                        onClick = {
                            isSignUpMode = false
                            errorMessage = null
                            statusMessage = null
                        },
                        text = {
                            Text(
                                text = "Sign In",
                                fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_sign_in")
                    )
                    Tab(
                        selected = isSignUpMode,
                        onClick = {
                            isSignUpMode = true
                            errorMessage = null
                            statusMessage = null
                        },
                        text = {
                            Text(
                                text = "Create Account",
                                fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_create_account")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status Banner (e.g., Password reset email sent)
            if (statusMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("status_message_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = statusMessage ?: "",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Error Banner
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("Email Address") },
                placeholder = { Text("name@company.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_in_email_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("Password") },
                placeholder = { Text("••••••••") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { handleEmailAuth() }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_in_password_input")
            )

            if (!isSignUpMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            forgotPasswordEmail = email.trim()
                            forgotPasswordError = null
                            forgotPasswordSuccess = false
                            showForgotPasswordDialog = true
                        },
                        modifier = Modifier.testTag("forgot_password_button")
                    ) {
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Primary Auth Button (Sign In / Create Account)
            Button(
                onClick = { if (!isLoading) handleEmailAuth() },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(if (isSignUpMode) "sign_up_btn" else "sign_in_btn")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isSignUpMode) "Creating Account..." else "Signing In...",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = if (isSignUpMode) "Create Account" else "Sign In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Toggle Text Button
            TextButton(
                onClick = {
                    isSignUpMode = !isSignUpMode
                    errorMessage = null
                    statusMessage = null
                },
                modifier = Modifier.testTag("auth_mode_toggle_btn")
            ) {
                Text(
                    text = if (isSignUpMode)
                        "Already have an account? Sign In"
                    else
                        "Don't have an account? Create Account",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Divider "or continue with"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  OR  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In Button
            OutlinedButton(
                onClick = { if (!isLoading) handleGoogleSignIn() },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_sign_in_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "G",
                                color = Color(0xFF4285F4),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Passkey Sign-In Button
            OutlinedButton(
                onClick = { if (!isLoading) handlePasskeySignIn() },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("passkey_sign_in_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Passkey",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign in with Passkey",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recovery ticket support link
            TextButton(
                onClick = { MfaSecurity.openSupportTicket(context) },
                modifier = Modifier.testTag("sign_in_recovery_ticket_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Can't sign in or lost 2FA? Submit a recovery ticket",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Value props
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Syncs across all devices logged into this account",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DeviceSpot v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("sign_in_app_version")
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResetLoading) {
                    showForgotPasswordDialog = false
                }
            },
            icon = {
                Icon(
                    imageVector = if (forgotPasswordSuccess) Icons.Default.CheckCircle else Icons.Default.LockReset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (forgotPasswordSuccess) "Check Your Inbox" else "Reset Password",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (forgotPasswordSuccess) {
                        Text(
                            text = "We have sent a password reset link to:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = forgotPasswordEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please follow the instructions in the email to set a new password, then return here to sign in with your updated credentials.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Enter your account email address and we'll send you a link to reset your password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (forgotPasswordError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = forgotPasswordError ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = forgotPasswordEmail,
                            onValueChange = {
                                forgotPasswordEmail = it
                                if (forgotPasswordError != null) forgotPasswordError = null
                            },
                            label = { Text("Account Email") },
                            placeholder = { Text("name@company.com") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { handlePasswordReset() }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_password_email_input")
                        )
                    }
                }
            },
            confirmButton = {
                if (forgotPasswordSuccess) {
                    Button(
                        onClick = { showForgotPasswordDialog = false },
                        modifier = Modifier.testTag("forgot_password_done_btn")
                    ) {
                        Text("Done")
                    }
                } else {
                    Button(
                        onClick = { handlePasswordReset() },
                        enabled = !isResetLoading,
                        modifier = Modifier.testTag("forgot_password_submit_button")
                    ) {
                        if (isResetLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending...")
                        } else {
                            Text("Send Reset Link")
                        }
                    }
                }
            },
            dismissButton = {
                if (!forgotPasswordSuccess) {
                    OutlinedButton(
                        onClick = { showForgotPasswordDialog = false },
                        enabled = !isResetLoading,
                        modifier = Modifier.testTag("forgot_password_cancel_btn")
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showNoPasskeyDialog) {
        AlertDialog(
            onDismissRequest = { showNoPasskeyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "No Passkey on This Device",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "No passkey is set up on this device yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "For your account security, passkeys can only be created from User Settings after signing in with your email & password or Google account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNoPasskeyDialog = false },
                    modifier = Modifier.testTag("no_passkey_sign_in_pwd_btn")
                ) {
                    Text("Sign In with Password")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNoPasskeyDialog = false
                        MfaSecurity.openSupportTicket(context)
                    },
                    modifier = Modifier.testTag("no_passkey_support_ticket_btn")
                ) {
                    Text("Recovery Ticket")
                }
            }
        )
    }

    if (showMfaChallengeDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResolvingMfa) {
                    showMfaChallengeDialog = false
                    pendingMfaResolver = null
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "2-Step Verification Challenge",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "This account requires a second-factor verification code to finish signing in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = mfaChallengeCode,
                        onValueChange = {
                            mfaChallengeCode = it.filter { ch -> ch.isDigit() }.take(6)
                            if (mfaChallengeError != null) mfaChallengeError = null
                        },
                        label = { Text("6-Digit Verification Code") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mfa_challenge_code_input")
                    )

                    if (mfaChallengeError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = mfaChallengeError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Cannot access your authenticator?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If you lost your 2FA device or backup codes, submit an account recovery ticket below or ask your administrator to remove MFA in Firebase Console under Authentication > Users > ⋮ > 'Configure MFA'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            MfaSecurity.openSupportTicket(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mfa_challenge_recovery_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Recovery Ticket", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val resolver = pendingMfaResolver ?: return@Button
                        val cleanCode = mfaChallengeCode.filter { it.isDigit() }
                        if (cleanCode.length != 6) {
                            mfaChallengeError = "Please enter all 6 digits."
                            return@Button
                        }

                        isResolvingMfa = true
                        mfaChallengeError = null

                        coroutineScope.launch {
                            try {
                                val totpHint = resolver.hints.filterIsInstance<TotpMultiFactorInfo>().firstOrNull()
                                val assertion = if (totpHint != null) {
                                    TotpMultiFactorGenerator.getAssertionForSignIn(totpHint.uid, cleanCode)
                                } else {
                                    val firstHint = resolver.hints.firstOrNull()
                                    if (firstHint is TotpMultiFactorInfo) {
                                        TotpMultiFactorGenerator.getAssertionForSignIn(firstHint.uid, cleanCode)
                                    } else {
                                        throw IllegalStateException("Second factor format not supported directly. Please use account recovery.")
                                    }
                                }
                                resolver.resolveSignIn(assertion).await()
                                showMfaChallengeDialog = false
                                pendingMfaResolver = null
                                onSignInSuccess()
                            } catch (ex: Exception) {
                                Log.e("SignInScreen", "MFA resolution failed", ex)
                                mfaChallengeError = ex.localizedMessage ?: "Invalid verification code. Please check your authenticator and try again."
                            } finally {
                                isResolvingMfa = false
                            }
                        }
                    },
                    enabled = !isResolvingMfa && mfaChallengeCode.length == 6,
                    modifier = Modifier.testTag("mfa_challenge_confirm_btn")
                ) {
                    if (isResolvingMfa) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verifying...")
                    } else {
                        Text("Verify & Sign In")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isResolvingMfa) {
                            showMfaChallengeDialog = false
                            pendingMfaResolver = null
                        }
                    },
                    enabled = !isResolvingMfa
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
