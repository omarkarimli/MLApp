package com.omarkarimli.mlapp.ui.presentation.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {

    // Expose the Flow of saved results as a StateFlow to be observed by the UI
    val savedResults: StateFlow<List<ResultCardModel>> =
        roomRepository.getAllSavedResultCards()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Keep collecting for 5 seconds after last observer
                initialValue = emptyList() // Initial empty list
            )

    fun clearAllSavedResults() {
        viewModelScope.launch {
            roomRepository.clearAllSavedResults()
        }
    }
}