package com.hhp227.concafe.presentation.cafe.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.colorFromHex

@Composable
fun CafeMenuScreen(menus: List<CafeMenu>) {
    if (menus.isEmpty()) {
        EmptyContent(text = "등록된 메뉴가 없습니다.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            menus.forEach { menu ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = if (!menu.image.isNullOrBlank()) {
                                            listOf(colorFromHex("FFD8E8"), colorFromHex("F5AFCC"))
                                        } else {
                                            listOf(colorFromHex("FFE2D2"), colorFromHex("FFC9A9"))
                                        }
                                    )
                                )
                        ) {
                            if (!menu.image.isNullOrBlank()) {
                                CompatImageDisplay(
                                    imageUrl = menu.image,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    colorFromHex("FFD8E8").copy(alpha = 0.28f),
                                                    colorFromHex("F5AFCC").copy(alpha = 0.28f)
                                                )
                                            )
                                        )
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = menu.name,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${menu.price}원",
                                color = colorFromHex("EF6797"),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = menu.desc,
                                color = Color(0xFF777777),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
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
