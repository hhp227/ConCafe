package org.hhp227.concafe.presentation.cafe.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import org.hhp227.concafe.domain.model.CafeDetailCast
import org.hhp227.concafe.presentation.cafe.CafeAction
import org.hhp227.concafe.presentation.component.colorFromHex

@Composable
fun CafeCastScreen(
    casts: List<CafeDetailCast>,
    onAction: (CafeAction) -> Unit
) {
    if (casts.isEmpty()) {
        EmptyContent(text = "등록된 메이드가 없습니다.")
    } else {
        val rows = casts.chunked(2)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { castItem ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAction(CafeAction.ClickMaid(castItem.cast.id)) },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(colorFromHex("FFDFEA"), colorFromHex("FFBED5"))
                                            )
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (castItem.isWorking) {
                                            Text(
                                                text = "출근중",
                                                color = Color.White,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(Color(0xFF35B56A))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Text(
                                            text = castItem.cast.conceptRole.uppercase(),
                                            color = Color.White.copy(alpha = 0.88f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = castItem.cast.name,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = castItem.cast.desc,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color(0xFF777777),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
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