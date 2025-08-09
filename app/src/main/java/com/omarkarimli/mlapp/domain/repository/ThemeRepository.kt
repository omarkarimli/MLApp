package com.omarkarimli.mlapp.domain.repository

import com.omarkarimli.mlapp.ui.theme.AppTheme

interface ThemeRepository {
    suspend fun applyTheme(theme: AppTheme)
}