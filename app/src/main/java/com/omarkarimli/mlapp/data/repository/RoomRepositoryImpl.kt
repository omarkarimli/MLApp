package com.omarkarimli.mlapp.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.omarkarimli.mlapp.data.local.ResultCardDao
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.domain.repository.RoomRepository
import com.omarkarimli.mlapp.utils.Constants
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val resultCardDao: ResultCardDao
) : RoomRepository {

    override suspend fun saveResultCard(resultCard: ResultCardModel) {
        resultCardDao.insertResultCard(resultCard)
    }

    override fun getPaginatedSavedResultCards(searchQuery: String): Flow<PagingData<ResultCardModel>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.MAX_PAGE_CHILD_COUNT, // Number of items per page
                enablePlaceholders = true, // Shows placeholders while loading
                initialLoadSize = Constants.MAX_PAGE_CHILD_COUNT // Initial items loaded
            ),
            // Pass the searchQuery to the PagingSource factory
            pagingSourceFactory = { resultCardDao.getPaginatedResultCardsPagingSource(searchQuery) }
        ).flow
    }

    override suspend fun clearAllSavedResults() {
        resultCardDao.deleteAllResults()
    }

    override fun getRecentResults(limit: Int): Flow<List<ResultCardModel>> {
        return resultCardDao.getRecentResults(limit)
    }

    override suspend fun deleteSavedResult(id: Int) {
        resultCardDao.deleteResultCard(id)
    }
}