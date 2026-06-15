package com.example.styled

import android.app.Application
import android.util.Log
import com.example.styled.network.SupabaseClientProvider

class StyledApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Supabase with credentials from BuildConfig or LocalSecrets fallback
        try {
            val supabaseUrl = if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_URL != "https://your-project.supabase.co") {
                BuildConfig.SUPABASE_URL
            } else {
                LocalSecrets.SUPABASE_URL
            }

            val supabaseAnonKey = if (BuildConfig.SUPABASE_ANON_KEY.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY != "your-anon-key-here") {
                BuildConfig.SUPABASE_ANON_KEY
            } else {
                LocalSecrets.SUPABASE_ANON_KEY
            }

            SupabaseClientProvider.initialize(supabaseUrl, supabaseAnonKey)
            Log.d("StyledApplication", "Supabase initialized with URL: $supabaseUrl")
        } catch (e: Exception) {
            Log.e("StyledApplication", "Failed to initialize Supabase", e)
        }
    }
}
