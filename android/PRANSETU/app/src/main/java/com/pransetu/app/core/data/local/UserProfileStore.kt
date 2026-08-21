package com.pransetu.app.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore for persisting user profile and onboarding state locally.
 * Separate from the language DataStore to avoid conflicts.
 */
val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")

class UserProfileStore(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_PHONE = stringPreferencesKey("user_phone")
        private val KEY_SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    }

    val isOnboardingComplete: Flow<Boolean> = context.profileDataStore.data
        .map { it[KEY_ONBOARDING_COMPLETE] ?: false }

    val userName: Flow<String> = context.profileDataStore.data
        .map { it[KEY_USER_NAME] ?: "" }

    val userPhone: Flow<String> = context.profileDataStore.data
        .map { it[KEY_USER_PHONE] ?: "" }

    val selectedLanguage: Flow<String> = context.profileDataStore.data
        .map { it[KEY_SELECTED_LANGUAGE] ?: "en" }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.profileDataStore.edit { it[KEY_ONBOARDING_COMPLETE] = complete }
    }

    suspend fun saveUserName(name: String) {
        context.profileDataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun saveUserPhone(phone: String) {
        context.profileDataStore.edit { it[KEY_USER_PHONE] = phone }
    }

    suspend fun saveSelectedLanguage(languageCode: String) {
        context.profileDataStore.edit { it[KEY_SELECTED_LANGUAGE] = languageCode }
    }

    suspend fun clearUserProfile() {
        context.profileDataStore.edit {
            it[KEY_ONBOARDING_COMPLETE] = false
            it[KEY_USER_NAME] = ""
            it[KEY_USER_PHONE] = ""
        }
    }

    suspend fun isOnboardingCompleteSync(): Boolean {
        return context.profileDataStore.data.first()[KEY_ONBOARDING_COMPLETE] ?: false
    }
}
