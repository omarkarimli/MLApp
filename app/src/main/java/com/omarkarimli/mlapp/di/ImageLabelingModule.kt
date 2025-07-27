package com.omarkarimli.mlapp.di

import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageLabelingModule {

    @Singleton
    @Provides
    fun provideImageLabelerOptions(): ImageLabelerOptions = ImageLabelerOptions.DEFAULT_OPTIONS

    @Singleton
    @Provides
    fun provideImageLabeler(): ImageLabeler = ImageLabeling.getClient(provideImageLabelerOptions())
}