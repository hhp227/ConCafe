package com.hhp227.concafe.presentation.main.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.HomeCafeEvent
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ConCafeCastCard
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.auth_login_required_message
import concafe.composeapp.generated.resources.auth_login_required_title
import concafe.composeapp.generated.resources.common_cancel
import concafe.composeapp.generated.resources.home_banner_placeholder_desc
import concafe.composeapp.generated.resources.home_banner_placeholder_title
import concafe.composeapp.generated.resources.home_cast_followers
import concafe.composeapp.generated.resources.home_nearby_cafe_empty_desc
import concafe.composeapp.generated.resources.home_nearby_cafe_empty_title
import concafe.composeapp.generated.resources.home_nearby_cafe_type_butler
import concafe.composeapp.generated.resources.home_nearby_cafe_type_devil
import concafe.composeapp.generated.resources.home_nearby_cafe_type_idol
import concafe.composeapp.generated.resources.home_nearby_cafe_type_maid
import concafe.composeapp.generated.resources.home_ongoing_cafe_event_empty_desc
import concafe.composeapp.generated.resources.home_ongoing_cafe_event_empty_title
import concafe.composeapp.generated.resources.home_popular_cast_empty_desc
import concafe.composeapp.generated.resources.home_popular_cast_empty_title
import concafe.composeapp.generated.resources.home_section_birthday_cast
import concafe.composeapp.generated.resources.home_section_nearby_cafe
import concafe.composeapp.generated.resources.home_section_ongoing_cafe_event
import concafe.composeapp.generated.resources.home_section_popular_cast
import concafe.composeapp.generated.resources.home_show_more
import concafe.composeapp.generated.resources.signin_submit
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<HomeViewModel>() }
        }
    ),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { uiState.banners.size })

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is HomeEvent.NavigateToExternalLink -> onNavigate(
                    NavigationAction.NavigateToExternalLink(
                        title = event.title,
                        url = event.url
                    )
                )
                is HomeEvent.NavigateToCafe -> onNavigate(NavigationAction.NavigateToCafe(event.id))
                is HomeEvent.NavigateToCast -> onNavigate(NavigationAction.NavigateToCast(event.id))
                HomeEvent.NavigateToSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
            }
        }
    }
    LaunchedEffect(uiState.banners.size) {
        if (!uiState.banners.isEmpty()) {
            val normalizedPage = pagerState.currentPage.coerceIn(0, uiState.banners.lastIndex)

            pagerState.scrollToPage(normalizedPage)
            if (uiState.banners.size != 1) {
                while (true) {
                    delay(5000)
                    val nextPage = (pagerState.settledPage + 1) % uiState.banners.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }
    HomeContentScreen(uiState, pagerState, viewModel::onAction)
    if (uiState.isLoginPromptVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(HomeAction.DismissLoginPrompt) },
            title = { Text(stringResource(Res.string.auth_login_required_title)) },
            text = { Text(stringResource(Res.string.auth_login_required_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(HomeAction.ClickLoginPromptSignIn) }) {
                    Text(stringResource(Res.string.signin_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(HomeAction.DismissLoginPrompt) }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeContentScreen(
    uiState: HomeUiState,
    pagerState: PagerState,
    onAction: (HomeAction) -> Unit
) {
    val screenBackgroundColor = Color(0xFFFFFBFD)

    if (!uiState.isLoading) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundColor),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                HomeBannerSection(
                    uiState = uiState,
                    pagerState = pagerState,
                    onAction = onAction
                )
            }
        item {
            SectionTitle(
                text = stringResource(Res.string.home_section_popular_cast),
                actionLabel = if (uiState.canLoadMorePopularCasts) stringResource(Res.string.home_show_more) else null,
                onAction = { onAction(HomeAction.LoadMorePopularCasts) }
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                if (uiState.popularCasts.isNotEmpty()) {
                    items(uiState.popularCasts) { maid ->
                        ConCafeCastCard(
                            name = maid.name,
                            subtitle = uiState.popularCastCafeNames[maid.cafeId] ?: maid.cafeId,
                            imageUrl = maid.profileImage,
                            modifier = Modifier.width(132.dp),
                            metaText = stringResource(Res.string.home_cast_followers, maid.followerCount),
                            onClick = { onAction(HomeAction.ClickMaid(maid.id)) }
                        )
                    }
                } else {
                    item {
                        HomeSectionPlaceholderCard(
                            title = stringResource(Res.string.home_popular_cast_empty_title),
                            description = stringResource(Res.string.home_popular_cast_empty_desc),
                            modifier = Modifier.fillParentMaxWidth()
                        )
                    }
                }
            }
        }
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val contentWidth = maxWidth
                val itemWidth = nearbyCafeItemWidth(contentWidth)

                Column {
                    SectionTitle(
                        text = stringResource(Res.string.home_section_nearby_cafe),
                        actionLabel = if (uiState.canLoadMoreNearbyCafes) stringResource(Res.string.home_show_more) else null,
                        onAction = { onAction(HomeAction.LoadMoreNearbyCafes) }
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (uiState.nearbyCafes.isNotEmpty()) {
                            items(uiState.nearbyCafes) { cafe ->
                                NearByCafeItem(
                                    cafe = cafe,
                                    modifier = Modifier
                                        .width(itemWidth)
                                        .height(92.dp)
                                        .clickable { onAction(HomeAction.ClickCafe(cafe.id)) }
                                )
                            }
                        } else {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                HomeSectionPlaceholderCard(
                                    title = stringResource(Res.string.home_nearby_cafe_empty_title),
                                    description = stringResource(Res.string.home_nearby_cafe_empty_desc),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
        if (uiState.birthdayCasts.isNotEmpty()) {
            item {
                SectionTitle(stringResource(Res.string.home_section_birthday_cast))
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(uiState.birthdayCasts) { maid ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onAction(HomeAction.ClickBirthdayMaid(maid.id)) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(Color(0xFFFFD3E2), Color(0xFFFFB6D0))))
                            ) {
                                if (!maid.profileImage.isNullOrBlank()) {
                                    CompatImageDisplay(
                                        imageUrl = maid.profileImage,
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(CircleShape),
                                        applyRoundedClip = false
                                    )
                                }
                            }
                            Text(maid.name, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item {
            HomeCafeEventSection(
                events = uiState.cafeEvents,
                onAction = onAction
            )
        }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFEF6797))
        }
    }
}

@Composable
private fun HomeCafeEventSection(
    events: List<HomeCafeEvent>,
    onAction: (HomeAction) -> Unit
) {
    val visibleEvents = events.take(6)

    SectionTitle(stringResource(Res.string.home_section_ongoing_cafe_event))
    Spacer(Modifier.height(10.dp))
    if (visibleEvents.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(visibleEvents) { event ->
                HomeCafeEventCard(
                    event = event,
                    modifier = Modifier.width(276.dp),
                    onClick = { onAction(HomeAction.ClickCafe(event.cafeId)) }
                )
            }
        }
    } else {
        HomeSectionPlaceholderCard(
            title = stringResource(Res.string.home_ongoing_cafe_event_empty_title),
            description = stringResource(Res.string.home_ongoing_cafe_event_empty_desc),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun HomeCafeEventCard(
    event: HomeCafeEvent,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
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
                        .background(Brush.linearGradient(listOf(Color(0xFFFDE7EF), Color(0xFFFCCFDF))))
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = event.cafeName,
                color = Color(0xFFEF6797),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeBannerSection(
    uiState: HomeUiState,
    pagerState: PagerState,
    onAction: (HomeAction) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bannerHeight = homeBannerHeight(maxWidth)
        val bannerCount = uiState.banners.size

        LaunchedEffect(bannerCount) {
            if (bannerCount > 0) {
                val settledPage = pagerState.settledPage.coerceIn(0, bannerCount - 1)

                if (pagerState.currentPageOffsetFraction != 0f || pagerState.currentPage != settledPage) {
                    pagerState.scrollToPage(settledPage)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (uiState.banners.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bannerHeight),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    val banner = uiState.banners[page]

                    HomeBannerItem(
                        banner = banner,
                        modifier = Modifier.fillMaxSize(),
                        onClick = { onAction(HomeAction.ClickBanner(banner)) }
                    )
                }
            } else {
                // 플레이스홀더에도 동일한 높이 적용
                HomeBannerPlaceholderCard(bannerHeight)
            }
            if (uiState.banners.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(uiState.banners.size) { page ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(
                                    width = if (pagerState.currentPage == page) 18.dp else 8.dp,
                                    height = 8.dp
                                )
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (pagerState.currentPage == page) Color(0xFFEF6797)
                                    else Color(0xFFD8D8D8)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeBannerItem(
    banner: HomeBanner,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val imageUrl = banner.imageUrl?.trim().takeUnless { it.isNullOrEmpty() }
    val subtitle = banner.subtitle.trim().takeUnless { it.isEmpty() }

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            colorFromHex(banner.startColorHex),
                            colorFromHex(banner.endColorHex)
                        )
                    )
                ),
            contentAlignment = Alignment.BottomStart
        ) {
            if (imageUrl != null) {
                CompatImageDisplay(
                    imageUrl = imageUrl,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.04f),
                                    Color.Black.copy(alpha = 0.34f)
                                )
                            )
                        )
                )
            }
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = banner.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSectionPlaceholderCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
private fun HomeBannerPlaceholderCard(height: Dp) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFEDE7EA),
                            Color(0xFFF6F2F4)
                        )
                    )
                )
                .padding(18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(Res.string.home_banner_placeholder_title),
                    color = Color(0xFF6E6671),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(Res.string.home_banner_placeholder_desc),
                    color = Color(0xFF8E8794),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun homeBannerHeight(contentWidth: Dp): Dp {
    val horizontalPadding = 32.dp
    val bannerWidth = (contentWidth - horizontalPadding).coerceAtLeast(0.dp)
    return (bannerWidth * (10f / 16f)).coerceAtMost(360.dp)
}

private fun nearbyCafeItemWidth(contentWidth: Dp): Dp {
    val horizontalPadding = 16.dp
    val itemSpacing = 12.dp
    val nextItemPeekWidth = 16.dp
    return if (contentWidth >= 840.dp) {
        (contentWidth - horizontalPadding - (itemSpacing * 2) - nextItemPeekWidth) / 2
    } else {
        contentWidth - horizontalPadding - itemSpacing - nextItemPeekWidth
    }
}

@Composable
private fun SectionTitle(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val actionSlotWidth = 44.dp
    val actionSlotHeight = 24.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = actionSlotHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B2330)
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(actionSlotWidth)
                .height(actionSlotHeight),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    color = Color(0xFFEF6797),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onAction)
                )
            }
        }
    }
}

@Composable
private fun NearByCafeItem(
    cafe: Cafe,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFE1C7), Color(0xFFFFCEAE))))
        ) {
            val resolvedThumbnailImage = cafe.thumbnailImage?.trim().orEmpty()

            if (resolvedThumbnailImage.isNotBlank()) {
                CompatImageDisplay(
                    imageUrl = resolvedThumbnailImage,
                    modifier = Modifier.size(92.dp),
                    applyRoundedClip = false
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(cafe.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val conceptLabel = nearbyCafeConceptLabel(cafe.conceptType)
            if (conceptLabel.isNotEmpty()) {
                Text(
                    text = conceptLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEF6797),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(cafe.region.city, color = Color(0xFF7E7E7E), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun nearbyCafeConceptLabel(rawConceptType: String): String {
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
