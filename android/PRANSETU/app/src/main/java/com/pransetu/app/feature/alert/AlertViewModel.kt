package com.pransetu.app.feature.alert

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.pransetu.app.core.network.EmergencyAlertEngine
import com.pransetu.app.core.network.SystemAlert
import kotlinx.coroutines.flow.StateFlow

/**
 * AlertViewModel bridges the UI with the singleton EmergencyAlertEngine daemon.
 */
class AlertViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = EmergencyAlertEngine.getInstance(application.applicationContext)
    val currentAlert: StateFlow<SystemAlert?> = engine.activeAlert

    companion object {
        fun globalDismiss() {
            EmergencyAlertEngine.globalDismiss()
        }
    }

    fun dismissAlert() {
        engine.dismissActiveAlert()
    }
}
