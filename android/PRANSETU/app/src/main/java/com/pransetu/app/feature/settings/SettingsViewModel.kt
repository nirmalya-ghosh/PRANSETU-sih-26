package com.pransetu.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pransetu.app.core.localization.LanguagePreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val languagePreferencesRepository: LanguagePreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    
    val uiState: StateFlow<SettingsUiState> = combine(
        _uiState,
        languagePreferencesRepository.selectedLanguageFlow
    ) { state, language ->
        state.copy(currentLanguage = language)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.OnLanguageSelected -> {
                viewModelScope.launch {
                    languagePreferencesRepository.saveLanguagePreference(intent.languageCode)
                }
            }
        }
    }
}
