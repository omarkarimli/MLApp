package com.omarkarimli.mlapp.di

import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FaceMeshModule {

    @Singleton
    @Provides
    fun provideFaceMeshDetectorOptions(): FaceMeshDetectorOptions = FaceMeshDetectorOptions.Builder().build()

    @Singleton
    @Provides
    fun provideFaceMeshDetector(): FaceMeshDetector = FaceMeshDetection.getClient(provideFaceMeshDetectorOptions())
}