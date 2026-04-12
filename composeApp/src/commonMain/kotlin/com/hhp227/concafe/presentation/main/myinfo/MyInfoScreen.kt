package com.hhp227.concafe.presentation.main.myinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.presentation.component.CafeSummaryCard
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.RatingBox
import com.hhp227.concafe.presentation.navigation.NavigationAction
import com.hhp227.concafe.presentation.navigation.NavigationAction.*
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.auth_login_required_message
import concafe.composeapp.generated.resources.auth_login_required_title
import concafe.composeapp.generated.resources.common_cancel
import concafe.composeapp.generated.resources.home_show_more
import concafe.composeapp.generated.resources.home_nearby_cafe_type_butler
import concafe.composeapp.generated.resources.home_nearby_cafe_type_devil
import concafe.composeapp.generated.resources.home_nearby_cafe_type_idol
import concafe.composeapp.generated.resources.home_nearby_cafe_type_maid
import concafe.composeapp.generated.resources.myinfo_guest_feature_badge_desc
import concafe.composeapp.generated.resources.myinfo_guest_feature_badge_title
import concafe.composeapp.generated.resources.myinfo_guest_feature_bookmark_desc
import concafe.composeapp.generated.resources.myinfo_guest_feature_bookmark_title
import concafe.composeapp.generated.resources.myinfo_guest_feature_checkin_desc
import concafe.composeapp.generated.resources.myinfo_guest_feature_checkin_title
import concafe.composeapp.generated.resources.myinfo_guest_feature_membership_desc
import concafe.composeapp.generated.resources.myinfo_guest_feature_membership_title
import concafe.composeapp.generated.resources.myinfo_guest_popular_empty_desc
import concafe.composeapp.generated.resources.myinfo_guest_popular_empty_title
import concafe.composeapp.generated.resources.myinfo_guest_features_title
import concafe.composeapp.generated.resources.myinfo_guest_popular_cafes_title
import concafe.composeapp.generated.resources.myinfo_guest_start_subtitle
import concafe.composeapp.generated.resources.myinfo_guest_start_title
import concafe.composeapp.generated.resources.myinfo_guest_signin_cta
import concafe.composeapp.generated.resources.myinfo_guest_welcome_subtitle
import concafe.composeapp.generated.resources.myinfo_guest_welcome_title
import concafe.composeapp.generated.resources.signin_sign_up
import concafe.composeapp.generated.resources.signin_submit
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import org.koin.core.context.GlobalContext

