package com.omarkarimli.mlapp.utils

import android.content.Context
import android.widget.Toast

// Toast
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}