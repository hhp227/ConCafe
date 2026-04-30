package com.hhp227.concafe.presentation.main.myinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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
import com.hhp227.concafe.core.util.RatingUtils
import com.hhp227.concafe.domain.model.ProfileBadge
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.presentation.component.CafeSummaryCard
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.RatingBox
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import com.hhp227.concafe.presentation.navigation.NavigationAction.*
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.auth_login_required_message
import concafe.composeapp.generated.resources.auth_login_required_title
import concafe.composeapp.generated.resources.common_cancel
import concafe.composeapp.generated.resources.home_show_more
import concafe.composeapp.generated.resources.home_nearby_cafe_type_butler
import concafe.composeapp.generated.resources.home_nearby_cafe_type_devil
import concafe.composeapp.generated.resources.home_nearby_cafe_type_doll
import concafe.composeapp.generated.resources.home_nearby_cafe_type_idol
import concafe.composeapp.generated.resources.home_nearby_cafe_type_maid
import concafe.composeapp.generated.resources.home_nearby_cafe_type_other
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
import concafe.composeapp.generated.resources.myinfo_metric_affiliated_casts
import concafe.composeapp.generated.resources.myinfo_metric_average_rating
import concafe.composeapp.generated.resources.myinfo_metric_favorites
import concafe.composeapp.generated.resources.myinfo_metric_following
import concafe.composeapp.generated.resources.myinfo_metric_operating_cafes
import concafe.composeapp.generated.resources.myinfo_metric_rating
import concafe.composeapp.generated.resources.myinfo_metric_total_followers
import concafe.composeapp.generated.resources.myinfo_metric_visit_count
import concafe.composeapp.generated.resources.myinfo_metric_work_schedule
import concafe.composeapp.generated.resources.myinfo_profile_accent_admin
import concafe.composeapp.generated.resources.myinfo_profile_accent_visitor
import concafe.composeapp.generated.resources.myinfo_profile_affiliation_none
import concafe.composeapp.generated.resources.myinfo_profile_badges_empty_desc
import concafe.composeapp.generated.resources.myinfo_profile_badges_empty_title
import concafe.composeapp.generated.resources.myinfo_profile_favorites_empty_desc
import concafe.composeapp.generated.resources.myinfo_profile_favorites_empty_title
import concafe.composeapp.generated.resources.myinfo_profile_followed_casts_empty_desc
import concafe.composeapp.generated.resources.myinfo_profile_followed_casts_empty_title
import concafe.composeapp.generated.resources.myinfo_profile_operating_cafe_none
import concafe.composeapp.generated.resources.myinfo_profile_recent_visits_empty_desc
import concafe.composeapp.generated.resources.myinfo_profile_recent_visits_empty_title
import concafe.composeapp.generated.resources.myinfo_profile_role_admin_account
import concafe.composeapp.generated.resources.myinfo_profile_role_cafe_owner
import concafe.composeapp.generated.resources.myinfo_profile_role_visitor_level
import concafe.composeapp.generated.resources.myinfo_profile_section_badges
import concafe.composeapp.generated.resources.myinfo_profile_section_favorites
import concafe.composeapp.generated.resources.myinfo_profile_section_followed_casts
import concafe.composeapp.generated.resources.myinfo_profile_section_recent_visits
import concafe.composeapp.generated.resources.signin_sign_up
import concafe.composeapp.generated.resources.signin_submit
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
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
        GuestFeatureItem(Icons.Filled.Place, stringResource(Res.string.myinfo_guest_feature_checkin_title), stringResource(Res.string.myinfo_guest_feature_checkin_desc), colorFromHex("EF6797"), colorFromHex("F57AA8")),
        GuestFeatureItem(Icons.Filled.Favorite, stringResource(Res.string.myinfo_guest_feature_bookmark_title), stringResource(Res.string.myinfo_guest_feature_bookmark_desc), colorFromHex("9C6ADE"), colorFromHex("B388EB")),
        GuestFeatureItem(Icons.Filled.Star, stringResource(Res.string.myinfo_guest_feature_badge_title), stringResource(Res.string.myinfo_guest_feature_badge_desc), colorFromHex("F0B429"), colorFromHex("F5C857")),
        GuestFeatureItem(Icons.Filled.CardGiftcard, stringResource(Res.string.myinfo_guest_feature_membership_title), stringResource(Res.string.myinfo_guest_feature_membership_desc), colorFromHex("4C8BF5"), colorFromHex("71A7FF"))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFromHex("FFFBFD")),
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
                        .background(Brush.linearGradient(listOf(colorFromHex("EF6797"), colorFromHex("F8A0C2"))))
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colorFromHex("EF6797"),
                            modifier = Modifier.size(18.dp)
                        )
                        Box(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(Res.string.myinfo_guest_signin_cta),
                            color = colorFromHex("EF6797"),
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
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
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
                                color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text(stringResource(Res.string.home_show_more), color = colorFromHex("EF6797"), style = MaterialTheme.typography.bodySmall)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colorFromHex("EF6797"),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (uiState.popularCafes.isNotEmpty()) {
                    uiState.popularCafes.forEach { cafe ->
                        val resolvedThumbnail = cafe.thumbnailImage?.trim().orEmpty()
                        val ratingText = RatingUtils.formatOneDecimal(cafe.ratingAvg)
                        val imageShape = RoundedCornerShape(12.dp)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAction(MyInfoAction.ClickCafe(cafe.id)) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(imageShape)
                                        .background(Brush.verticalGradient(listOf(colorFromHex("FFE2D2"), colorFromHex("FFC9A9"))))
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
                                    val conceptType = localizedCafeConceptType(cafe.conceptType)

                                    if (conceptType.isNotBlank()) {
                                        Text(
                                            text = conceptType,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorFromHex("EF6797"),
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
                        .background(Brush.linearGradient(listOf(colorFromHex("FFEAF2"), colorFromHex("FDE3F0"))))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✨", style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(Res.string.myinfo_guest_start_title), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(Res.string.myinfo_guest_start_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorFromHex("7E7E7E")
                    )
                    Button(
                        onClick = {},
                        modifier = Modifier.padding(top = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorFromHex("EF6797"))
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
        color = colorFromHex("2B2330")
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .background(colorFromHex("FFFBFD")),
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
                MyInfoSectionTitle(stringResource(Res.string.myinfo_profile_section_badges))
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
                        BadgeItem(badge)
                    }
                } else {
                    MyInfoSectionPlaceholder(
                        title = stringResource(Res.string.myinfo_profile_badges_empty_title),
                        description = stringResource(Res.string.myinfo_profile_badges_empty_desc)
                    )
                }
            }
        }
        item {
            MyInfoSectionTitle(stringResource(Res.string.myinfo_profile_section_recent_visits))
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
                                    .background(Brush.verticalGradient(listOf(colorFromHex("FFE2D2"), colorFromHex("FFC9A9"))))
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
                    title = stringResource(Res.string.myinfo_profile_recent_visits_empty_title),
                    description = stringResource(Res.string.myinfo_profile_recent_visits_empty_desc),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item {
            val favoriteItems = uiState.favorites.take(12)

            MyInfoSectionTitle(stringResource(Res.string.myinfo_profile_section_favorites))
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
                                            rating = RatingUtils.formatOneDecimal(cafe.ratingAvg),
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
                    title = stringResource(Res.string.myinfo_profile_favorites_empty_title),
                    description = stringResource(Res.string.myinfo_profile_favorites_empty_desc),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item {
            if (uiState.user?.role != UserRole.CAST) {
                MyInfoSectionTitle(stringResource(Res.string.myinfo_profile_section_followed_casts))
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
                                                .background(Brush.verticalGradient(listOf(colorFromHex("FFDFEA"), colorFromHex("FFBED5"))))
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
                        title = stringResource(Res.string.myinfo_profile_followed_casts_empty_title),
                        description = stringResource(Res.string.myinfo_profile_followed_casts_empty_desc),
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
        UserRole.CAFE_OWNER -> stringResource(Res.string.myinfo_profile_role_cafe_owner)
        UserRole.ADMIN -> stringResource(Res.string.myinfo_profile_role_admin_account)
        UserRole.VISITOR -> stringResource(Res.string.myinfo_profile_role_visitor_level, uiState.summary?.level ?: 1)
    }
    val accentText = when (user.role) {
        UserRole.CAST -> castDetail?.cafe?.name ?: stringResource(Res.string.myinfo_profile_affiliation_none)
        UserRole.CAFE_OWNER -> ownerCafe?.name ?: stringResource(Res.string.myinfo_profile_operating_cafe_none)
        UserRole.ADMIN -> stringResource(Res.string.myinfo_profile_accent_admin)
        UserRole.VISITOR -> stringResource(Res.string.myinfo_profile_accent_visitor)
    }
    val profileAccent = title.take(2).uppercase()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
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
                            colors = listOf(colorFromHex("FFD7E5"), colorFromHex("F2ADC2"))
                        )
                    )
                    .background(colorFromHex("FFD7E5"))
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profileAccent,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorFromHex("7C3F67")
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle.isNotBlank()) {
                        Box(modifier = Modifier.width(8.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        tint = colorFromHex("EF6797"),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = accentText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun myInfoMetricCards(uiState: MyInfoUiState): List<MyInfoMetricCardModel> {
    val user = uiState.user ?: return emptyList()
    return when (user.role) {
        UserRole.CAST -> {
            val cast = uiState.castDetail?.cast
            val scheduleCount = resolveCurrentWeekScheduleCount(uiState)
            listOf(
                MyInfoMetricCardModel(stringResource(Res.string.myinfo_metric_total_followers), (cast?.followerCount ?: 0).toString(), false),
                MyInfoMetricCardModel(stringResource(Res.string.myinfo_metric_work_schedule), scheduleCount.toString(), true),
                MyInfoMetricCardModel(stringResource(Res.string.myinfo_metric_rating), RatingUtils.formatOneDecimalTruncated(cast?.rating ?: 0.0), false)
            )
        }
        UserRole.CAFE_OWNER -> {
            val cafeCount = uiState.ownedCafes.size
            val castCount = uiState.ownedCafes.sumOf { it.castCount }
            val rating = if (uiState.ownedCafes.isEmpty()) 0.0 else uiState.ownedCafes.map { it.rating }.average()
            listOf(
                MyInfoMetricCardModel(stringResource(Res.string.myinfo_metric_operating_cafes), cafeCount.toString(), false),
                MyInfoMetricCardModel(stringResource(Res.string.myinfo_metric_affiliated_casts), castCount.toString(), true),
                MyInfoMetricCardModel(stringResource(Res.string.myinfo_metric_average_rating), RatingUtils.formatOneDecimalTruncated(rating), false)
            )
        }
        else -> {
            listOf(
                MyInfoMetricCardModel(stringResource(Res.string.myinfo_metric_visit_count), (uiState.summary?.totalVisits ?: 0).toString(), false),
                MyInfoMetricCardModel(stringResource(Res.string.myinfo_metric_favorites), uiState.favorites.size.toString(), true),
                MyInfoMetricCardModel(stringResource(Res.string.myinfo_metric_following), (uiState.summary?.followedCastsCount ?: 0).toString(), false)
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
        "DOLL" -> stringResource(Res.string.home_nearby_cafe_type_doll)
        "OTHER" -> stringResource(Res.string.home_nearby_cafe_type_other)
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

@Composable
private fun BadgeItem(badge: ProfileBadge) {
    var showTooltip by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            delay(2000)
            showTooltip = false
        }
    }
    Box {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (badge.unlocked) colorFromHex("EF6797") else colorFromHex("DADADA"))
                    .clickable { showTooltip = true },
                contentAlignment = Alignment.Center
            ) {
                Text(badge.icon)
            }
            Text(badge.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        if (showTooltip) {
            Popup(
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset = IntOffset(
                        x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2,
                        y = anchorBounds.top - popupContentSize.height - with(density) { 8.dp.roundToPx() }
                    )
                },
                properties = PopupProperties(focusable = false),
                onDismissRequest = { showTooltip = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            badge.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${minOf(badge.currentCount, badge.goalCount)} / ${badge.goalCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (badge.unlocked) colorFromHex("EF6797") else Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
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
        color = if (highlight) Color(0x1AFFD1DC) else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = if (highlight) colorFromHex("D94A82") else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
