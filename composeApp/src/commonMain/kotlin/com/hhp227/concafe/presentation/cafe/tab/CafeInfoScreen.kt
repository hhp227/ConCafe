package com.hhp227.concafe.presentation.cafe.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.presentation.component.colorFromHex
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.cafe_info_action_reserve
import concafe.composeapp.generated.resources.cafe_info_label_address
import concafe.composeapp.generated.resources.cafe_info_label_business_hours
import concafe.composeapp.generated.resources.cafe_info_label_phone
import concafe.composeapp.generated.resources.cafe_info_placeholder_business_hours
import concafe.composeapp.generated.resources.cafe_info_placeholder_phone
import concafe.composeapp.generated.resources.cafe_info_section_description
import concafe.composeapp.generated.resources.cafe_info_section_social_media
import concafe.composeapp.generated.resources.social_instagram_icon
import concafe.composeapp.generated.resources.social_tiktok_icon
import concafe.composeapp.generated.resources.social_x_icon
import concafe.composeapp.generated.resources.social_youtube_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CafeInfoScreen(detail: CafeDetail) {
    val uriHandler = LocalUriHandler.current

    InfoCard(detail = detail)
    DescriptionCard(detail = detail)
    SocialMediaCard(detail = detail)
    ReservationButton(
        reservationUrl = detail.cafe.reservationUrl,
        onClick = { url -> uriHandler.openUri(url) }
    )
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
                title = stringResource(Res.string.cafe_info_label_address),
                value = detail.cafe.region.address
            )
            InfoRow(
                icon = Icons.Default.AccessTime,
                title = stringResource(Res.string.cafe_info_label_business_hours),
                value = detail.businessHours.ifBlank { stringResource(Res.string.cafe_info_placeholder_business_hours) }
            )
            InfoRow(
                icon = Icons.Default.Phone,
                title = stringResource(Res.string.cafe_info_label_phone),
                value = detail.phoneNumber.ifBlank { stringResource(Res.string.cafe_info_placeholder_phone) }
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
                text = stringResource(Res.string.cafe_info_section_description),
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
private fun SocialMediaCard(detail: CafeDetail) {
    val cafe = detail.cafe
    val uriHandler = LocalUriHandler.current
    val socialMedia = cafe.socialMedia
    val items = buildList {
        socialMedia["instagram"]?.trim()?.takeIf { it.isNotEmpty() }?.let {
            add(SocialMediaItem("Instagram", "https://instagram.com/$it", colorFromHex("E1306C"), SocialPlatform.Instagram))
        }
        socialMedia["youtube"]?.trim()?.takeIf { it.isNotEmpty() }?.let {
            add(SocialMediaItem("YouTube", "https://youtube.com/@$it", colorFromHex("FF0000"), SocialPlatform.YouTube))
        }
        socialMedia["twitter"]?.trim()?.takeIf { it.isNotEmpty() }?.let {
            add(SocialMediaItem("X", "https://x.com/$it", Color(0xFF111111), SocialPlatform.X))
        }
        socialMedia["tiktok"]?.trim()?.takeIf { it.isNotEmpty() }?.let {
            add(SocialMediaItem("TikTok", "https://tiktok.com/@$it", Color(0xFF010101), SocialPlatform.TikTok))
        }
    }

    if (items.isEmpty()) return
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
                text = stringResource(Res.string.cafe_info_section_social_media),
                fontWeight = FontWeight.SemiBold
            )
            val rows = items.chunked(2)

            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        SocialMediaChip(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onClick = { uriHandler.openUri(item.url) }
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class SocialMediaItem(
    val label: String,
    val url: String,
    val tint: Color,
    val platform: SocialPlatform
)

private enum class SocialPlatform {
    Instagram,
    YouTube,
    X,
    TikTok
}

@Composable
private fun SocialMediaChip(
    item: SocialMediaItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(color = Color(0xFFF5EDF4), shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SocialMediaPlatformIcon(platform = item.platform)
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = item.tint
            )
        }
    }
}

@Composable
private fun SocialMediaPlatformIcon(platform: SocialPlatform) {
    when (platform) {
        SocialPlatform.Instagram -> Image(
            painter = painterResource(Res.drawable.social_instagram_icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        SocialPlatform.YouTube -> Image(
            painter = painterResource(Res.drawable.social_youtube_icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        SocialPlatform.X -> Image(
            painter = painterResource(Res.drawable.social_x_icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        SocialPlatform.TikTok -> Image(
            painter = painterResource(Res.drawable.social_tiktok_icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ReservationButton(reservationUrl: String?, onClick: (String) -> Unit) {
    val isEnabled = !reservationUrl.isNullOrBlank()
    Button(
        onClick = { reservationUrl?.let { onClick(it) } },
        enabled = isEnabled,
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
            text = stringResource(Res.string.cafe_info_action_reserve),
            fontWeight = FontWeight.Bold
        )
    }
}
