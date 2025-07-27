package com.omarkarimli.mlapp.di

import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ObjectDetectionModule {

    @Singleton
    @Provides
    fun provideObjectDetectorOptions(): ObjectDetectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    @Singleton
    @Provides
    fun provideObjectDetector(): ObjectDetector = ObjectDetection.getClient(provideObjectDetectorOptions())
}