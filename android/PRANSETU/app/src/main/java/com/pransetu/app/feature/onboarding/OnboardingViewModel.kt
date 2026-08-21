package com.pransetu.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pransetu.app.core.data.local.UserProfileStore
import com.pransetu.app.core.localization.LanguageManager
import com.pransetu.app.core.localization.LanguagePreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val userProfileStore: UserProfileStore,
    private val languagePreferencesRepository: LanguagePreferencesRepository,
    private val authRepository: com.pransetu.app.core.auth.AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.Next -> moveToNextStep()
            is OnboardingIntent.Back -> moveToPreviousStep()
            is OnboardingIntent.Skip -> moveToNextStep()
            is OnboardingIntent.SelectLanguage -> selectLanguage(intent.code)
            is OnboardingIntent.UpdateName -> _uiState.update { it.copy(userName = intent.name) }
            is OnboardingIntent.UpdatePhone -> _uiState.update { it.copy(userPhone = intent.phone) }
            is OnboardingIntent.PermissionsResult -> handlePermissionsResult(intent.granted)
            is OnboardingIntent.AuthComplete -> {
                val authUser = authRepository?.currentUser?.value
                val displayName = authUser?.displayName ?: ""
                val phone = ""
                if (displayName.isNotBlank()) {
                    viewModelScope.launch {
                        userProfileStore.saveUserName(displayName)
                    }
                }
                _uiState.update {
                    it.copy(
                        isAuthComplete = true,
                        userName = if (displayName.isNotBlank()) displayName else it.userName,
                        userPhone = if (phone.isNotBlank()) phone else it.userPhone
                    )
                }
                moveToNextStep()
            }
            is OnboardingIntent.FinishOnboarding -> finishOnboarding()
        }
    }

    private fun moveToNextStep() {
        val steps = OnboardingStep.entries
        val currentIndex = steps.indexOf(_uiState.value.currentStep)
        if (currentIndex < steps.size - 1) {
            _uiState.update { it.copy(currentStep = steps[currentIndex + 1]) }
        }
    }

    private fun moveToPreviousStep() {
        val steps = OnboardingStep.entries
        val currentIndex = steps.indexOf(_uiState.value.currentStep)
        if (currentIndex > 0) {
            _uiState.update { it.copy(currentStep = steps[currentIndex - 1]) }
        }
    }

    private fun selectLanguage(code: String) {
        _uiState.update { it.copy(selectedLanguage = code) }
        viewModelScope.launch {
            languagePreferencesRepository.saveLanguagePreference(code)
            userProfileStore.saveSelectedLanguage(code)
        }
    }

    private fun handlePermissionsResult(granted: Map<String, Boolean>) {
        val locationGranted = granted.entries.any { 
            it.key.contains("LOCATION") && it.value 
        }
        val nearbyGranted = granted.entries.any { 
            (it.key.contains("BLUETOOTH") || it.key.contains("NEARBY")) && it.value 
        }
        val notificationGranted = granted.entries.any { 
            it.key.contains("NOTIFICATION") && it.value 
        }

        _uiState.update {
            it.copy(
                locationPermissionGranted = locationGranted || it.locationPermissionGranted,
                nearbyPermissionGranted = nearbyGranted || it.nearbyPermissionGranted,
                notificationPermissionGranted = notificationGranted || it.notificationPermissionGranted
            )
        }
    }

    private fun finishOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Persist profile data
            val state = _uiState.value
            if (state.userName.isNotBlank()) {
                userProfileStore.saveUserName(state.userName)
            }
            if (state.userPhone.isNotBlank()) {
                userProfileStore.saveUserPhone(state.userPhone)
            }
            userProfileStore.setOnboardingComplete(true)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun resetOnboarding() {
        _uiState.value = OnboardingUiState(
            currentStep = OnboardingStep.WELCOME,
            isAuthComplete = false,
            userName = "",
            userPhone = ""
        )
    }
}
