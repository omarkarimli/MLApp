package com.omarkarimli.mlapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.omarkarimli.mlapp.data.local.UriConverter
import com.omarkarimli.mlapp.domain.models.ResultCardModel

@Database(entities = [ResultCardModel::class], version = 1, exportSchema = false) // Entity is ResultCardModel
@TypeConverters(UriConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resultCardDao(): ResultCardDao
}