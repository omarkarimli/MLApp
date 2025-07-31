package com.omarkarimli.mlapp.ui.presentation.common.state

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()

    data class PermissionAction(val permission: String) : UiState()

    // Add other common UI states as needed, e.g., Success, Dialog, etc.
}