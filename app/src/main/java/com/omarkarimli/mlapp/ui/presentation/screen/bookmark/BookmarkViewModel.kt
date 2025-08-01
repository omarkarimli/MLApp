package com.omarkarimli.mlapp.ui.presentation.screen.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.domain.repository.RoomRepository
import com.omarkarimli.mlapp.ui.presentation.common.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val savedResults: Flow<PagingData<ResultCardModel>> =
        searchQuery
            .debounce(300L)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                roomRepository.getPaginatedSavedResultCards(query)
            }
            .cachedIn(viewModelScope)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearAllSavedResults() {
        viewModelScope.launch {
            roomRepository.clearAllSavedResults()
        }
    }

    fun deleteSavedResult(id: Int) {
        viewModelScope.launch {
            roomRepository.deleteSavedResult(id)

            _uiState.value = UiState.Success("Item deleted successfully")
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }
}