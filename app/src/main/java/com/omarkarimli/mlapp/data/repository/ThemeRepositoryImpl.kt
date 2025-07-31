package com.omarkarimli.mlapp.data.repository

import androidx.appcompat.app.AppCompatDelegate
import com.omarkarimli.mlapp.domain.repository.ThemeRepository
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor() : ThemeRepository {
    override suspend fun applyTheme(isDarkMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
