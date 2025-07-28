package com.omarkarimli.mlapp.utils

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast

// Toast
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.getDeviceScreenRatioDp(): Float {
    val configuration: Configuration = this.resources.configuration
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    // Handle potential division by zero to prevent crashes
    if (screenHeightDp == 0) {
        return 0f
    }

    return screenWidthDp.toFloat() / screenHeightDp.toFloat()
}