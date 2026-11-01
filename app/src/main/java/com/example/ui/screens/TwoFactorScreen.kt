// Location: app/src/main/java/com/example/ui/screens/TwoFactorScreen.kt
package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.time.SystemTimeProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun TwoFactorScreen(
    userUid: String,
    onVerificationSuccess: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    
    var verificationCode by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleVerifyMfa() {
        val trimmedCode = verificationCode.trim()
        if (trimmedCode.length != 6) {
            errorMessage = "Verification code must be exactly 6 digits."
            return
        }

        focusManager.clearFocus()
        isVerifying = true
        errorMessage = null

        coroutineScope.launch {
            try {
                // Fetch the dynamic database name from your resources metadata
                val databaseId = context.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)

                // Query the user's specific security factor settings document from Firestore
                val documentSnapshot = db.collection("users")
                    .document(userUid)
                    .collection("security")
                    .document("mfa_settings")
                    .get()
                    .await()

                val savedUserSecretKey = documentSnapshot.getString("totpSecret")

                if (savedUserSecretKey.isNullOrEmpty()) {
                    errorMessage = "No 2FA configuration record found for this profile. Please register a key first."
                    isVerifying = false
                    return@launch
                }

                // Verify the running authenticator pin against the retrieved secret
                val timeProvider = SystemTimeProvider()
                val codeGenerator = DefaultCodeGenerator()
                val verifier = DefaultCodeVerifier(codeGenerator, timeProvider)

                val isValid = verifier.isValidCode(savedUserSecretKey, trimmedCode)

                if (isValid) {
                    onVerificationSuccess()
                } else {
                    errorMessage = "Invalid verification code. Please check your device clock sync."
                    isVerifying = false
                }

            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to reach security servers. Verify your network link."
                isVerifying = false
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
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Two-Factor Authentication",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter the 6-digit verification code from your authenticator app to access your device ledger logs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = verificationCode,
                onValueChange = { if (it.length <= 6) verificationCode = it },
                label = { Text("Verification Code") },
                placeholder = { Text("000000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { handleVerifyMfa() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { handleVerifyMfa() },
                enabled = !isVerifying && verificationCode.length == 6,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Verify Code", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onSignOut) {
                Text("Cancel and Sign Out", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