@Composable
fun MyInfoScreen(
    viewModel: MyInfoViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<MyInfoViewModel>() }
        }
    ),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is MyInfoEvent.NavigateToCafe -> onNavigate(NavigateToCafe(event.id))
                is MyInfoEvent.NavigateToCast -> onNavigate(NavigateToCast(event.id))
                MyInfoEvent.NavigateToSignIn -> onNavigate(NavigateToSignIn)
            }
        }
    }
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        !uiState.isLoggedIn -> {
            GuestMyInfoScreen(
                uiState, viewModel::onAction
            )
        }
        else -> {
            ProfileMyInfoScreen(
                uiState = uiState,
                onAction = viewModel::onAction
            )
        }
    }
    if (uiState.isLoginPromptVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(MyInfoAction.DismissLoginPrompt) },
            title = { Text(stringResource(Res.string.auth_login_required_title)) },
            text = { Text(stringResource(Res.string.auth_login_required_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(MyInfoAction.ClickLoginPromptSignIn) }) {
                    Text(stringResource(Res.string.signin_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(MyInfoAction.DismissLoginPrompt) }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun GuestMyInfoScreen(
    uiState: MyInfoUiState,
    onAction: (MyInfoAction) -> Unit
) {
    val features = listOf(
        GuestFeatureItem(Icons.Filled.Place, stringResource(Res.string.myinfo_guest_feature_checkin_title), stringResource(Res.string.myinfo_guest_feature_checkin_desc), Color(0xFFEF6797), Color(0xFFF57AA8)),
        GuestFeatureItem(Icons.Filled.Favorite, stringResource(Res.string.myinfo_guest_feature_bookmark_title), stringResource(Res.string.myinfo_guest_feature_bookmark_desc), Color(0xFF9C6ADE), Color(0xFFB388EB)),
        GuestFeatureItem(Icons.Filled.Star, stringResource(Res.string.myinfo_guest_feature_badge_title), stringResource(Res.string.myinfo_guest_feature_badge_desc), Color(0xFFF0B429), Color(0xFFF5C857)),
        GuestFeatureItem(Icons.Filled.CardGiftcard, stringResource(Res.string.myinfo_guest_feature_membership_title), stringResource(Res.string.myinfo_guest_feature_membership_desc), Color(0xFF4C8BF5), Color(0xFF71A7FF))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFFEF6797), Color(0xFFF8A0C2))))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💗", style = MaterialTheme.typography.headlineLarge)
                    Text(stringResource(Res.string.myinfo_guest_welcome_title), color = Color.White, fontWeight = FontWeight.Bold)
                    Text(stringResource(Res.string.myinfo_guest_welcome_subtitle), color = Color.White.copy(alpha = 0.9f))
                    Button(
                        onClick = { onAction(MyInfoAction.ClickSignIn) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFFEF6797),
                            modifier = Modifier.size(18.dp)
                        )
                        Box(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(Res.string.myinfo_guest_signin_cta),
                            color = Color(0xFFEF6797),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item {
            MyInfoSectionTitle(stringResource(Res.string.myinfo_guest_features_title))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .height(296.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(features) { feature ->
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier
                                .height(140.dp)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(feature.startColor, feature.endColor))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(feature.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                feature.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF777777),
                                maxLines = 3,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MyInfoSectionTitle(stringResource(Res.string.myinfo_guest_popular_cafes_title))
                Row(
                    modifier = Modifier.clickable { },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.home_show_more), color = Color(0xFFEF6797), style = MaterialTheme.typography.bodySmall)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFFEF6797),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (uiState.popularCafes.isNotEmpty()) {
                    uiState.popularCafes.forEach { cafe ->
                        val resolvedThumbnail = cafe.thumbnailImage?.trim().orEmpty()
                        val ratingText = formatCafeRating(cafe.ratingAvg)
                        val imageShape = RoundedCornerShape(12.dp)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAction(MyInfoAction.ClickCafe(cafe.id)) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(imageShape)
                                        .background(Brush.verticalGradient(listOf(Color(0xFFFFE2D2), Color(0xFFFFC9A9))))
                                ) {
                                    if (resolvedThumbnail.isNotBlank()) {
                                        CompatImageDisplay(
                                            imageUrl = resolvedThumbnail,
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(imageShape),
                                            applyRoundedClip = false
                                        )
                                    }
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(cafe.name, fontWeight = FontWeight.SemiBold)
                                    RatingBox(rating = ratingText)
                                    if (cafe.conceptType.isNotBlank()) {
                                        Text(
                                            text = cafe.conceptType,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFEF6797),
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    MyInfoSectionPlaceholder(
                        title = stringResource(Res.string.myinfo_guest_popular_empty_title),
                        description = stringResource(Res.string.myinfo_guest_popular_empty_desc)
                    )
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFFFFEAF2), Color(0xFFFDE3F0))))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✨", style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(Res.string.myinfo_guest_start_title), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(Res.string.myinfo_guest_start_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7E7E7E)
                    )
                    Button(
                        onClick = {},
                        modifier = Modifier.padding(top = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6797))
                    ) {
                        Text(stringResource(Res.string.signin_sign_up), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class GuestFeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val startColor: Color,
    val endColor: Color
)

@Composable
private fun MyInfoSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2B2330)
    )
}

@Composable
private fun MyInfoSectionPlaceholder(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5C525D)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A7F8B)
            )
        }
    }
}

