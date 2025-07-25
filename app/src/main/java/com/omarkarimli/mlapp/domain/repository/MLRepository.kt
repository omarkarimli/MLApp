package com.omarkarimli.mlapp.domain.repository

import android.content.Context
import com.omarkarimli.mlapp.domain.models.ScannedBarcode
import kotlinx.coroutines.flow.Flow

interface MLRepository {
    fun checkCameraPermission(context: Context): Boolean
    fun checkStoragePermission(context: Context): Boolean
}