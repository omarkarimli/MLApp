package com.omarkarimli.mlapp.di

import android.content.Context
import android.content.SharedPreferences
import com.omarkarimli.mlapp.utils.Constants.SHARED_PREFS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
    }
}