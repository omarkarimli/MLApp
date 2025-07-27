package com.omarkarimli.mlapp.ui.presentation.ui.barcodescanning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlipCameraIos
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.omarkarimli.mlapp.domain.models.ScannedBarcode
import com.omarkarimli.mlapp.utils.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeBottomSheetContent(barcodeResults: List<ScannedBarcode>, onFlipCamera: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.PaddingMedium), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Detected Barcodes", modifier = Modifier.padding(bottom = Dimens.PaddingSmall), textAlign = TextAlign.Start, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onFlipCamera, modifier = Modifier.size(Dimens.IconSizeLarge)) {
                Icon(Icons.Rounded.FlipCameraIos, contentDescription = "Flip Camera")
            }
        }
        if (barcodeResults.isEmpty()) {
            Text("No barcodes detected yet. Scan live or pick an image.", modifier = Modifier.fillMaxWidth().padding(Dimens.PaddingSmall), textAlign = TextAlign.Start, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = Dimens.BarcodeListMaxHeight), contentPadding = PaddingValues(vertical = Dimens.PaddingExtraSmall)) {
                itemsIndexed(barcodeResults) { index, scannedBarcode ->
                    BarcodeResultCard(scannedBarcode)
                    if (index < barcodeResults.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}