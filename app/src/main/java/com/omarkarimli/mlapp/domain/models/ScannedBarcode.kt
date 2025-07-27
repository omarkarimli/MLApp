package com.omarkarimli.mlapp.domain.models

import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode

data class ScannedBarcode(
    val barcode: Barcode,
    val imageUri: Uri? = null
) {
    // Override equals and hashCode to consider ScannedBarcode objects equal
    // if their underlying Barcode rawValue is the same.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScannedBarcode

        return barcode.rawValue == other.barcode.rawValue
    }

    override fun hashCode(): Int {
        return barcode.rawValue.hashCode()
    }
}