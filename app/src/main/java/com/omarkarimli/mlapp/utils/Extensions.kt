package com.omarkarimli.mlapp.utils

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import com.google.mlkit.vision.barcode.common.Barcode
import com.omarkarimli.mlapp.domain.models.ImageLabelResult
import com.omarkarimli.mlapp.domain.models.RecognizedText
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.domain.models.ScannedBarcode
import com.omarkarimli.mlapp.domain.models.ScannedFaceMesh
import com.omarkarimli.mlapp.domain.models.ScannedObject

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

fun Int.getBarcodeFormatName(): String = when (this) {
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

fun Int.getBarcodeValueTypeName(): String = when (this) {
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

@JvmName("scannedBarcodesToResultCards")
fun List<ScannedBarcode>.toResultCards(): List<ResultCardModel> {
    return this.map { scannedBarcode ->
        ResultCardModel(
            title = scannedBarcode.barcode.rawValue ?: Constants.NOT_APPLICABLE,
            subtitle = "${scannedBarcode.barcode.valueType.getBarcodeValueTypeName()}: ${scannedBarcode.barcode.format.getBarcodeFormatName()}",
            imageUri = scannedBarcode.imageUri
        )
    }
}

@JvmName("imageLabelResultsToResultCards")
fun List<ImageLabelResult>.toResultCards(): List<ResultCardModel> {
    return this.map { imageLabelResult ->
        ResultCardModel(
            title = imageLabelResult.label.text,
            subtitle = "Confidence: ${imageLabelResult.label.confidence}",
            imageUri = imageLabelResult.imageUri
        )
    }
}

@JvmName("recognizedTextsToResultCards")
fun List<RecognizedText>.toResultCards(): List<ResultCardModel> {
    return this.map {
        ResultCardModel(
            title = it.text,
            subtitle = "Text:",
            imageUri = it.imageUri
        )
    }
}

@JvmName("scannedFaceMeshesToResultCards")
fun List<ScannedFaceMesh>.toResultCards(): List<ResultCardModel> {
    return this.map { scannedFaceMesh ->
        val title = "${scannedFaceMesh.faceMesh.boundingBox.width()}x${scannedFaceMesh.faceMesh.boundingBox.height()}"
        val subtitle = "${scannedFaceMesh.faceMesh.allPoints.size} points"

        ResultCardModel(
            title = title,
            subtitle = subtitle,
            imageUri = scannedFaceMesh.imageUri
        )
    }
}

@JvmName("scannedObjectsToResultCards")
fun List<ScannedObject>.toResultCards(): List<ResultCardModel> {
    return this.map { scannedObject ->
        val title: String = scannedObject.detectedObject.labels.firstOrNull()?.text ?: Constants.NOT_APPLICABLE
        val subtitle = "${scannedObject.detectedObject.boundingBox.width()}x${scannedObject.detectedObject.boundingBox.height()}"

        ResultCardModel(
            title = title,
            subtitle = subtitle,
            imageUri = scannedObject.imageUri
        )
    }
}
