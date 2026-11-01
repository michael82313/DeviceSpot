package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.DeviceViewModel
import com.example.util.SecuritySettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsDialog(
    viewModel: DeviceViewModel,
    deviceCount: Int,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentDisplayName by viewModel.userDisplayNameState.collectAsStateWithLifecycle()
    val currentEmail by viewModel.userEmailState.collectAsStateWithLifecycle()
    val isEmailVerified by viewModel.isEmailVerified.collectAsStateWithLifecycle()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val isPasswordUser = currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true

    var displayNameInput by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }
    var isUpdatingName by remember { mutableStateOf(false) }
    var isTwoFactorEnabled by remember { mutableStateOf(SecuritySettings.is2FAEnabled(context)) }
    var isPasskeysEnabled by remember { mutableStateOf(SecuritySettings.isPasskeysEnabled(context)) }

    // Password change state
    var showPasswordForm by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isUpdatingPassword by remember { mutableStateOf(false) }
    var passwordErrorMessage by remember { mutableStateOf<String?>(null) }

    // Email verification state
    var isSendingVerification by remember { mutableStateOf(false) }
    var isCheckingVerification by remember { mutableStateOf(false) }
    var verificationStatusMessage by remember { mutableStateOf<String?>(null) }

    // Account deletion state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var deleteErrorMessage by remember { mutableStateOf<String?>(null) }

    // Refresh user state when opening settings
    LaunchedEffect(Unit) {
        viewModel.reloadUser {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("user_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "User Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Profile Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initials = currentDisplayName.firstOrNull()?.uppercaseChar()?.toString()
                                    ?: currentEmail.firstOrNull()?.uppercaseChar()?.toString()
                                    ?: "U"
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentDisplayName.ifBlank { "User" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentEmail.isNotBlank()) {
                                Text(
                                    text = currentEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isEmailVerified) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isEmailVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isEmailVerified) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEmailVerified) "Email Verified" else "Email Unverified",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEmailVerified) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$deviceCount Devices Synced",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("2-Step Verification")
                            Text(
                                text = if (isTwoFactorEnabled) "Required for sign in" else "Currently off",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = isTwoFactorEnabled,
                            onCheckedChange = {
                                isTwoFactorEnabled = it
                                SecuritySettings.set2FAEnabled(context, it)
                                Toast.makeText(
                                    context,
                                    if (it) "2-step verification enabled" else "2-step verification disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Passkeys")
                            Text(
                                text = if (isPasskeysEnabled) "Passkey sign-in is enabled" else "Passkey sign-in is off",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = isPasskeysEnabled,
                            onCheckedChange = {
                                isPasskeysEnabled = it
                                SecuritySettings.setPasskeysEnabled(context, it)
                                Toast.makeText(
                                    context,
                                    if (it) "Passkeys enabled for this device" else "Passkeys disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 1: Change Account Details (Display Name)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Account Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("display_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            isUpdatingName = true
                            viewModel.updateDisplayName(displayNameInput) { result ->
                                isUpdatingName = false
                                result.fold(
                                    onSuccess = {
                                        Toast.makeText(context, "Display name updated!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        enabled = !isUpdatingName && displayNameInput.isNotBlank() && displayNameInput != currentDisplayName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_display_name_btn")
                    ) {
                        if (isUpdatingName) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Name")
                        }
                    }

                    if (isPasswordUser) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = { showPasswordForm = !showPasswordForm },
                            modifier = Modifier.testTag("toggle_password_form_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showPasswordForm) "Cancel Password Change" else "Change Account Password")
                        }

                        AnimatedVisibility(visible = showPasswordForm) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = {
                                        newPassword = it
                                        passwordErrorMessage = null
                                    },
                                    label = { Text("New Password (min 6 chars)") },
                                    singleLine = true,
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle password visibility"
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_password_input")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = {
                                        confirmPassword = it
                                        passwordErrorMessage = null
                                    },
                                    label = { Text("Confirm New Password") },
                                    singleLine = true,
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_password_input")
                                )

                                passwordErrorMessage?.let { err ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (newPassword.length < 6) {
                                            passwordErrorMessage = "Password must be at least 6 characters"
                                            return@Button
                                        }
                                        if (newPassword != confirmPassword) {
                                            passwordErrorMessage = "Passwords do not match"
                                            return@Button
                                        }

                                        isUpdatingPassword = true
                                        viewModel.updatePassword(newPassword) { result ->
                                            isUpdatingPassword = false
                                            result.fold(
                                                onSuccess = {
                                                    Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                                    newPassword = ""
                                                    confirmPassword = ""
                                                    showPasswordForm = false
                                                },
                                                onFailure = { err ->
                                                    if (err is FirebaseAuthRecentLoginRequiredException) {
                                                        passwordErrorMessage = "For security, changing password requires recent sign-in. Please sign out and sign in again."
                                                    } else {
                                                        passwordErrorMessage = err.message ?: "Failed to update password"
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    enabled = !isUpdatingPassword && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submit_password_change_btn")
                                ) {
                                    if (isUpdatingPassword) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Updating...")
                                    } else {
                                        Text("Update Password")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            tint = if (isEmailVerified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Email Verification",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isEmailVerified) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Your email address is verified and active.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Verifying your email confirms ownership, secures your account, and guarantees multi-device hardware synchronization.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        verificationStatusMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    isSendingVerification = true
                                    viewModel.sendEmailVerification { result ->
                                        isSendingVerification = false
                                        result.fold(
                                            onSuccess = {
                                                verificationStatusMessage = "Verification link sent to $currentEmail! Check your inbox and spam folder."
                                                Toast.makeText(context, "Verification email sent!", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = { err ->
                                                verificationStatusMessage = "Error: ${err.message}"
                                            }
                                        )
                                    }
                                },
                                enabled = !isSendingVerification,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("send_email_verification_btn")
                            ) {
                                if (isSendingVerification) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sending...")
                                } else {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send Link")
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    isCheckingVerification = true
                                    viewModel.reloadUser { verified ->
                                        isCheckingVerification = false
                                        if (verified) {
                                            verificationStatusMessage = "Email verified successfully!"
                                            Toast.makeText(context, "Email is now verified!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Not verified yet. Check the link in your email.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isCheckingVerification,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("refresh_verification_btn")
                            ) {
                                if (isCheckingVerification) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Checking...")
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Refresh")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Account",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Permanently deletes your account and wipes all registered devices, QR codes, and audit logs from the cloud database. This action is irreversible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    deleteErrorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_open_dialog_btn")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete My Account & Data")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onSignOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_sign_out_btn")
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) showDeleteConfirmDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Delete Account Permanently?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Are you completely sure you want to delete your account? All $deviceCount synced hardware devices, audit logs, and cloud storage records will be erased forever.")
                    Text("This operation cannot be undone.", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeletingAccount = true
                        deleteErrorMessage = null
                        viewModel.deleteAccount { result ->
                            isDeletingAccount = false
                            result.fold(
                                onSuccess = {
                                    showDeleteConfirmDialog = false
                                    onDismiss()
                                    Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_LONG).show()
                                    onSignOut()
                                },
                                onFailure = { err ->
                                    showDeleteConfirmDialog = false
                                    if (err is FirebaseAuthRecentLoginRequiredException) {
                                        deleteErrorMessage = "For your security, deleting an account requires recent sign-in. Please sign out, sign back in, and try deleting again."
                                    } else {
                                        deleteErrorMessage = err.message ?: "Failed to delete account"
                                    }
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    enabled = !isDeletingAccount,
                    modifier = Modifier.testTag("confirm_delete_account_btn")
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deleting...")
                    } else {
                        Text("Yes, Delete Everything")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    enabled = !isDeletingAccount,
                    modifier = Modifier.testTag("cancel_delete_account_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
