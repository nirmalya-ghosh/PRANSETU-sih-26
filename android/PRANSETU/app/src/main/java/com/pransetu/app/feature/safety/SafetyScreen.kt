package com.pransetu.app.feature.safety

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pransetu.app.R
import com.pransetu.app.core.ui.components.PransetuTopAppBar

data class SafetyCategory(
    val key: String,
    @StringRes val titleResId: Int,
    @StringRes val subtitleResId: Int
)

@Composable
fun SafetyScreen(onNavigateToDetail: (String) -> Unit = {}) {
    val categories = listOf(
        SafetyCategory("before", R.string.safety_before, R.string.safety_before_desc),
        SafetyCategory("during", R.string.safety_during, R.string.safety_during_desc),
        SafetyCategory("after", R.string.safety_after, R.string.safety_after_desc),
        SafetyCategory("cyclone", R.string.safety_cyclone, R.string.safety_cyclone_desc),
        SafetyCategory("flood", R.string.safety_flood, R.string.safety_flood_desc),
        SafetyCategory("earthquake", R.string.safety_earthquake, R.string.safety_earthquake_desc),
        SafetyCategory("first_aid", R.string.safety_first_aid, R.string.safety_first_aid_desc)
    )

    Scaffold(
        topBar = {
            PransetuTopAppBar(
                title = stringResource(R.string.title_safety)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                SafetyCategoryCard(category, onClick = { onNavigateToDetail(category.key) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCategoryCard(category: SafetyCategory, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(category.titleResId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(category.subtitleResId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
