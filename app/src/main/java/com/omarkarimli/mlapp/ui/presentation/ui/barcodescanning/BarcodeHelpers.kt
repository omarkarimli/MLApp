package com.omarkarimli.mlapp.ui.presentation.ui.barcodescanning

import com.google.mlkit.vision.barcode.common.Barcode

fun getBarcodeFormatName(format: Int): String = when (format) {
    Barcode.FORMAT_AZTEC -> "AZTEC"
    Barcode.FORMAT_CODABAR -> "CODABAR"
    Barcode.FORMAT_CODE_39 -> "CODE_39"
    Barcode.FORMAT_CODE_93 -> "CODE_93"
    Barcode.FORMAT_CODE_128 -> "CODE_128"
    Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
    Barcode.FORMAT_EAN_8 -> "EAN_8"
    Barcode.FORMAT_EAN_13 -> "EAN_13"
    Barcode.FORMAT_ITF -> "ITF"
    Barcode.FORMAT_QR_CODE -> "QR_CODE"
    Barcode.FORMAT_UPC_A -> "UPC_A"
    Barcode.FORMAT_UPC_E -> "UPC_E"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_ALL_FORMATS -> "ALL_FORMATS"
    else -> "UNKNOWN"
}

fun getBarcodeValueTypeName(type: Int): String = when (type) {
    Barcode.TYPE_CONTACT_INFO -> "CONTACT_INFO"
    Barcode.TYPE_EMAIL -> "EMAIL"
    Barcode.TYPE_ISBN -> "ISBN"
    Barcode.TYPE_PHONE -> "PHONE"
    Barcode.TYPE_PRODUCT -> "PRODUCT"
    Barcode.TYPE_SMS -> "SMS"
    Barcode.TYPE_TEXT -> "TEXT"
    Barcode.TYPE_URL -> "URL"
    Barcode.TYPE_WIFI -> "WIFI"
    Barcode.TYPE_GEO -> "GEO"
    Barcode.TYPE_CALENDAR_EVENT -> "CALENDAR_EVENT"
    Barcode.TYPE_DRIVER_LICENSE -> "DRIVER_LICENSE"
    else -> "UNKNOWN"
}