package com.pransetu.app.feature.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pransetu.app.core.network.SystemAlert

@Composable
fun SystemAlertDialog(
    alert: SystemAlert,
    onDismiss: () -> Unit
) {
    val backgroundColor = when (alert.severityCode) {
        5 -> Color(0xFFB3261E) // Red
        4 -> Color(0xFFF9A825) // Orange
        else -> Color(0xFFFBC02D) // Yellow
    }
    
    val severityText = when (alert.severityCode) {
        5 -> "CRITICAL ALERT"
        4 -> "WARNING"
        else -> "ADVISORY"
    }

    Dialog(
        onDismissRequest = { /* User MUST click the button to dismiss */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false // Make it truly full screen
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = Color.White,
                    modifier = Modifier.size(100.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = severityText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = alert.message,
                    color = Color.White,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(64.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = backgroundColor
                    )
                ) {
                    Text(
                        text = "ACKNOWLEDGE & CONFIRM RECEIPT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}
