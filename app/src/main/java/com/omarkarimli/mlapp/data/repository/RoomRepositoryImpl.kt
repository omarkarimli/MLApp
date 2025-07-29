package com.omarkarimli.mlapp.data.repository

import com.omarkarimli.mlapp.data.dao.ResultCardDao
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val resultCardDao: ResultCardDao
) : RoomRepository {

    override suspend fun saveResultCard(resultCard: ResultCardModel) {
        // Since ResultCardModel is the entity, no mapping is needed here
        resultCardDao.insertResultCard(resultCard)
    }

    override fun getAllSavedResultCards(): Flow<List<ResultCardModel>> {
        // Dao already returns Flow<List<ResultCardModel>>
        return resultCardDao.getAllResultCards()
    }

    override suspend fun clearAllSavedResults() {
        resultCardDao.deleteAllResults()
    }
}