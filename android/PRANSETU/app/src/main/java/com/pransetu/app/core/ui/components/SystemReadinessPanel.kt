package com.pransetu.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pransetu.app.R
import com.pransetu.app.core.location.LocationStatus
import com.pransetu.app.core.network.NetworkStatus

@Composable
fun SystemReadinessPanel(
    networkStatus: NetworkStatus,
    locationStatus: LocationStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_system_readiness),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem(
                    isOk = networkStatus == NetworkStatus.Available,
                    label = if (networkStatus == NetworkStatus.Available) "Online (Internet)" else "Offline (Mesh Relay)",
                    iconOk = Icons.Default.SignalCellularAlt,
                    iconError = Icons.Default.SignalCellularOff
                )
                
                StatusItem(
                    isOk = locationStatus == LocationStatus.Available,
                    label = if (locationStatus == LocationStatus.Available) "GPS Ready" else "GPS Unavailable",
                    iconOk = Icons.Default.LocationOn,
                    iconError = Icons.Default.LocationOff
                )
            }
        }
    }
}

@Composable
private fun StatusItem(
    isOk: Boolean,
    label: String,
    iconOk: androidx.compose.ui.graphics.vector.ImageVector,
    iconError: androidx.compose.ui.graphics.vector.ImageVector
) {
    val color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val icon = if (isOk) iconOk else iconError

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
