// Location: app/src/main/java/com/example/util/SupabaseClient.kt
package com.example.util

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.gotrue

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://supabase.co",
        supabaseKey = "your-public-anon-key"
    ) {
        install(GoTrue) // Handles all 2FA & Passkey sign-ins
    }
}

