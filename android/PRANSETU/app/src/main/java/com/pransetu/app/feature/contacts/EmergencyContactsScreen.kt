package com.pransetu.app.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pransetu.app.R
import com.pransetu.app.core.data.local.EmergencyContactEntity
import com.pransetu.app.core.ui.components.PransetuTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    viewModel: EmergencyContactsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val onCall = rememberCallHandler()

    Scaffold(
        topBar = {
            PransetuTopAppBar(
                title = stringResource(R.string.contacts_title),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.contacts_add))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.contacts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.contacts_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.contacts) { contact ->
                    ContactCard(contact = contact, onDelete = { viewModel.deleteContact(contact.id) }, onCall = onCall)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OfficialContactsCard(onCall = onCall)
            }
        }

        if (showAddDialog) {
            AddContactDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, phone, relationship ->
                    viewModel.addContact(name, phone, relationship)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun rememberCallHandler(): (String) -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingNumber?.let { rawNumber ->
            val cleanNumber = rawNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
            val uri = android.net.Uri.fromParts("tel", cleanNumber, null)
            
            if (isGranted) {
                try {
                    val callIntent = android.content.Intent(android.content.Intent.ACTION_CALL, uri).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(callIntent)
                } catch (e: Exception) {
                    try {
                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, uri).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(dialIntent)
                    } catch (_: Exception) {}
                }
            } else {
                try {
                    val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, uri).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialIntent)
                } catch (_: Exception) {}
            }
        }
        pendingNumber = null
    }
    
    return { rawNumber ->
        val cleanNumber = rawNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        val uri = android.net.Uri.fromParts("tel", cleanNumber, null)
        
        val isShortEmergencyCode = cleanNumber.length <= 5 || 
            cleanNumber in listOf("112", "100", "101", "102", "108", "1070", "1077", "181", "1091", "1098", "14567", "104", "1073", "1554", "1930")
        
        if (isShortEmergencyCode) {
            // Android OS blocks ACTION_CALL for emergency shortcodes; ACTION_DIAL is required
            try {
                val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, uri).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
            } catch (e: Exception) {
                try {
                    val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(viewIntent)
                } catch (_: Exception) {}
            }
        } else {
            // Personal contacts: Direct call if CALL_PHONE is granted, else request runtime permission
            val hasCallPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            if (hasCallPermission) {
                try {
                    val callIntent = android.content.Intent(android.content.Intent.ACTION_CALL, uri).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(callIntent)
                } catch (e: Exception) {
                    try {
                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, uri).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(dialIntent)
                    } catch (_: Exception) {}
                }
            } else {
                pendingNumber = cleanNumber
                launcher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }
}

@Composable
fun ContactCard(contact: EmergencyContactEntity, onDelete: () -> Unit, onCall: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onCall(contact.phoneNumber)
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.relationship,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Contact",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, relationship: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Family") } // Default

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.contacts_add)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.contacts_name_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.contacts_phone_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, phone, relationship) },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun OfficialContactsCard(onCall: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Official Emergency Helplines (Tap to Call)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            val officialHelplines = listOf(
                Pair("112", "National Emergency Response (Police / Fire / Medical)"),
                Pair("1070", "OSDMA Odisha State Disaster Control Room"),
                Pair("1077", "District Disaster Management Control"),
                Pair("108", "Emergency Medical Ambulance Service"),
                Pair("102", "Janani/Patient Transport Ambulance"),
                Pair("101", "Odisha Fire & Emergency Services"),
                Pair("100", "Police Control Room"),
                Pair("181", "Women Helpline"),
                Pair("1091", "Women in Distress Helpline"),
                Pair("1098", "Child Helpline"),
                Pair("14567", "Senior Citizen Helpline"),
                Pair("104", "Health Helpline (General Counselling)"),
                Pair("1073", "Road Accident Helpline"),
                Pair("1554", "Coast Guard Search & Rescue"),
                Pair("1930", "National Cyber Crime Helpline")
            )

            officialHelplines.forEach { (number, label) ->
                Surface(
                    onClick = { onCall(number) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "📞 $number",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
