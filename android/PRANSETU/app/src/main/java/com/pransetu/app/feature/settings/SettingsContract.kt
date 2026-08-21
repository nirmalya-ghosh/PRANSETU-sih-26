package com.pransetu.app.feature.settings

data class SettingsUiState(
    val currentLanguage: String = "en"
)

sealed interface SettingsIntent {
    data class OnLanguageSelected(val languageCode: String) : SettingsIntent
}