@Composable
private fun ProfileMyInfoScreen(
    uiState: MyInfoUiState,
    onAction: (MyInfoAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileSummaryCard(uiState = uiState)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val metricCards = myInfoMetricCards(uiState)
                metricCards.forEach { card ->
                    MyInfoMetricCard(
                        title = card.title,
                        value = card.value,
                        highlight = card.highlight
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MyInfoSectionTitle("활동 뱃지")
                Text("${uiState.badges.count { it.unlocked }} / ${uiState.badges.size}", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.badges.isNotEmpty()) {
                    uiState.badges.forEach { badge ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (badge.unlocked) Color(0xFFEF6797) else Color(0xFFDADADA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(badge.icon)
                            }
                            Text(badge.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                } else {
                    MyInfoSectionPlaceholder(
                        title = "획득한 활동 뱃지가 아직 없어요",
                        description = "체크인과 팬 활동으로 첫 뱃지를 모아보세요."
                    )
                }
            }
        }
        item {
            MyInfoSectionTitle("최근 방문")
            if (uiState.recentVisits.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(uiState.recentVisits) { cafe ->
                        val thumbnailImage = cafe.thumbnailImage?.trim().orEmpty()

                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { onAction(MyInfoAction.ClickCafe(cafe.id)) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE2D2), Color(0xFFFFC9A9))))
                            ) {
                                if (thumbnailImage.isNotBlank()) {
                                    CompatImageDisplay(
                                        imageUrl = thumbnailImage,
                                        modifier = Modifier.matchParentSize(),
                                        applyRoundedClip = false
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Image,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.82f),
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(28.dp)
                                    )
                                }
                            }
                            Text(cafe.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            } else {
                MyInfoSectionPlaceholder(
                    title = "최근 방문 기록이 없어요",
                    description = "첫 체크인을 완료하면 이곳에 방문한 카페가 표시됩니다.",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item {
            val favoriteItems = uiState.favorites.take(4)

            MyInfoSectionTitle("즐겨찾기")
            if (favoriteItems.isNotEmpty()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    val columnCount = myInfoFavoriteGridColumnCount(maxWidth)
                    val favoriteRows = favoriteItems.chunked(columnCount)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        favoriteRows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { cafe ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        CafeSummaryCard(
                                            name = cafe.name,
                                            rating = formatCafeRating(cafe.ratingAvg),
                                            conceptType = localizedCafeConceptType(cafe.conceptType),
                                            location = cafe.region.city,
                                            thumbnailImage = cafe.thumbnailImage,
                                            showLocationIcon = false,
                                            onClick = { onAction(MyInfoAction.ClickCafe(cafe.id)) }
                                        )
                                    }
                                }
                                repeat(columnCount - rowItems.size) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                MyInfoSectionPlaceholder(
                    title = "즐겨찾기한 카페가 없어요",
                    description = "좋아하는 카페를 즐겨찾기에 추가해보세요.",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item {
            if (uiState.user?.role != UserRole.CAST) {
                MyInfoSectionTitle("팔로우한 캐스트")
                if (uiState.followedMaids.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                        items(uiState.followedMaids.take(6)) { maid ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAction(MyInfoAction.ClickMaid(maid.id)) }) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (maid.profileImage.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(Brush.verticalGradient(listOf(Color(0xFFFFDFEA), Color(0xFFFFBED5))))
                                        )
                                    } else {
                                        CompatImageDisplay(
                                            imageUrl = maid.profileImage,
                                            modifier = Modifier.matchParentSize(),
                                            applyRoundedClip = false
                                        )
                                    }
                                }
                                Text(maid.name, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    MyInfoSectionPlaceholder(
                        title = "팔로우한 캐스트가 없어요",
                        description = "관심 있는 캐스트를 팔로우하면 여기서 바로 볼 수 있어요.",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(uiState: MyInfoUiState) {
    val user = uiState.user ?: return
    val castDetail = uiState.castDetail
    val ownerCafe = uiState.ownedCafes.firstOrNull()
    val title = when (user.role) {
        UserRole.CAST -> castDetail?.cast?.name ?: user.nickname
        else -> user.nickname
    }
    val subtitle = when (user.role) {
        UserRole.CAST -> castDetail?.let { detail ->
            val accountName = user.nickname.takeIf { it != detail.cast.name }.orEmpty()
            accountName
        }.orEmpty()
        UserRole.CAFE_OWNER -> "카페 운영자"
        UserRole.ADMIN -> "관리자 계정"
        UserRole.VISITOR -> "레벨 ${uiState.summary?.level ?: 1} · 열정적인 팬"
    }
    val accentText = when (user.role) {
        UserRole.CAST -> castDetail?.cafe?.name ?: "소속 카페 없음"
        UserRole.CAFE_OWNER -> ownerCafe?.name ?: "운영 카페 없음"
        UserRole.ADMIN -> "ConCafe 운영"
        UserRole.VISITOR -> "내 활동 요약"
    }
    val profileAccent = title.take(2).uppercase()
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFD7E5), Color(0xFFF2ADC2))
                        )
                    )
                    .background(Color(0xFFFFD7E5))
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profileAccent,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3F67)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF24161E)
                    )
                    if (subtitle.isNotBlank()) {
                        Box(modifier = Modifier.width(8.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7A707A)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = Color(0xFFEF6797),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = accentText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A57)
                    )
                }
            }
        }
    }
}

private data class MyInfoMetricCardModel(
    val title: String,
    val value: String,
    val highlight: Boolean
)

