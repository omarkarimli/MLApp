package com.omarkarimli.mlapp.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultCardDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertResultCard(resultCard: ResultCardModel)

    @Query("SELECT * FROM scanned_results WHERE title LIKE '%' || :searchQuery || '%' OR subtitle LIKE '%' || :searchQuery || '%' ORDER BY id DESC")
    fun getPaginatedResultCardsPagingSource(searchQuery: String): PagingSource<Int, ResultCardModel>

    @Query("DELETE FROM scanned_results")
    suspend fun deleteAllResults()

    @Query("SELECT * FROM scanned_results ORDER BY id DESC LIMIT :limit")
    fun getRecentResults(limit: Int): Flow<List<ResultCardModel>>
}