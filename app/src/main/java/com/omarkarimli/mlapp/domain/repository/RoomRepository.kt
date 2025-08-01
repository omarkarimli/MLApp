package com.omarkarimli.mlapp.domain.repository

import androidx.paging.PagingData
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    suspend fun deleteSavedResult(id: Int)
    suspend fun saveResultCard(resultCard: ResultCardModel)

    fun getPaginatedSavedResultCards(searchQuery: String): Flow<PagingData<ResultCardModel>>

    suspend fun clearAllSavedResults()

    fun getRecentResults(limit: Int): Flow<List<ResultCardModel>>
}