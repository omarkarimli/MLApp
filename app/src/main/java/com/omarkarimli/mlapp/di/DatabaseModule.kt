package com.omarkarimli.mlapp.di

import android.content.Context
import androidx.room.Room
import com.omarkarimli.mlapp.data.local.AppDatabase
import com.omarkarimli.mlapp.data.local.ResultCardDao
import com.omarkarimli.mlapp.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideResultCardDao(appDatabase: AppDatabase): ResultCardDao {
        return appDatabase.resultCardDao()
    }
}