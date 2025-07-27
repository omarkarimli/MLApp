package com.omarkarimli.mlapp.di

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TextRecognitionModule {

    @Singleton
    @Provides
    fun provideTextRecognizerOptions(): TextRecognizerOptions = TextRecognizerOptions.DEFAULT_OPTIONS

    @Singleton
    @Provides
    fun provideTextRecognizer(): TextRecognizer = TextRecognition.getClient(provideTextRecognizerOptions())
}