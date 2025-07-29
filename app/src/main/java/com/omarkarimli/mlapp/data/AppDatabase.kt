package com.omarkarimli.mlapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.omarkarimli.mlapp.data.converters.UriConverter
import com.omarkarimli.mlapp.data.dao.ResultCardDao
import com.omarkarimli.mlapp.domain.models.ResultCardModel // Import your ResultCardModel

@Database(entities = [ResultCardModel::class], version = 1, exportSchema = false) // Entity is ResultCardModel
@TypeConverters(UriConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resultCardDao(): ResultCardDao
}