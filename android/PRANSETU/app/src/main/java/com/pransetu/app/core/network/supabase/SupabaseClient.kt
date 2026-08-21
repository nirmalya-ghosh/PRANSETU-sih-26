package com.pransetu.app.core.network.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resilient, zero-dependency HTTP Client for Supabase PostgREST & GoTrue Auth.
 * 
 * Target Project: jdgypmmixkzamzcqdewk
 * URL: https://jdgypmmixkzamzcqdewk.supabase.co
 */
object SupabaseClient {

    private const val TAG = "SupabaseClient"

    // Project reference jdgypmmixkzamzcqdewk
    var supabaseUrl: String = "https://jdgypmmixkzamzcqdewk.supabase.co"
    var supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpkZ3lwbW1peGt6YW16Y3FkZXdrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODczMjc5NTQsImV4cCI6MjEwMjkwMzk1NH0.M_BS1bOQZ_PxblmX7zY5RJeyU6FB8kmISymHvfMityI"
    var userAccessToken: String? = null

    /**
     * Executes a POST / INSERT request to a Supabase PostgREST table.
     */
    suspend fun post(table: String, jsonPayload: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$supabaseUrl/rest/v1/$table"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", supabaseAnonKey)
                setRequestProperty("Authorization", "Bearer ${userAccessToken ?: supabaseAnonKey}")
                setRequestProperty("Prefer", "return=representation")
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }

            val responseCode = conn.responseCode
            val isSuccess = responseCode in 200..299

            val inputStream = if (isSuccess) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }

            if (isSuccess) {
                Log.d(TAG, "Supabase POST to $table succeeded ($responseCode): $responseText")
                Result.success(responseText)
            } else {
                Log.w(TAG, "Supabase POST to $table failed ($responseCode): $responseText")
                // Check if duplicate key violation (already received in database)
                if (responseText.contains("duplicate key", ignoreCase = true) || responseText.contains("already exists", ignoreCase = true)) {
                    Result.success(responseText)
                } else {
                    Result.failure(Exception("Supabase HTTP $responseCode: $responseText"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Supabase POST to $table", e)
            Result.failure(e)
        }
    }

    /**
     * Executes a GET / Query request to a Supabase PostgREST table.
     */
    suspend fun get(table: String, queryParams: String = ""): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (queryParams.isNotBlank()) "$supabaseUrl/rest/v1/$table?$queryParams" else "$supabaseUrl/rest/v1/$table"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                doInput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("apikey", supabaseAnonKey)
                setRequestProperty("Authorization", "Bearer ${userAccessToken ?: supabaseAnonKey}")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = conn.responseCode
            val isSuccess = responseCode in 200..299

            val inputStream = if (isSuccess) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }

            if (isSuccess) {
                Result.success(responseText)
            } else {
                Result.failure(Exception("Supabase GET HTTP $responseCode: $responseText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exchanges a Google OAuth ID Token with Supabase GoTrue Auth.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$supabaseUrl/auth/v1/token?grant_type=id_token"
            val url = URL(endpoint)
            val payload = JSONObject().apply {
                put("provider", "google")
                put("id_token", idToken)
            }

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", supabaseAnonKey)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val isSuccess = responseCode in 200..299

            val inputStream = if (isSuccess) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }

            if (isSuccess) {
                val json = JSONObject(responseText)
                userAccessToken = json.optString("access_token", null)
                Result.success(json)
            } else {
                Result.failure(Exception("Supabase Auth HTTP $responseCode: $responseText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
