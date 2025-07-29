package com.omarkarimli.mlapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omarkarimli.mlapp.domain.models.ResultCardModel // Import your ResultCardModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResultCard(resultCard: ResultCardModel) // Now inserts ResultCardModel directly

    @Query("SELECT * FROM scanned_results ORDER BY id DESC")
    fun getAllResultCards(): Flow<List<ResultCardModel>> // Returns Flow of ResultCardModel

    @Query("DELETE FROM scanned_results")
    suspend fun deleteAllResults()

    // You might add more operations like delete by id, update, etc.
}