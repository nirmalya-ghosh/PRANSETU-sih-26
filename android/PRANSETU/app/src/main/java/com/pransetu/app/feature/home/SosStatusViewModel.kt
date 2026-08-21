package com.pransetu.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pransetu.app.core.data.local.SosEntity
import com.pransetu.app.core.data.repository.RoomSosRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SosStatusUiState(
    val activeSos: SosEntity? = null
)

class SosStatusViewModel(
    private val sosRepository: RoomSosRepository
) : ViewModel() {

    val uiState: StateFlow<SosStatusUiState> = sosRepository.observeAllSos()
        .map { sosList ->
            // Just get the most recent one for now
            SosStatusUiState(activeSos = sosList.firstOrNull())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SosStatusUiState()
        )
}
