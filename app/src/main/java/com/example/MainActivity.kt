package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.DeviceRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.TwoFactorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DeviceViewModel
import com.example.util.configureAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val auth = FirebaseAuth.getInstance()
                    var currentUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
                    
                    // State tracker to ensure 2FA challenges are satisfied before entering the App
                    var isMfaVerified by remember { mutableStateOf(false) }

                    DisposableEffect(auth) {
                        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                            currentUser = firebaseAuth.currentUser
                            if (firebaseAuth.currentUser == null) {
                                isMfaVerified = false
                            }
                        }
                        auth.addAuthStateListener(listener)
                        onDispose {
                            auth.removeAuthStateListener(listener)
                        }
                    }

                    val user = currentUser
                    when {
                        // Condition A: Not logged in at all -> Go to Sign In
                        user == null -> {
                            SignInScreen(
                                onSignInSuccess = {
                                    currentUser = auth.currentUser
                                },
                                onPasskeyVerificationCompleted = { credentialDataJson ->
                                    android.util.Log.d("MainActivity", "Received validated passkey payload: $credentialDataJson")
                                    // Pass this credential structural string to your backend server if needed
                                }
                            )
                        }
                        
                        // Condition B: Logged in, but needs to clear the 2FA Check
                        !isMfaVerified -> {
                            TwoFactorScreen(
                                userUid = user.uid,
                                onVerificationSuccess = {
                                    isMfaVerified = true
                                },
                                onSignOut = {
                                    auth.signOut()
                                    currentUser = null
                                }
                            )
                        }
                        
                        // Condition C: Fully authorized -> Launch core screen
                        else -> {
                            val context = LocalContext.current
                            val databaseId = context.getString(R.string.firestore_database_id)
                            val viewModel: DeviceViewModel = viewModel(
                                key = user.uid,
                                factory = viewModelFactory {
                                    initializer {
                                        val db = FirebaseFirestore.getInstance(databaseId)
                                        val repository = DeviceRepository(db, user.uid)
                                        DeviceViewModel(
                                            repository = repository,
                                            currentUserId = user.uid,
                                            userEmail = user.email ?: "",
                                            userDisplayName = user.displayName ?: (user.email?.substringBefore("@") ?: "User")
                                        )
                                    }
                                }
                            )

                            HomeScreen(
                                viewModel = viewModel,
                                onSignOut = {
                                    auth.signOut()
                                    currentUser = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
