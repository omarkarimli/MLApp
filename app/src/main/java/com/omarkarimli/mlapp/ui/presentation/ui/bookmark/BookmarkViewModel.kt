package com.omarkarimli.mlapp.ui.presentation.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.domain.repository.RoomRepository
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

@OptIn(FlowPreview::class) // For debounce
@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {

    // StateFlow to hold the current search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Flow of PagingData that reacts to search query changes
    @OptIn(ExperimentalCoroutinesApi::class)
    val savedResults: Flow<PagingData<ResultCardModel>> =
        searchQuery
            .debounce(300L) // Debounce search input to avoid too frequent database calls
            .distinctUntilChanged() // Only proceed if the query actually changed
            .flatMapLatest { query ->
                // When query changes, flatMapLatest re-collects from the new paging flow
                roomRepository.getPaginatedSavedResultCards(query)
            }
            .cachedIn(viewModelScope) // Caches the PagingData for configuration changes

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearAllSavedResults() {
        viewModelScope.launch {
            roomRepository.clearAllSavedResults()
        }
    }
}