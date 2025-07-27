package com.omarkarimli.mlapp.di

import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BarcodeScanningModule {

    @Singleton
    @Provides
    fun provideBarcodeScannerOptions(): BarcodeScannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    @Singleton
    @Provides
    fun provideBarcodeScanner(): BarcodeScanner = BarcodeScanning.getClient(provideBarcodeScannerOptions())
}