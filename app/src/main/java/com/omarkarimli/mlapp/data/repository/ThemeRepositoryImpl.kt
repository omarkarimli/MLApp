package com.omarkarimli.mlapp.data.repository

import androidx.appcompat.app.AppCompatDelegate
import com.omarkarimli.mlapp.domain.repository.ThemeRepository
import com.omarkarimli.mlapp.ui.theme.AppTheme
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor() : ThemeRepository {
    override suspend fun applyTheme(theme: AppTheme) {
        when (theme) {
            AppTheme.System -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            AppTheme.Light -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            AppTheme.Dark -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }
}
