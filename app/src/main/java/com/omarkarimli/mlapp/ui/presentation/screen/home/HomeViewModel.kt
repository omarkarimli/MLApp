package com.omarkarimli.mlapp.ui.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.domain.repository.RoomRepository
import com.omarkarimli.mlapp.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    val roomRepository: RoomRepository
) : ViewModel() {

    val savedResults: StateFlow<List<ResultCardModel>> =
        roomRepository.getRecentResults(Constants.RECENT_SAVED_COUNT)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Keep collecting for 5 seconds after last observer
                initialValue = emptyList()
            )
}