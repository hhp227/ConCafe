package com.hhp227.concafe.presentation.cafe.tab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.presentation.component.colorFromHex

@Composable
fun CafeInfoScreen(detail: CafeDetail) {
    InfoCard(detail = detail)
    DescriptionCard(detail = detail)
    ReservationButton()
}

@Composable
private fun InfoCard(detail: CafeDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InfoRow(
                icon = Icons.Default.LocationOn,
                title = "주소",
                value = detail.cafe.region.address
            )
            InfoRow(
                icon = Icons.Default.AccessTime,
                title = "영업시간",
                value = detail.businessHours.ifBlank { "운영시간 정보 준비중" }
            )
            InfoRow(
                icon = Icons.Default.Phone,
                title = "전화번호",
                value = detail.phoneNumber.ifBlank { "연락처 정보 준비중" }
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorFromHex("EF6797"),
            modifier = Modifier.padding(top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                color = Color(0xFF777777)
            )
        }
    }
}

@Composable
private fun DescriptionCard(detail: CafeDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "소개",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail.cafe.desc,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun ReservationButton() {
    Button(
        onClick = {},
        enabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD1DC),
            contentColor = Color(0xFF2B2330),
            disabledContainerColor = Color(0xFFF4D7DF),
            disabledContentColor = Color(0x802B2330)
        )
    ) {
        Text(
            text = "예약하기",
            fontWeight = FontWeight.Bold
        )
    }
}
