package com.omarkarimli.mlapp.ui.presentation.ui.barcodescanning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.omarkarimli.mlapp.domain.models.ScannedBarcode
import com.omarkarimli.mlapp.ui.presentation.ui.components.DetectedActionImage
import com.omarkarimli.mlapp.utils.Constants
import com.omarkarimli.mlapp.utils.Dimens

@Composable
fun BarcodeResultCard(scannedBarcode: ScannedBarcode) {
    val barcode = scannedBarcode.barcode
    val imageUri = scannedBarcode.imageUri
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.PaddingExtraSmall, vertical = Dimens.PaddingMedium), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Format: ${getBarcodeFormatName(barcode.format)} (${getBarcodeValueTypeName(barcode.valueType)})", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text(barcode.rawValue ?: Constants.NOT_APPLICABLE, style = MaterialTheme.typography.bodyLarge)
        }
        DetectedActionImage(context, imageUri)
    }
}