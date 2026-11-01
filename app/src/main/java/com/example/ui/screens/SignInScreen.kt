// Location: app/src/main/java/com/example/ui/screens/SignInScreen.kt

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit = {},
    onPasskeyVerificationCompleted: (credentialDataJson: String) -> Unit = {}, // 👈 Added custom passkey state delegate slot
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    
    // ... Maintain existing mutable state definitions exactly as they are ...

    // Locate the handlePasskeySignIn structure inside your file and update the try block:
    fun handlePasskeySignIn() {
        focusManager.clearFocus()
        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                
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
                
                // Extract raw verification credential data response bundle string
                val rawCredentialJsonData = result.credential.data.getString("androidx.credentials.BUNDLE_KEY_AUTHENTICATION_RESPONSE_DATA") ?: ""
                
                statusMessage = "Passkey authenticated successfully!"
                
                // 👈 Trigger your state callback loop so MainActivity can authorize the user profile mapping session
                onPasskeyVerificationCompleted(rawCredentialJsonData)
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
    
    // ... Maintain all remaining Google Sign In logic exactly as written ...


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
