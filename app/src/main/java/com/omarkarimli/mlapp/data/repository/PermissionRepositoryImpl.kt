package com.omarkarimli.mlapp.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.omarkarimli.mlapp.domain.repository.PermissionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionRepository {

    override fun getStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private val _cameraPermissionState = MutableStateFlow(checkPermission(Manifest.permission.CAMERA))
    override val cameraPermissionState: StateFlow<Boolean> = _cameraPermissionState

    private val _storagePermissionState = MutableStateFlow(checkPermission(getStoragePermission()))
    override val storagePermissionState: StateFlow<Boolean> = _storagePermissionState

    override fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun notifyPermissionChanged(permission: String) {
        when (permission) {
            Manifest.permission.CAMERA -> _cameraPermissionState.value = checkPermission(Manifest.permission.CAMERA)
            getStoragePermission() -> _storagePermissionState.value = checkPermission(getStoragePermission())
        }
    }
}