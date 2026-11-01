package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DeviceViewModel
import com.example.util.configureAppCheck
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 1. Track the active Firebase Auth user state dynamically
                    val auth = FirebaseAuth.getInstance()
                    var currentUser by remember { mutableStateOf(auth.currentUser) }

                    // 2. Decide what screen to show based on login status
                    if (currentUser == null) {
                        SignInScreen(onSignInSuccess = {
                            // Update state to trigger recomposition and load the main app
                            currentUser = auth.currentUser
                        })
                    } else {
                        // User is logged in, load the main app view and its ViewModel
                        val viewModel: DeviceViewModel = viewModel()
                        HomeScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

