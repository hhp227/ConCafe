package com.hhp227.concafe.presentation.cafe.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.domain.model.CafeDetailCast
import com.hhp227.concafe.presentation.cafe.CafeAction
import com.hhp227.concafe.presentation.component.ConCafeCastCard
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.cafe_cast_empty
import org.jetbrains.compose.resources.stringResource

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
        val rows = casts.chunked(2)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { castItem ->
                        ConCafeCastCard(
                            name = castItem.cast.name,
                            subtitle = castItem.cast.desc,
                            imageUrl = castItem.cast.profileImage,
                            isWorking = castItem.isWorking,
                            subtitleMaxLines = 2,
                            modifier = Modifier.weight(1f),
                            onClick = { onAction(CafeAction.ClickMaid(castItem.cast.id)) }
                        )
                    }
                    if (rowItems.size == 1) {
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
