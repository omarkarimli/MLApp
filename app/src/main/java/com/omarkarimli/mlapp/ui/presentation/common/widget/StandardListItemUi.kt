package com.omarkarimli.mlapp.ui.presentation.common.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.omarkarimli.mlapp.domain.models.StandardListItemModel
import com.omarkarimli.mlapp.utils.Dimens

@Composable
fun StandardListItemUi(
    item: StandardListItemModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = Dimens.PaddingMedium, horizontal = Dimens.PaddingSmall)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.IconBackgroundSize)
                    .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(Dimens.CornerRadiusLarge)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(Dimens.IconSizeMedium)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.SpacerMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(Dimens.SpacerMedium))
            Icon(
                imageVector = item.endingIcon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconSizeMedium)
            )
        }
    }
}