package com.omarkarimli.mlapp.ui.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    roomRepository: RoomRepository
) : ViewModel() {

    // Expose the Flow of saved results as a StateFlow to be observed by the UI
    val savedResults: StateFlow<List<ResultCardModel>> =
        roomRepository.getAllSavedResultCards()
            .map { fullList ->
                // Apply the limit here
                fullList.take(3)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Keep collecting for 5 seconds after last observer
                initialValue = emptyList() // Initial empty list
            )
}