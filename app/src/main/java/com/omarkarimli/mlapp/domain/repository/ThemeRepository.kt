package com.omarkarimli.mlapp.domain.repository

interface ThemeRepository {
    suspend fun applyTheme(isDarkMode: Boolean)
}