package com.hhp227.concafe.presentation.cafe.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit
) {
    var expandedNoticeIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    if (events.isEmpty() && notices.isEmpty()) {
        EmptyContent(text = stringResource(Res.string.cafe_notice_empty))
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(Res.string.noticeevent_tab_event),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (events.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(events) { event ->
                        CafeEventCard(
                            event = event,
                            modifier = Modifier.width(248.dp)
                        )
                    }
                }
            } else {
                EmptyContent(text = stringResource(Res.string.noticeevent_empty_event))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.noticeevent_tab_notice),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (notices.isEmpty()) {
                EmptyContent(text = stringResource(Res.string.cafe_notice_empty))
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
                                color = Color(0xFFEF6797)
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFDE7EF), Color(0xFFFCCFDF))))
        ) {
            if (event.imageUrl.isNotBlank()) {
                CompatImageDisplay(
                    imageUrl = event.imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    applyRoundedClip = false
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
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
                    tint = Color(0xFF8A7F8B),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = event.periodText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A7F8B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = event.statusLabel,
                color = Color(0xFF7A707A),
                style = MaterialTheme.typography.labelSmall
            )
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = notice.displayDate,
                    color = Color(0xFF999999),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = notice.content,
                color = Color(0xFF666666),
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
                    color = Color(0xFFEF6797)
                )
            } else if (canLoadMore) {
                Spacer(modifier = Modifier.height(1.dp))
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
            .background(Color.White)
            .border(width = 1.dp, color = Color(0xFFF0E4EA), shape = RoundedCornerShape(24.dp))
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF777777)
        )
    }
}
