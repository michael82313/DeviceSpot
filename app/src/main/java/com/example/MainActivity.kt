package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.DeviceRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.TwoFactorScreen
import com.example.ui.screens.TwoFactorSetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DeviceViewModel
import com.example.util.MfaSecurity
import com.example.util.configureAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

private enum class MfaScreenState {
    Loading,
    Setup,
    Verify,
    Verified
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val auth = FirebaseAuth.getInstance()
                    var currentUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
                    var mfaState by remember(currentUser?.uid) { mutableStateOf(MfaScreenState.Loading) }

                    LaunchedEffect(currentUser?.uid) {
                        val user = currentUser ?: run {
                            mfaState = MfaScreenState.Loading
                            return@LaunchedEffect
                        }

                        val context = LocalContext.current
                        val databaseId = context.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        val hasSecret = MfaSecurity.fetchTotpSecret(db, user.uid) != null
                        mfaState = if (hasSecret) MfaScreenState.Verify else MfaScreenState.Setup
                    }

                    when {
                        currentUser == null -> {
                            SignInScreen(
                                onSignInSuccess = {
                                    currentUser = auth.currentUser
                                },
                                onPasskeyVerificationCompleted = {
                                    android.util.Log.d("MainActivity", "Passkey payload: $it")
                                }
                            )
                        }
                        mfaState == MfaScreenState.Loading -> {
                            LoadingScreen()
                        }
                        mfaState == MfaScreenState.Setup -> {
                            TwoFactorSetupScreen(
                                userUid = currentUser!!.uid,
                                onSetupComplete = {
                                    mfaState = MfaScreenState.Verify
                                },
                                onBackClick = {
                                    auth.signOut()
                                    currentUser = null
                                }
                            )
                        }
                        mfaState == MfaScreenState.Verify -> {
                            TwoFactorScreen(
                                userUid = currentUser!!.uid,
                                onVerificationSuccess = {
                                    mfaState = MfaScreenState.Verified
                                },
                                onSignOut = {
                                    auth.signOut()
                                    currentUser = null
                                }
                            )
                        }
                        else -> {
                            val context = LocalContext.current
                            val databaseId = context.getString(R.string.firestore_database_id)
                            val viewModel: DeviceViewModel = viewModel(
                                key = currentUser!!.uid,
                                factory = viewModelFactory {
                                    initializer {
                                        val db = FirebaseFirestore.getInstance(databaseId)
                                        val repository = DeviceRepository(db, currentUser!!.uid)
                                        DeviceViewModel(
                                            repository = repository,
                                            currentUserId = currentUser!!.uid,
                                            userEmail = currentUser!!.email ?: "",
                                            userDisplayName = currentUser!!.displayName ?: (currentUser!!.email?.substringBefore("@") ?: "User")
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

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
