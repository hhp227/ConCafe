package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ConCafeCastCard(
    name: String,
    subtitle: String,
    imageUrl: String? = null,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 130.dp,
    subtitleMaxLines: Int = 1,
    containerColor: Color = Color(0xFFFFF9FC),
    containerCornerRadius: Dp = 18.dp,
    imageCornerRadius: Dp = 16.dp,
    contentPadding: Dp = 10.dp,
    metaText: String? = null,
    conceptRole: String? = null,
    attendanceStatusText: String? = null,
    isWorking: Boolean = false,
    onClick: () -> Unit
) {
    val statusText = attendanceStatusText ?: if (isWorking) "출근중" else null

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(containerCornerRadius))
            .background(containerColor)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clip(RoundedCornerShape(imageCornerRadius))
        ) {
            if (imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(colorFromHex("FFDCE8"), colorFromHex("FFC4D8"))
                            )
                        )
                )
            } else {
                CompatImageDisplay(
                    imageUrl = imageUrl,
                    modifier = Modifier
                        .matchParentSize(),
                    applyRoundedClip = false
                )
            }
            if (!statusText.isNullOrBlank() || !conceptRole.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!statusText.isNullOrBlank()) {
                        Text(
                            text = statusText,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF35B56A))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!conceptRole.isNullOrBlank()) {
                        Text(
                            text = conceptRole.uppercase(),
                            color = Color.White.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                maxLines = subtitleMaxLines,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF7E7E7E),
                style = MaterialTheme.typography.bodySmall
            )
            if (!metaText.isNullOrBlank()) {
                Text(
                    text = metaText,
                    color = Color(0xFFEF6797),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
