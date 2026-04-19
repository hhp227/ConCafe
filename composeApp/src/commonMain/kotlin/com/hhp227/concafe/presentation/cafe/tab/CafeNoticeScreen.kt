package com.hhp227.concafe.presentation.cafe.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.colorFromHex
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.cafe_notice_empty
import concafe.composeapp.generated.resources.noticeevent_empty_event
import concafe.composeapp.generated.resources.noticeevent_tab_event
import concafe.composeapp.generated.resources.noticeevent_tab_notice
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeNoticeScreen(
    events: List<CafeEventManagementItem>,
    notices: List<CafeNoticeManagementItem>,
    canLoadMore: Boolean,
    isLoadingMore: Boolean
) {
    var expandedNoticeIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    if (events.isEmpty() && notices.isEmpty()) {
        EmptyContent(
            text = stringResource(Res.string.cafe_notice_empty)
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.noticeevent_tab_event),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color.White else colorFromHex("1F1A22")
            )
            if (events.isNotEmpty()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        modifier = Modifier.requiredWidth(maxWidth + 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(events) { event ->
                            CafeEventCard(
                                event = event,
                                modifier = Modifier.width(276.dp)
                            )
                        }
                    }
                }
            } else {
                EmptyContent(
                    text = stringResource(Res.string.noticeevent_empty_event)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.noticeevent_tab_notice),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color.White else colorFromHex("1F1A22")
            )
            if (notices.isEmpty()) {
                EmptyContent(
                    text = stringResource(Res.string.cafe_notice_empty)
                )
            } else {
                notices.forEach { notice ->
                    NoticeCard(
                        notice = notice,
                        isExpanded = notice.id in expandedNoticeIds,
                        onToggle = {
                            expandedNoticeIds = if (notice.id in expandedNoticeIds) {
                                expandedNoticeIds - notice.id
                            } else {
                                expandedNoticeIds + notice.id
                            }
                        }
                    )
                }
                if (canLoadMore || isLoadingMore) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = colorFromHex("EF6797")
                            )
                        } else {
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CafeEventCard(
    event: CafeEventManagementItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.TopEnd
        ) {
            if (event.imageUrl.isNotBlank()) {
                BoxWithConstraints(modifier = Modifier.matchParentSize()) {
                    CompatImageDisplay(
                        imageUrl = event.imageUrl,
                        modifier = Modifier.size(maxWidth, maxHeight),
                        applyRoundedClip = false
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Brush.linearGradient(listOf(colorFromHex("FDE7EF"), colorFromHex("FCCFDF"))))
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = event.periodText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeCard(
    notice: CafeNoticeManagementItem,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onToggle,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = notice.title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = notice.displayDate,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = notice.content,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NoticeLoadMoreFooter(
    canLoadMore: Boolean,
    isLoadingMore: Boolean
) {
    if (canLoadMore || isLoadingMore) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingMore) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = colorFromHex("EF6797")
                )
            } else if (canLoadMore) {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

@Composable
private fun EmptyContent(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
