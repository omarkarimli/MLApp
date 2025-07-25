package com.omarkarimli.mlapp.utils

import androidx.compose.runtime.MutableState
import com.omarkarimli.mlapp.ui.presentation.ui.components.CustomToastState
import com.omarkarimli.mlapp.ui.presentation.ui.components.CustomToastType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun CoroutineScope.showCustomToast(
    toastState: MutableState<CustomToastState>,
    message: String? = null,
    type: CustomToastType,
    durationMillis: Long = 3000
) {
    this.launch {
        toastState.value = CustomToastState(message, type, true)
        delay(durationMillis)
        toastState.value = toastState.value.copy(isVisible = false)
    }
}