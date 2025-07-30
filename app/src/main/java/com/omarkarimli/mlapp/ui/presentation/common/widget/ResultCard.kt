package com.omarkarimli.mlapp.ui.presentation.common.widget

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
import androidx.compose.ui.text.style.TextOverflow
import com.omarkarimli.mlapp.domain.models.ResultCardModel
import com.omarkarimli.mlapp.utils.Dimens

@Composable
fun ResultCard(resultCardModel: ResultCardModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingExtraSmall, vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Dimens.PaddingMedium)
        ) {
            Text(
                resultCardModel.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                resultCardModel.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DetectedActionImage(resultCardModel.imageUri)
    }
}