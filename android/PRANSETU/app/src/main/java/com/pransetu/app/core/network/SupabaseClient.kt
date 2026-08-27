package com.pransetu.app.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth

object SupabaseManager {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://jdgypmmixkzamzcqdewk.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpkZ3lwbW1peGt6YW16Y3FkZXdrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODczMjc5NTQsImV4cCI6MjEwMjkwMzk1NH0.M_BS1bOQZ_PxblmX7zY5RJeyU6FB8kmISymHvfMityI"
        ) {
            install(Auth)
        }
    }
}

