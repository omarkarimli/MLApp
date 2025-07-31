package com.omarkarimli.mlapp.data.repository

import android.content.SharedPreferences
import com.omarkarimli.mlapp.domain.repository.SharedPreferenceRepository
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.content.edit

@OptIn(ExperimentalGetImage::class)
class SharedPreferenceRepositoryImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : SharedPreferenceRepository {

    override suspend fun clearSharedPreferences() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit { clear() }
        }
    }

    override suspend fun saveString(key: String, value: String) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit { putString(key, value) }
        }
    }

    override suspend fun getString(key: String, defaultValue: String): String {
        return withContext(Dispatchers.IO) {
            sharedPreferences.getString(key, defaultValue) ?: defaultValue
        }
    }

    override suspend fun saveBoolean(key: String, value: Boolean) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit { putBoolean(key, value) }
        }
    }

    override suspend fun getBoolean(
        key: String,
        defaultValue: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            sharedPreferences.getBoolean(key, defaultValue)
        }
    }
}
