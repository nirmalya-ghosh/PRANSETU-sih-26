package com.pransetu.app.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.pransetu.app.core.network.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SupabaseAuthRepository(
    private val context: Context,
    private val supabase: SupabaseClient = SupabaseClient
) : AuthRepository {
    private val TAG = "SupabaseAuthRepository"
    private val prefs: SharedPreferences = context.getSharedPreferences("pransetu_supabase_auth", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<AuthUser?>(loadCachedUser())
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private fun loadCachedUser(): AuthUser? {
        val uid = prefs.getString("user_uid", null) ?: return null
        val email = prefs.getString("user_email", null)
        val name = prefs.getString("user_display_name", null)
        val photo = prefs.getString("user_photo_url", null)
        val token = prefs.getString("access_token", null)
        if (token != null) {
            supabase.userAccessToken = token
        }
        return AuthUser(uid = uid, email = email, displayName = name, photoUrl = photo)
    }

    private fun saveUser(user: AuthUser, accessToken: String?) {
        prefs.edit().apply {
            putString("user_uid", user.uid)
            putString("user_email", user.email)
            putString("user_display_name", user.displayName)
            putString("user_photo_url", user.photoUrl)
            putString("access_token", accessToken)
            apply()
        }
        supabase.userAccessToken = accessToken
        _currentUser.value = user
    }

    private fun parseJwtClaims(idToken: String): Map<String, String?> {
        return try {
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
                val json = JSONObject(payload)
                mapOf(
                    "sub" to json.optString("sub", null),
                    "email" to json.optString("email", null),
                    "name" to json.optString("name", null),
                    "picture" to json.optString("picture", null)
                )
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not decode JWT payload", e)
            emptyMap()
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val claims = parseJwtClaims(idToken)
            val uid = claims["sub"] ?: "user_${System.currentTimeMillis()}"
            val email = claims["email"]
            val name = claims["name"] ?: "Citizen User"
            val photo = claims["picture"]

            // Attempt Supabase GoTrue token exchange
            val supabaseResult = supabase.signInWithGoogleIdToken(idToken)
            val accessToken = if (supabaseResult.isSuccess) {
                val json = supabaseResult.getOrNull()
                json?.optString("access_token", null)
            } else {
                null
            }

            val user = AuthUser(
                uid = uid,
                email = email,
                displayName = name,
                photoUrl = photo
            )

            saveUser(user, accessToken)
            Log.d(TAG, "Supabase sign in successful for user: $name ($email)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sign in failed", e)
            Result.failure(e)
        }
    }

    override fun signOut() {
        prefs.edit().clear().apply()
        supabase.userAccessToken = null
        _currentUser.value = null
        Log.d(TAG, "User signed out from Supabase Auth")
    }
}
