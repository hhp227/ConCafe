package com.hhp227.concafe.presentation.cafe.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeDetailReview
import com.hhp227.concafe.presentation.cafe.CafeAction
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.colorFromHex
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.cafe_accessibility_more
import concafe.composeapp.generated.resources.cafe_review_action_delete
import concafe.composeapp.generated.resources.cafe_review_action_edit
import concafe.composeapp.generated.resources.cafe_review_action_report
import concafe.composeapp.generated.resources.cafe_review_count
import concafe.composeapp.generated.resources.cafe_review_empty
import concafe.composeapp.generated.resources.cafe_review_load_more_hint
import concafe.composeapp.generated.resources.cafe_review_verified
import org.jetbrains.compose.resources.stringResource

@Composable
fun CafeReviewScreen(
    detail: CafeDetail,
    reviews: List<CafeDetailReview>,
    canLoadMore: Boolean,
    isLoadingMore: Boolean,
    currentUserId: String? = null,
    onAction: (CafeAction) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = colorFromHex("FFC107"),
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = formatRating(detail.cafe.ratingAvg),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.cafe_review_count, detail.cafe.reviewCount),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (reviews.isEmpty()) {
            EmptyContent(text = stringResource(Res.string.cafe_review_empty))
        } else {
            reviews.forEach { review ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        var menuExpanded by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = review.userNickname,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (review.verified) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(colorFromHex("EF6797"))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = stringResource(Res.string.cafe_review_verified),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = review.createdDate,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(Res.string.cafe_accessibility_more),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        if (currentUserId != null && review.userId == currentUserId) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(Res.string.cafe_review_action_edit)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    onAction(CafeAction.EditReview(review.id))
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(Res.string.cafe_review_action_delete)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    onAction(CafeAction.DeleteReview(review.id))
                                                }
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(Res.string.cafe_review_action_report)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    onAction(CafeAction.ReportReview(review.id))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (index < review.rating.toInt()) colorFromHex("FFC107") else colorFromHex("E1E1E1"),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (review.taggedCastNames.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                review.taggedCastNames.forEach { castName ->
                                    Text(
                                        text = castName,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0x1AFFD1DC))
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        color = colorFromHex("C9527E"),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                        val reviewImageUrl = review.imageUrls.firstOrNull { imageUrl -> imageUrl.isNotBlank() }

                        if (reviewImageUrl != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = review.content,
                                    modifier = Modifier.weight(1f)
                                )
                                CompatImageDisplay(
                                    imageUrl = reviewImageUrl,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onAction(CafeAction.ClickReviewImage(reviewImageUrl)) },
                                    applyRoundedClip = false
                                )
                            }
                        } else {
                            Text(text = review.content)
                        }
                    }
                }
            }
            if (isLoadingMore) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (canLoadMore) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.cafe_review_load_more_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = colorFromHex("F0E4EA"), shape = RoundedCornerShape(24.dp))
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



private fun formatRating(rating: Double): String {
    val scaled = (rating * 10).toInt()
    val whole = scaled / 10
    val decimal = scaled % 10
    return "$whole.$decimal"
}
