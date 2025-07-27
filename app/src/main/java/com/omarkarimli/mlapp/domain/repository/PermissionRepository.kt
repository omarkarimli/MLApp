package com.omarkarimli.mlapp.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface PermissionRepository {
    fun getStoragePermission(): String
    fun checkPermission(permission: String): Boolean
    fun notifyPermissionChanged(permission: String)

    val cameraPermissionState: StateFlow<Boolean>
    val storagePermissionState: StateFlow<Boolean>
}