private fun myInfoMetricCards(uiState: MyInfoUiState): List<MyInfoMetricCardModel> {
    val user = uiState.user ?: return emptyList()

    return when (user.role) {
        UserRole.CAST -> {
            val cast = uiState.castDetail?.cast
            val scheduleCount = resolveCurrentWeekScheduleCount(uiState)
            listOf(
                MyInfoMetricCardModel("전체 팔로워", (cast?.followerCount ?: 0).toString(), false),
                MyInfoMetricCardModel("근무 일정", scheduleCount.toString(), true),
                MyInfoMetricCardModel("평점", ((cast?.rating ?: 0.0) * 10).toInt().div(10.0).toString(), false)
            )
        }
        UserRole.CAFE_OWNER -> {
            val cafeCount = uiState.ownedCafes.size
            val castCount = uiState.ownedCafes.sumOf { it.castCount }
            val rating = if (uiState.ownedCafes.isEmpty()) 0.0 else uiState.ownedCafes.map { it.rating }.average()
            listOf(
                MyInfoMetricCardModel("운영 카페", cafeCount.toString(), false),
                MyInfoMetricCardModel("소속 캐스트", castCount.toString(), true),
                MyInfoMetricCardModel("평균 평점", ((rating * 10).toInt() / 10.0).toString(), false)
            )
        }
        else -> {
            listOf(
                MyInfoMetricCardModel("방문 횟수", (uiState.summary?.totalVisits ?: 0).toString(), false),
                MyInfoMetricCardModel("즐겨찾기", uiState.favorites.size.toString(), true),
                MyInfoMetricCardModel("팔로우", (uiState.summary?.followedCastsCount ?: 0).toString(), false)
            )
        }
    }
}

private fun resolveCurrentWeekScheduleCount(uiState: MyInfoUiState): Int {
    val schedules = uiState.castDetail?.schedule.orEmpty()

    if (schedules.isEmpty()) {
        return 0
    }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val daysFromSunday = today.dayOfWeek.isoDayNumber % 7
    val weekStart = today.minus(DatePeriod(days = daysFromSunday))
    val weekEnd = weekStart.plus(DatePeriod(days = 6))
    return schedules
        .mapNotNull { schedule ->
            val normalizedDate = schedule.date.take(10)
            runCatching { LocalDate.parse(normalizedDate) }.getOrNull()
        }
        .filter { scheduleDate -> scheduleDate >= weekStart && scheduleDate <= weekEnd }
        .map { scheduleDate -> scheduleDate.toString() }
        .distinct()
        .size
}

private fun formatCafeRating(rating: Double): String {
    val roundedRating = (rating * 10).roundToInt() / 10.0
    return if (roundedRating % 1.0 == 0.0) {
        "${roundedRating.toInt()}.0"
    } else {
        roundedRating.toString()
    }
}

@Composable
private fun localizedCafeConceptType(rawConceptType: String): String {
    val normalized = rawConceptType.trim()
    if (normalized.isEmpty()) {
        return ""
    }
    return when (normalized.uppercase()) {
        "MAID" -> stringResource(Res.string.home_nearby_cafe_type_maid)
        "BUTLER" -> stringResource(Res.string.home_nearby_cafe_type_butler)
        "IDOL" -> stringResource(Res.string.home_nearby_cafe_type_idol)
        "DEVIL" -> stringResource(Res.string.home_nearby_cafe_type_devil)
        else -> normalized
    }
}

private fun myInfoFavoriteGridColumnCount(contentWidth: Dp): Int {
    val availableWidth = contentWidth.value - MYINFO_GRID_HORIZONTAL_PADDING_DP
    val minimumGridWidth = (MYINFO_GRID_MIN_CELL_WIDTH_DP * 2) + MYINFO_GRID_ITEM_SPACING_DP
    val normalizedWidth = maxOf(availableWidth, minimumGridWidth)
    val rawCount = ((normalizedWidth + MYINFO_GRID_ITEM_SPACING_DP) /
        (MYINFO_GRID_MIN_CELL_WIDTH_DP + MYINFO_GRID_ITEM_SPACING_DP)).toInt()
    return rawCount.coerceIn(MYINFO_GRID_MIN_COLUMN_COUNT, MYINFO_GRID_MAX_COLUMN_COUNT)
}

private const val MYINFO_GRID_MIN_COLUMN_COUNT = 2
private const val MYINFO_GRID_MAX_COLUMN_COUNT = 6
private const val MYINFO_GRID_HORIZONTAL_PADDING_DP = 24f
private const val MYINFO_GRID_ITEM_SPACING_DP = 12f
private const val MYINFO_GRID_MIN_CELL_WIDTH_DP = 180f

@Composable
private fun RowScope.MyInfoMetricCard(
    title: String,
    value: String,
    highlight: Boolean
) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(20.dp),
        color = if (highlight) Color(0x1AFFD1DC) else Color.White.copy(alpha = 0.92f),
        tonalElevation = if (highlight) 0.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (highlight) Color(0x33FFB3C6) else Color(0x1AFFD1DC)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7A707A),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = if (highlight) Color(0xFFD94A82) else Color(0xFF24161E),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
