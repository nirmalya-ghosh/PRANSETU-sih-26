package com.pransetu.app.feature.onboarding

/**
 * Models all steps of the PRANSETU onboarding flow.
 */
enum class OnboardingStep {
    WELCOME,
    LANGUAGE_SELECT,
    CAPABILITY_OVERVIEW,
    PERM_LOCATION_EXPLAIN,
    PERM_NEARBY_EXPLAIN,
    PERM_NOTIFICATION_EXPLAIN,
    PERMISSIONS_GRANT,
    AUTH,
    PROFILE,
    EMERGENCY_CONTACTS,
    READY
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val selectedLanguage: String = "en",
    val userName: String = "",
    val userPhone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val locationPermissionGranted: Boolean = false,
    val nearbyPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val otpSent: Boolean = false,
    val otpVerified: Boolean = false,
    val otpError: String? = null,
    val otpMessage: String? = null,
    val verificationId: String? = null,
    val registrationComplete: Boolean = false
) {
    val stepIndex: Int get() = OnboardingStep.entries.indexOf(currentStep)
    val totalSteps: Int get() = OnboardingStep.entries.size
    val progress: Float get() = (stepIndex + 1).toFloat() / totalSteps.toFloat()

    val canGoBack: Boolean get() = currentStep != OnboardingStep.WELCOME
    val canSkip: Boolean get() = currentStep in setOf(
        OnboardingStep.PERM_LOCATION_EXPLAIN,
        OnboardingStep.PERM_NEARBY_EXPLAIN,
        OnboardingStep.PERM_NOTIFICATION_EXPLAIN,
        OnboardingStep.EMERGENCY_CONTACTS
    )
}

sealed interface OnboardingIntent {
    object Next : OnboardingIntent
    object Back : OnboardingIntent
    object Skip : OnboardingIntent
    data class SelectLanguage(val code: String) : OnboardingIntent
    data class UpdateName(val name: String) : OnboardingIntent
    data class UpdatePhone(val phone: String) : OnboardingIntent
    data class PermissionsResult(val granted: Map<String, Boolean>) : OnboardingIntent
    data class SendOtp(val activity: android.app.Activity? = null) : OnboardingIntent
    data class ResendOtp(val activity: android.app.Activity? = null) : OnboardingIntent
    object EditPhone : OnboardingIntent
    data class VerifyOtp(val code: String) : OnboardingIntent
    object FinishOnboarding : OnboardingIntent
}
