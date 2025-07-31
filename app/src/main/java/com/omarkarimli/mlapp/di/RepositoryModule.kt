package com.omarkarimli.mlapp.di

import com.omarkarimli.mlapp.data.repository.BarcodeScanningRepositoryImpl
import com.omarkarimli.mlapp.data.repository.FaceMeshDetectionRepositoryImpl
import com.omarkarimli.mlapp.data.repository.ImageLabelingRepositoryImpl
import com.omarkarimli.mlapp.data.repository.ObjectDetectionRepositoryImpl
import com.omarkarimli.mlapp.data.repository.PermissionRepositoryImpl
import com.omarkarimli.mlapp.data.repository.RoomRepositoryImpl
import com.omarkarimli.mlapp.data.repository.SharedPreferenceRepositoryImpl
import com.omarkarimli.mlapp.data.repository.TextRecognitionRepositoryImpl
import com.omarkarimli.mlapp.domain.repository.BarcodeScanningRepository
import com.omarkarimli.mlapp.domain.repository.FaceMeshDetectionRepository
import com.omarkarimli.mlapp.domain.repository.ImageLabelingRepository
import com.omarkarimli.mlapp.domain.repository.ObjectDetectionRepository
import com.omarkarimli.mlapp.domain.repository.PermissionRepository
import com.omarkarimli.mlapp.domain.repository.RoomRepository
import com.omarkarimli.mlapp.domain.repository.SharedPreferenceRepository
import com.omarkarimli.mlapp.domain.repository.TextRecognitionRepository
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
    abstract fun bindPermissionRepository(
        permissionRepositoryImpl: PermissionRepositoryImpl
    ): PermissionRepository

    @Binds
    @Singleton
    abstract fun bindRoomRepository(
        roomRepositoryImpl: RoomRepositoryImpl
    ): RoomRepository

    @Binds
    @Singleton
    abstract fun bindBarcodeScanningRepository(
        barcodeScanningRepositoryImpl: BarcodeScanningRepositoryImpl
    ): BarcodeScanningRepository

    @Binds
    @Singleton
    abstract fun bindTextRecognitionRepository(
        textRecognitionRepositoryImpl: TextRecognitionRepositoryImpl
    ): TextRecognitionRepository

    @Binds
    @Singleton
    abstract fun bindImageLabelingRepository(
        imageLabelingRepositoryImpl: ImageLabelingRepositoryImpl
    ): ImageLabelingRepository

    @Binds
    @Singleton
    abstract fun bindObjectDetectionRepository(
        objectDetectionRepositoryImpl: ObjectDetectionRepositoryImpl
    ): ObjectDetectionRepository

    @Binds
    @Singleton
    abstract fun bindFaceMeshDetectionRepository(
        faceMeshDetectionRepositoryImpl: FaceMeshDetectionRepositoryImpl
    ): FaceMeshDetectionRepository

    @Binds
    @Singleton
    abstract fun bindSharedPreferenceRepository(
        sharedPreferenceRepositoryImpl: SharedPreferenceRepositoryImpl
    ): SharedPreferenceRepository
}