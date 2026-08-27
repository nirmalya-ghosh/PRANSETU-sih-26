package com.pransetu.app.feature.alert

import android.app.Application
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pransetu.app.core.network.SystemAlert
import com.pransetu.app.core.network.SystemAlertService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertViewModel(application: Application) : AndroidViewModel(application) {

    private val alertService = SystemAlertService()
    
    private val _currentAlert = MutableStateFlow<SystemAlert?>(null)
    val currentAlert: StateFlow<SystemAlert?> = _currentAlert.asStateFlow()

    private var currentRingtone: Ringtone? = null

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            alertService.pollForAlerts(intervalMs = 10000L).collect { alert ->
                // If there's already an active alert, we could queue it, but for emergency
                // we'll just show the latest one.
                if (_currentAlert.value == null || _currentAlert.value!!.createdAt < alert.createdAt) {
                    _currentAlert.value = alert
                    triggerAlarm()
                }
            }
        }
    }

    private fun triggerAlarm() {
        val context = getApplication<Application>().applicationContext
        
        // 1. Play Alarm Sound
        try {
            var alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            currentRingtone?.stop()
            currentRingtone = ringtone
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Trigger Vibration
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 1000, 500, 1000, 500, 2000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat at index 0
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissAlert() {
        _currentAlert.value = null
        currentRingtone?.stop()
        
        // Stop vibration
        val context = getApplication<Application>().applicationContext
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
