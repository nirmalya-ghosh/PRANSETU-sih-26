package com.pransetu.app.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pransetu.app.core.data.local.EmergencyContactDao
import com.pransetu.app.core.data.local.EmergencyContactEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EmergencyContactsUiState(
    val contacts: List<EmergencyContactEntity> = emptyList()
)

class EmergencyContactsViewModel(
    private val emergencyContactDao: EmergencyContactDao
) : ViewModel() {

    val uiState: StateFlow<EmergencyContactsUiState> = emergencyContactDao.observeAllContacts()
        .map { contacts -> EmergencyContactsUiState(contacts = contacts) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EmergencyContactsUiState()
        )

    fun addContact(name: String, phoneNumber: String, relationship: String) {
        viewModelScope.launch(Dispatchers.IO) {
            emergencyContactDao.insertContact(
                EmergencyContactEntity(
                    name = name,
                    phoneNumber = phoneNumber,
                    relationship = relationship
                )
            )
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            emergencyContactDao.deleteContact(contactId)
        }
    }
}
