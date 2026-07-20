package com.hhp227.concafe.presentation.cafe.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.core.util.CastScheduleAttendanceUtils
import com.hhp227.concafe.domain.model.CafeDetailCast
import com.hhp227.concafe.domain.model.CastAttendanceStatus
import com.hhp227.concafe.presentation.cafe.CafeAction
import com.hhp227.concafe.presentation.component.ConCafeCastCard
import com.hhp227.concafe.presentation.component.ImageDisplaySize
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.component.rememberImagePrefetcher
import concafe.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.hhp227.concafe.presentation.component.ConCafeColors

@Composable
fun CafeCastScreen(
    casts: List<CafeDetailCast>,
    canLoadMore: Boolean,
    isLoadingMore: Boolean,
    onAction: (CafeAction) -> Unit
) {
    if (casts.isEmpty()) {
        EmptyContent(text = stringResource(Res.string.cafe_cast_empty))
    } else {
        val imagePrefetcher = rememberImagePrefetcher()

        LaunchedEffect(casts, imagePrefetcher) {
            imagePrefetcher.prefetch(
                imageUrls = casts.map { it.cast.profileImage }.take(24),
                displaySize = ImageDisplaySize.THUMBNAIL
            )
        }
        BoxWithConstraints {
            val gridColumnCount = cafeCastGridColumnCount(maxWidth)
            val rows = casts.chunked(gridColumnCount)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { castItem ->
                            key(castItem.cast.id) {
                                val attendanceStatus = CastScheduleAttendanceUtils.attendanceStatus(castItem.todaySchedule)

                                ConCafeCastCard(
                                    name = castItem.cast.name,
                                    subtitle = castItem.cast.desc,
                                    imageUrl = castItem.cast.profileImage,
                                    attendanceStatusText = cafeCastAttendanceStatusText(attendanceStatus),
                                    isWorking = castItem.isWorking,
                                    subtitleMaxLines = 2,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onAction(CafeAction.ClickMaid(castItem.cast.id)) }
                                )
                            }
                        }
                        repeat(gridColumnCount - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (canLoadMore) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun cafeCastGridColumnCount(contentWidth: Dp): Int {
    val availableWidth = contentWidth.value - CAFE_CAST_GRID_HORIZONTAL_PADDING_DP
    val minimumGridWidth = (CAFE_CAST_GRID_MIN_CELL_WIDTH_DP * 2) + CAFE_CAST_GRID_ITEM_SPACING_DP
    val normalizedWidth = maxOf(availableWidth, minimumGridWidth)
    val rawCount = ((normalizedWidth + CAFE_CAST_GRID_ITEM_SPACING_DP) /
        (CAFE_CAST_GRID_MIN_CELL_WIDTH_DP + CAFE_CAST_GRID_ITEM_SPACING_DP)).toInt()
    return rawCount.coerceIn(CAFE_CAST_GRID_MIN_COLUMN_COUNT, CAFE_CAST_GRID_MAX_COLUMN_COUNT)
}

private const val CAFE_CAST_GRID_MIN_COLUMN_COUNT = 2
private const val CAFE_CAST_GRID_MAX_COLUMN_COUNT = 4
private const val CAFE_CAST_GRID_HORIZONTAL_PADDING_DP = 24f
private const val CAFE_CAST_GRID_ITEM_SPACING_DP = 12f
private const val CAFE_CAST_GRID_MIN_CELL_WIDTH_DP = 180f

@Composable
private fun cafeCastAttendanceStatusText(status: CastAttendanceStatus): String? {
    return when (status) {
        CastAttendanceStatus.UPCOMING -> stringResource(Res.string.cast_today_upcoming)
        CastAttendanceStatus.ON_SHIFT -> stringResource(Res.string.cast_today_working)
        CastAttendanceStatus.COMPLETED -> stringResource(Res.string.cast_today_finished)
        CastAttendanceStatus.OFF -> null
    }
}

@Composable
private fun EmptyContent(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = ConCafeColors.primaryContainer, shape = RoundedCornerShape(24.dp))
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
