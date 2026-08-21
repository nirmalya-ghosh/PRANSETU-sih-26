package com.pransetu.app.feature.safety

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import com.pransetu.app.R
import com.pransetu.app.core.ui.components.PransetuTopAppBar

@Composable
fun SafetyDetailScreen(
    categoryKey: String,
    onNavigateBack: () -> Unit
) {
    val titleResId = when (categoryKey) {
        "before" -> R.string.safety_before
        "during" -> R.string.safety_during
        "after" -> R.string.safety_after
        "cyclone" -> R.string.safety_cyclone
        "flood" -> R.string.safety_flood
        "earthquake" -> R.string.safety_earthquake
        "first_aid" -> R.string.safety_first_aid
        else -> R.string.title_safety
    }

    val contentResId = when (categoryKey) {
        "before" -> R.string.safety_before_content
        "during" -> R.string.safety_during_content
        "after" -> R.string.safety_after_content
        "cyclone" -> R.string.safety_cyclone_before_content // simplification for demo
        "flood" -> R.string.safety_flood_before_content
        "earthquake" -> R.string.safety_earthquake_during_content
        "first_aid" -> R.string.safety_first_aid_content
        else -> R.string.safety_before_content
    }

    Scaffold(
        topBar = {
            PransetuTopAppBar(
                title = stringResource(titleResId),
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(titleResId),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(contentResId),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
