package com.pransetu.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pransetu.app.feature.home.HomeViewModel
import com.pransetu.app.feature.onboarding.OnboardingViewModel
import com.pransetu.app.feature.settings.SettingsViewModel

class AppViewModelFactory(private val application: PransetuApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                application.networkObserver,
                application.locationObserver,
                application.locationProvider,
                application.languageRepository,
                application.userProfileStore,
                application.sosRepository,
                application.nearbyConnectionsManager,
                application.batteryMonitor,
                application.authRepository
            ) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application.languageRepository) as T
        }
        if (modelClass.isAssignableFrom(com.pransetu.app.feature.alerts.AlertsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.pransetu.app.feature.alerts.AlertsViewModel(
                application.database.alertDao(),
                application.locationProvider
            ) as T
        }
        if (modelClass.isAssignableFrom(com.pransetu.app.feature.home.SosStatusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.pransetu.app.feature.home.SosStatusViewModel(application.sosRepository) as T
        }
        if (modelClass.isAssignableFrom(com.pransetu.app.feature.auth.AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.pransetu.app.feature.auth.AuthViewModel(
                application.authRepository,
                application.userProfileStore
            ) as T
        }
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(
                application.userProfileStore,
                application.languageRepository,
                application.authRepository
            ) as T
        }
        if (modelClass.isAssignableFrom(com.pransetu.app.feature.contacts.EmergencyContactsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.pransetu.app.feature.contacts.EmergencyContactsViewModel(
                application.database.emergencyContactDao()
            ) as T
        }
        if (modelClass.isAssignableFrom(com.pransetu.app.feature.history.SosHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.pransetu.app.feature.history.SosHistoryViewModel(
                application.sosRepository
            ) as T
        }
        if (modelClass.isAssignableFrom(com.pransetu.app.feature.family.FamilyCircleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.pransetu.app.feature.family.FamilyCircleViewModel(
                application.applicationContext,
                application.database.familyDao(),
                application.locationProvider,
                application.userProfileStore,
                application.nearbyConnectionsManager,
                application.batteryMonitor,
                application.authRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
