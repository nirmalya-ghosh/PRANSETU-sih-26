package com.pransetu.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pransetu.app.R
import com.pransetu.app.core.ui.components.PransetuTopAppBar

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pransetu.app.core.sos.DeliveryState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.graphicsLayer

enum class SosTimelineState {
    COMPLETED, CURRENT, PENDING
}

@Composable
fun SosStatusScreen(
    viewModel: SosStatusViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeSos = uiState.activeSos
    val context = LocalContext.current

    // Auto-revert back to Home Screen once SOS is transmitted successfully
    androidx.compose.runtime.LaunchedEffect(activeSos?.deliveryState) {
        if (activeSos != null && activeSos.deliveryState >= DeliveryState.SERVER_RECEIVED) {
            android.widget.Toast.makeText(
                context,
                "✅ Message transmitted successfully to State Emergency Operations Centre.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            kotlinx.coroutines.delay(2200)
            onNavigateBack()
        }
    }

    DisposableEffect(activeSos) {
        if (activeSos != null) {
            val vibrator = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            } catch (e: Exception) { null }

            val cameraManager = try {
                context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            } catch (e: Exception) { null }

            var cameraId: String? = null
            try {
                cameraId = cameraManager?.cameraIdList?.firstOrNull()
            } catch (e: Exception) { /* No camera available */ }

            // Strobe flashlight using a Timer (safe for background thread)
            val timer = java.util.Timer()
            var flashOn = false
            if (cameraId != null) {
                timer.scheduleAtFixedRate(object : java.util.TimerTask() {
                    override fun run() {
                        try {
                            flashOn = !flashOn
                            cameraManager?.setTorchMode(cameraId, flashOn)
                        } catch (e: Exception) { /* Camera unavailable or permission denied */ }
                    }
                }, 0, 500)
            }
            
            // SOS Morse vibration pattern — API-level safe
            try {
                val pattern = longArrayOf(0, 200, 100, 200, 100, 200, 300, 500, 100, 500, 100, 500, 300, 200, 100, 200, 100, 200, 1000)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } catch (e: Exception) { /* Vibration not supported */ }

            onDispose {
                timer.cancel()
                try { vibrator?.cancel() } catch (e: Exception) { }
                try {
                    cameraId?.let { cameraManager?.setTorchMode(it, false) }
                } catch (e: Exception) { }
            }
        } else {
            onDispose { }
        }
    }

    // UI Strobe Effect Animation
    val infiniteTransition = rememberInfiniteTransition(label = "SosStrobe")
    val strobeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StrobeAlpha"
    )

    Scaffold(
        topBar = {
            PransetuTopAppBar(
                title = stringResource(R.string.sos_status_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (activeSos == null) {
                Text("No active SOS", style = MaterialTheme.typography.titleMedium)
                return@Scaffold
            }
            
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val timeString = dateFormat.format(Date(activeSos.createdAt))
            
            // Header Card (Strobing)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(alpha = strobeAlpha),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.sos_emergency_active),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${stringResource(R.string.sos_created_at, timeString)}\n${if (activeSos.latitude != null) stringResource(R.string.sos_location_attached) else "Location not available"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onError
                    )
                    
                    if (activeSos.userMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Voice Transcript: \"${activeSos.userMessage}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(R.string.sos_delivery_status),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val state = activeSos.deliveryState
            
            // Timeline
            Column(modifier = Modifier.fillMaxWidth()) {
                TimelineItem(
                    title = stringResource(R.string.delivery_sos_created),
                    state = if (state >= DeliveryState.CREATED) SosTimelineState.COMPLETED else SosTimelineState.PENDING,
                    isLast = false
                )
                TimelineItem(
                    title = stringResource(R.string.delivery_saved_locally),
                    state = if (state >= DeliveryState.STORED) SosTimelineState.COMPLETED else if (state == DeliveryState.CREATED) SosTimelineState.CURRENT else SosTimelineState.PENDING,
                    isLast = false
                )
                TimelineItem(
                    title = stringResource(R.string.delivery_searching_relay),
                    state = if (state >= DeliveryState.RELAYING) SosTimelineState.COMPLETED else if (state == DeliveryState.QUEUED || state == DeliveryState.STORED) SosTimelineState.CURRENT else SosTimelineState.PENDING,
                    isLast = false
                )
                TimelineItem(
                    title = stringResource(R.string.delivery_backend_received),
                    state = if (state >= DeliveryState.SERVER_RECEIVED) SosTimelineState.COMPLETED else if (state == DeliveryState.GATEWAY_RECEIVED || state == DeliveryState.RELAYING) SosTimelineState.CURRENT else SosTimelineState.PENDING,
                    isLast = false
                )
                TimelineItem(
                    title = stringResource(R.string.delivery_operator_acknowledged),
                    state = if (state == DeliveryState.ACKNOWLEDGED || state == DeliveryState.CLOSED) SosTimelineState.COMPLETED else if (state == DeliveryState.SERVER_RECEIVED) SosTimelineState.CURRENT else SosTimelineState.PENDING,
                    isLast = true
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    title: String,
    state: SosTimelineState,
    isLast: Boolean
) {
    val icon: ImageVector
    val iconTint: Color
    val textColor: Color
    val fontWeight: FontWeight

    when (state) {
        SosTimelineState.COMPLETED -> {
            icon = Icons.Default.Check
            iconTint = MaterialTheme.colorScheme.primary
            textColor = MaterialTheme.colorScheme.onBackground
            fontWeight = FontWeight.Normal
        }
        SosTimelineState.CURRENT -> {
            icon = Icons.Default.MoreHoriz
            iconTint = MaterialTheme.colorScheme.secondary
            textColor = MaterialTheme.colorScheme.onBackground
            fontWeight = FontWeight.Bold
        }
        SosTimelineState.PENDING -> {
            icon = Icons.Default.RadioButtonUnchecked
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
            fontWeight = FontWeight.Normal
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (state == SosTimelineState.COMPLETED) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(
                            if (state == SosTimelineState.COMPLETED) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = fontWeight,
            color = textColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
