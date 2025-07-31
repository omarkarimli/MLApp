package com.omarkarimli.mlapp.domain.repository

interface SharedPreferenceRepository {
    suspend fun clearSharedPreferences()
    suspend fun saveString(key: String, value: String)
    suspend fun getString(key: String, defaultValue: String): String
    suspend fun saveBoolean(key: String, value: Boolean)
    suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean
}
