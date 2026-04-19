package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun CafeSummaryCard(
    name: String,
    rating: String,
    conceptType: String? = null,
    location: String,
    thumbnailImage: String? = null,
    modifier: Modifier = Modifier,
    showLocationIcon: Boolean = true,
    trailingLabel: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            val resolvedThumbnailImage = thumbnailImage?.trim().orEmpty()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(colorFromHex("FFE2D2"), com.hhp227.concafe.presentation.component.colorFromHex("FFC9A9"))
                        )
                    )
            ) {
                if (resolvedThumbnailImage.isNotBlank()) {
                    CompatImageDisplay(
                        imageUrl = resolvedThumbnailImage,
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        applyRoundedClip = false
                    )
                }
                RatingBox(
                    rating = rating,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.SemiBold,
                color = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!conceptType.isNullOrBlank()) {
                Text(
                    text = conceptType,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorFromHex("EF6797"),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showLocationIcon) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (trailingLabel != null) {
                    Text(
                        text = trailingLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorFromHex("EF6797"),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
