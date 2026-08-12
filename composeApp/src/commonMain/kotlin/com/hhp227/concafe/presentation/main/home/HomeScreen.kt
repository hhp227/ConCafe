package com.hhp227.concafe.presentation.main.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CommunityPost
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.HomeCafeEvent
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ConCafeCastCard
import com.hhp227.concafe.presentation.component.ImageDisplaySize
import com.hhp227.concafe.presentation.component.LazyGridImagePrefetch
import com.hhp227.concafe.presentation.component.LazyListImagePrefetch
import com.hhp227.concafe.presentation.component.ShimmerBox
import com.hhp227.concafe.presentation.component.ShimmerListItemSkeleton
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.component.localizedRegionCity
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.auth_login_required_message
import concafe.composeapp.generated.resources.auth_login_required_title
import concafe.composeapp.generated.resources.common_cancel
import concafe.composeapp.generated.resources.community_post_comment_count
import concafe.composeapp.generated.resources.community_post_like_count
import concafe.composeapp.generated.resources.home_banner_placeholder_desc
import concafe.composeapp.generated.resources.home_banner_placeholder_title
import concafe.composeapp.generated.resources.home_cast_followers
import concafe.composeapp.generated.resources.home_nearby_cafe_empty_desc
import concafe.composeapp.generated.resources.home_nearby_cafe_empty_title
import concafe.composeapp.generated.resources.home_nearby_cafe_type_butler
import concafe.composeapp.generated.resources.home_nearby_cafe_type_cat
import concafe.composeapp.generated.resources.home_nearby_cafe_type_cosplay
import concafe.composeapp.generated.resources.home_nearby_cafe_type_devil
import concafe.composeapp.generated.resources.home_nearby_cafe_type_doll
import concafe.composeapp.generated.resources.home_nearby_cafe_type_idol
import concafe.composeapp.generated.resources.home_nearby_cafe_type_maid
import concafe.composeapp.generated.resources.home_nearby_cafe_type_namjang
import concafe.composeapp.generated.resources.home_nearby_cafe_type_other
import concafe.composeapp.generated.resources.home_nearby_cafe_type_yokai
import concafe.composeapp.generated.resources.home_ongoing_cafe_event_empty_desc
import concafe.composeapp.generated.resources.home_ongoing_cafe_event_empty_title
import concafe.composeapp.generated.resources.home_popular_cast_empty_desc
import concafe.composeapp.generated.resources.home_popular_cast_empty_title
import concafe.composeapp.generated.resources.home_community_latest_title
import concafe.composeapp.generated.resources.home_community_see_all
import concafe.composeapp.generated.resources.home_section_birthday_cast
import concafe.composeapp.generated.resources.home_section_nearby_cafe
import concafe.composeapp.generated.resources.home_section_ongoing_cafe_event
import concafe.composeapp.generated.resources.home_section_popular_cast
import concafe.composeapp.generated.resources.home_show_more
import concafe.composeapp.generated.resources.maid_logo
import concafe.composeapp.generated.resources.signin_submit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import com.hhp227.concafe.presentation.component.ConCafeColors
import com.hhp227.concafe.presentation.theme.AppContentLayout
import com.hhp227.concafe.presentation.theme.horizontalPadding
import com.hhp227.concafe.presentation.theme.shape
import com.hhp227.concafe.presentation.theme.topPadding

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
                is HomeEvent.NavigateToCafeEvent -> onNavigate(
                    NavigationAction.NavigateToCafeEvent(
                        cafeId = event.cafeId,
                        eventId = event.eventId,
                        showCafeButton = true
                    )
                )
                is HomeEvent.NavigateToCast -> onNavigate(NavigationAction.NavigateToCast(event.id))
                HomeEvent.NavigateToSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
                HomeEvent.NavigateToCommunity -> onNavigate(NavigationAction.NavigateToCommunity)
                is HomeEvent.NavigateToPostDetail -> onNavigate(NavigationAction.NavigateToPostDetail(event.postId))
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
    val screenBackgroundColor = MaterialTheme.colorScheme.background

    if (!uiState.isLoading) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundColor),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(
                top = uiState.contentLayout.topPadding(FEED_TOP_PADDING),
                bottom = 20.dp
            )
        ) {
            item {
                HomeBannerSection(
                    uiState = uiState,
                    pagerState = pagerState,
                    onAction = onAction
                )
            }
            item {
                HomeCafeEventSection(
                    events = uiState.cafeEvents,
                    canLoadMore = uiState.canLoadMoreCafeEvents && uiState.cafeEvents.isNotEmpty(),
                    onAction = onAction
                )
            }
            item {
                val popularCastListState = rememberLazyListState()

                LazyListImagePrefetch(
                    state = popularCastListState,
                    imageUrls = uiState.popularCasts.map { it.profileImage },
                    aheadCount = 10,
                    displaySize = ImageDisplaySize.THUMBNAIL
                )
                SectionTitle(
                    text = stringResource(Res.string.home_section_popular_cast),
                    actionLabel = if (uiState.canLoadMorePopularCasts) stringResource(Res.string.home_show_more) else null,
                    onAction = { onAction(HomeAction.LoadMorePopularCasts) }
                )
                Spacer(Modifier.height(10.dp))
                if (uiState.popularCasts.isNotEmpty()) {
                    LazyRow(
                        state = popularCastListState,
                        flingBehavior = rememberStartSnapFlingBehavior(popularCastListState),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(uiState.popularCasts) { maid ->
                            val cafeName = uiState.popularCastCafeNames[maid.cafeId] ?: maid.cafeId
                            val cafeRegion = uiState.popularCastCafeRegions[maid.cafeId]
                            val subtitle = if (!cafeRegion.isNullOrBlank()) "$cafeName(${localizedRegionCity(cafeRegion)})" else cafeName

                            ConCafeCastCard(
                                name = maid.name,
                                subtitle = subtitle,
                                imageUrl = maid.profileImage,
                                modifier = Modifier.width(132.dp),
                                metaText = stringResource(Res.string.home_cast_followers, maid.followerCount),
                                onClick = { onAction(HomeAction.ClickMaid(maid.id)) }
                            )
                        }
                    }
                } else {
                    HomeSectionPlaceholderCard(
                        title = stringResource(Res.string.home_popular_cast_empty_title),
                        description = stringResource(Res.string.home_popular_cast_empty_desc),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
            item {
                val nearbyGridState = rememberLazyGridState()
                LazyGridImagePrefetch(
                    state = nearbyGridState,
                    imageUrls = uiState.nearbyCafes.map { it.thumbnailImage },
                    aheadCount = 12,
                    displaySize = ImageDisplaySize.THUMBNAIL
                )

                SnapLazyHorizontalGridToStartEffect(nearbyGridState)
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val contentWidth = maxWidth
                    val itemWidth = nearbyCafeItemWidth(contentWidth)

                    Column {
                        SectionTitle(
                            text = stringResource(Res.string.home_section_nearby_cafe),
                            actionLabel = if (uiState.canLoadMoreNearbyCafes && uiState.nearbyCafes.isNotEmpty()) stringResource(Res.string.home_show_more) else null,
                            onAction = { onAction(HomeAction.LoadMoreNearbyCafes) }
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(3),
                            state = nearbyGridState,
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
                                item(span = { GridItemSpan(maxLineSpan) }) {
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
            if (uiState.communityPosts.isNotEmpty()) {
                item {
                    HomeCommunitySection(
                        posts = uiState.communityPosts,
                        onAction = onAction
                    )
                }
            }
            if (uiState.birthdayCasts.isNotEmpty()) {
                item {
                    val birthdayCastListState = rememberLazyListState()

                    LazyListImagePrefetch(
                        state = birthdayCastListState,
                        imageUrls = uiState.birthdayCasts.map { it.profileImage },
                        aheadCount = 10,
                        displaySize = ImageDisplaySize.THUMBNAIL
                    )
                    SectionTitle(stringResource(Res.string.home_section_birthday_cast))
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        state = birthdayCastListState,
                        flingBehavior = rememberStartSnapFlingBehavior(birthdayCastListState),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(uiState.birthdayCasts) { maid ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(74.dp)
                                    .clickable { onAction(HomeAction.ClickBirthdayMaid(maid.id)) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(74.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    ConCafeColors.primaryContainer,
                                                    ConCafeColors.primaryContainer
                                                )
                                            )
                                        )
                                ) {
                                    Image(
                                        painter = painterResource(Res.drawable.maid_logo),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
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
                                Text(
                                    maid.name,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                val cafeName = uiState.birthdayCastCafeNames[maid.cafeId]
                                if (!cafeName.isNullOrBlank()) {
                                    Text(
                                        cafeName,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        HomeSkeletonScreen(
            contentLayout = uiState.contentLayout,
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundColor)
        )
    }
}

@Composable
private fun HomeSkeletonScreen(contentLayout: AppContentLayout, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val bannerHeight = homeBannerHeight(maxWidth, contentLayout)

        HomeSkeletonContent(bannerHeight = bannerHeight, contentLayout = contentLayout)
    }
}

@Composable
private fun HomeSkeletonContent(bannerHeight: Dp, contentLayout: AppContentLayout) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentLayout.topPadding(FEED_TOP_PADDING), bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentLayout.horizontalPadding)
                .height(bannerHeight),
            shape = contentLayout.skeletonShape
        )
        repeat(2) {
            Column {
                ShimmerBox(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(140.dp)
                        .height(18.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) {
                        Column(modifier = Modifier.width(132.dp)) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(14.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(12.dp)
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .width(140.dp)
                    .height(18.dp)
            )
            repeat(3) {
                ShimmerListItemSkeleton(
                    avatarSize = 92.dp,
                    isAvatarCircular = false
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeCafeEventSection(
    events: List<HomeCafeEvent>,
    canLoadMore: Boolean,
    onAction: (HomeAction) -> Unit
) {
    val cafeEventListState = rememberLazyListState()
    LazyListImagePrefetch(
        state = cafeEventListState,
        imageUrls = events.map { it.imageUrl },
        aheadCount = 8,
        displaySize = ImageDisplaySize.MEDIUM
    )

    SectionTitle(
        text = stringResource(Res.string.home_section_ongoing_cafe_event),
        actionLabel = if (canLoadMore) stringResource(Res.string.home_show_more) else null,
        onAction = { onAction(HomeAction.LoadMoreCafeEvents) }
    )
    Spacer(Modifier.height(10.dp))
    if (events.isNotEmpty()) {
        LazyRow(
            state = cafeEventListState,
            flingBehavior = rememberStartSnapFlingBehavior(cafeEventListState),
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(events) { event ->
                HomeCafeEventCard(
                    event = event,
                    modifier = Modifier.width(276.dp),
                    onClick = { onAction(HomeAction.ClickCafeEvent(event.cafeId, event.id)) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberStartSnapFlingBehavior(listState: LazyListState) =
    rememberSnapFlingBehavior(
        SnapLayoutInfoProvider(
            lazyListState = listState,
            snapPosition = SnapPosition.Start
        )
    )

@Composable
private fun SnapLazyHorizontalGridToStartEffect(gridState: LazyGridState, rowCount: Int = 3) {
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .filter { !it }
            .collect {
                val firstVisibleItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@collect
                if (gridState.firstVisibleItemScrollOffset == 0) return@collect

                val currentColumn = firstVisibleItem.index / rowCount
                val targetColumn = if (gridState.firstVisibleItemScrollOffset > firstVisibleItem.size.width / 2) {
                    currentColumn + 1
                } else {
                    currentColumn
                }
                val targetIndex = (targetColumn * rowCount).coerceAtMost(gridState.layoutInfo.totalItemsCount - 1)

                gridState.animateScrollToItem(targetIndex)
            }
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
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.TopEnd
        ) {
            if (event.imageUrl.isNotBlank()) {
                BoxWithConstraints(modifier = Modifier.matchParentSize()) {
                    CompatImageDisplay(
                        imageUrl = event.imageUrl,
                        modifier = Modifier.size(maxWidth, maxHeight),
                        applyRoundedClip = false,
                        displaySize = ImageDisplaySize.MEDIUM
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Brush.linearGradient(listOf(ConCafeColors.surfaceTint, ConCafeColors.primaryContainer)))
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = event.cafeName,
                color = ConCafeColors.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
        val contentLayout = uiState.contentLayout
        val bannerHeight = homeBannerHeight(maxWidth, contentLayout)
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
                Box {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bannerHeight),
                        contentPadding = PaddingValues(horizontal = contentLayout.horizontalPadding),
                        pageSpacing = contentLayout.pageSpacing
                    ) { page ->
                        val banner = uiState.banners[page]

                        HomeBannerItem(
                            banner = banner,
                            contentLayout = contentLayout,
                            modifier = Modifier.fillMaxSize(),
                            onClick = { onAction(HomeAction.ClickBanner(banner)) }
                        )
                    }
                    if (contentLayout == AppContentLayout.FULL_BLEED && uiState.banners.size > 1) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${uiState.banners.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(18.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                // 플레이스홀더에도 동일한 높이 적용
                HomeBannerPlaceholderCard(bannerHeight, contentLayout)
            }
            if (contentLayout == AppContentLayout.LEGACY && uiState.banners.size > 1) {
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
                                    if (pagerState.currentPage == page) ConCafeColors.primary
                                    else ConCafeColors.outline
                                )
                        )
                    }
                }
            }
        } else {
            // 플레이스홀더에도 동일한 높이 적용
            HomeBannerPlaceholderCard(bannerHeight)
        }
    }
}

@Composable
private fun HomeBannerItem(
    banner: HomeBanner,
    contentLayout: AppContentLayout,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val imageUrl = banner.imageUrl?.trim().takeUnless { it.isNullOrEmpty() }
    val subtitle = banner.subtitle.trim().takeUnless { it.isEmpty() }

    Card(
        modifier = modifier.clickable { onClick() },
        shape = contentLayout.bannerShape
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
                    modifier = Modifier.matchParentSize(),
                    displaySize = ImageDisplaySize.MEDIUM,
                    applyRoundedClip = contentLayout == AppContentLayout.LEGACY
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
private fun HomeBannerPlaceholderCard(height: Dp, contentLayout: AppContentLayout) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = contentLayout.horizontalPadding),
        shape = contentLayout.bannerShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(ConCafeColors.outline, ConCafeColors.surfaceTint)
                    )
                )
                .padding(18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(Res.string.home_banner_placeholder_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(Res.string.home_banner_placeholder_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun homeBannerHeight(contentWidth: Dp, contentLayout: AppContentLayout): Dp {
    val bannerWidth = (contentWidth - contentLayout.horizontalPadding * 2).coerceAtLeast(0.dp)
    return (bannerWidth * (10f / 16f)).coerceAtMost(360.dp)
}

private val FEED_TOP_PADDING = 20.dp
private val BANNER_CORNER_RADIUS = 20.dp
private val BANNER_SKELETON_CORNER_RADIUS = 16.dp

private val AppContentLayout.pageSpacing: Dp
    get() = when (this) {
        AppContentLayout.FULL_BLEED -> 0.dp
        AppContentLayout.LEGACY -> 12.dp
    }

private val AppContentLayout.bannerShape: Shape
    get() = shape(BANNER_CORNER_RADIUS)

private val AppContentLayout.skeletonShape: Shape
    get() = shape(BANNER_SKELETON_CORNER_RADIUS)

private fun nearbyCafeItemWidth(contentWidth: Dp): Dp {
    val horizontalPadding = 16.dp
    val itemSpacing = 12.dp
    val nextItemPeekWidth = 16.dp
    return if (contentWidth >= 768.dp) {
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
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = ConCafeColors.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .wrapContentWidth(Alignment.End)
                    .clickable(onClick = onAction)
            )
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
                .background(Brush.verticalGradient(listOf(ConCafeColors.warningContainer, ConCafeColors.warningContainer)))
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
            Text(
                text = cafe.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val conceptLabel = nearbyCafeConceptLabel(cafe.conceptType)
            if (conceptLabel.isNotEmpty()) {
                Text(
                    text = conceptLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = ConCafeColors.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = localizedRegionCity(cafe.region.city),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeCommunitySection(
    posts: List<CommunityPost>,
    onAction: (HomeAction) -> Unit
) {
    val communityPostListState = rememberLazyListState()

    SectionTitle(
        text = stringResource(Res.string.home_community_latest_title),
        actionLabel = stringResource(Res.string.home_community_see_all),
        onAction = { onAction(HomeAction.ClickCommunity) }
    )
    Spacer(Modifier.height(10.dp))
    LazyRow(
        state = communityPostListState,
        flingBehavior = rememberStartSnapFlingBehavior(communityPostListState),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(posts) { post ->
            HomeCommunityPostCard(
                post = post,
                modifier = Modifier
                    .width(276.dp)
                    .fillParentMaxHeight(),
                onClick = { onAction(HomeAction.ClickCommunityPost(post.id)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCommunityPostCard(
    post: CommunityPost,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(ConCafeColors.surfaceTint, ConCafeColors.primaryContainer)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.userNickname.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ConCafeColors.primary
                    )
                }
                Text(
                    text = post.userNickname.ifBlank { "익명" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = post.displayDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = post.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = post.content,
                modifier = Modifier.height(64.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Divider(color = ConCafeColors.primaryContainer.copy(alpha = 0.3f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = stringResource(Res.string.community_post_like_count, post.likeCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = stringResource(Res.string.community_post_comment_count, post.commentCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
        "DOLL" -> stringResource(Res.string.home_nearby_cafe_type_doll)
        "COSPLAY" -> stringResource(Res.string.home_nearby_cafe_type_cosplay)
        "NAMJANG" -> stringResource(Res.string.home_nearby_cafe_type_namjang)
        "YOKAI" -> stringResource(Res.string.home_nearby_cafe_type_yokai)
        "CAT" -> stringResource(Res.string.home_nearby_cafe_type_cat)
        "OTHER" -> stringResource(Res.string.home_nearby_cafe_type_other)
        else -> normalized
    }
}
