package com.omarkarimli.mlapp.di

import com.omarkarimli.mlapp.data.repository.MLRepositoryImpl
import com.omarkarimli.mlapp.domain.repository.MLRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMLRepository(
        mlRepositoryImpl: MLRepositoryImpl
    ): MLRepository
}