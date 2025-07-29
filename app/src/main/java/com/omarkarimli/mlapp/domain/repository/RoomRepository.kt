package com.omarkarimli.mlapp.domain.repository

import com.omarkarimli.mlapp.domain.models.ResultCardModel
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    suspend fun saveResultCard(resultCard: ResultCardModel)
    fun getAllSavedResultCards(): Flow<List<ResultCardModel>>
    suspend fun clearAllSavedResults()
}