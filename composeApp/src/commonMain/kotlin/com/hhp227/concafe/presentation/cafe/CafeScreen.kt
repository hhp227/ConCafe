package com.hhp227.concafe.presentation.cafe

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.core.util.RatingUtils
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.presentation.cafe.tab.*
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ScrollableConCafeTabBar
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.*
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@Composable
fun CafeScreen(
    cafeId: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: CafeViewModel = viewModel(
        key = "cafe-$cafeId",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<CafeViewModel> { parametersOf(cafeId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CafeEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is CafeEvent.NavigateToCast -> onNavigationAction(NavigationAction.NavigateToCast(event.id))
                is CafeEvent.NavigateToCafeEvent -> {
                    onNavigationAction(NavigationAction.NavigateToCafeEvent(event.cafeId, event.eventId))
                }
                is CafeEvent.NavigateToReviewEdit -> {
                    onNavigationAction(NavigationAction.NavigateToReviewEdit(event.cafeId, event.reviewId))
                }
                is CafeEvent.NavigateToPicture -> onNavigationAction(NavigationAction.NavigateToPicture(event.imageUrl))
                CafeEvent.NavigateToSignIn -> onNavigationAction(NavigationAction.NavigateToSignIn)
                CafeEvent.ShowReviewDeleteFailedMessage -> snackbarHostState.showSnackbar(
                    getString(Res.string.cafe_message_review_delete_failed)
                )
                CafeEvent.ShowReviewReportedMessage -> snackbarHostState.showSnackbar(
                    getString(Res.string.cafe_message_report_received)
                )
            }
        }
    }
    LaunchedEffect(uiState.shouldScrollToTopOnReturn) {
        if (uiState.shouldScrollToTopOnReturn) {
            listState.animateScrollToItem(0)
            viewModel.onAction(CafeAction.ConsumeScrollToTopOnReturn)
        }
    }
    CafeContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        listState = listState,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CafeContentScreen(
    uiState: CafeUiState,
    onAction: (CafeAction) -> Unit,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val noticesTabLabel = stringResource(Res.string.cafe_tab_notices)
    val eventTabLabel = stringResource(Res.string.noticeevent_tab_event)
    val tabLabels = listOf(
        stringResource(Res.string.cafe_tab_info),
        stringResource(Res.string.cafe_tab_casts),
        stringResource(Res.string.cafe_tab_menu),
        stringResource(Res.string.cafe_tab_reviews),
        "$noticesTabLabel/$eventTabLabel"
    )
    val isTopBarVisible = uiState.detail != null && (
            listState.firstVisibleItemIndex > 1 ||
                    (listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 1 }?.offset
                        ?.let { summaryOffset ->
                            summaryOffset <= with(LocalDensity.current) { 16.dp.roundToPx() }
                        } == true)
            )

    LaunchedEffect(
        listState,
        uiState.selectedTab,
        uiState.casts.size,
        uiState.canLoadMoreCasts,
        uiState.isLoadingMoreCasts,
        uiState.notices.size,
        uiState.canLoadMoreNotices,
        uiState.isLoadingMoreNotices,
        uiState.reviews.size,
        uiState.canLoadMoreReviews,
        uiState.isLoadingMoreReviews
    ) {
        snapshotFlow {
            Triple(
                listState.canScrollForward,
                uiState.selectedTab,
                when (uiState.selectedTab) {
                    CafeUiState.TabType.CASTS -> uiState.casts.size
                    CafeUiState.TabType.NOTICES -> uiState.notices.size
                    CafeUiState.TabType.REVIEWS -> uiState.reviews.size
                    else -> 0
                }
            )
        }.distinctUntilChanged()
            .collect { (canScrollForward, _, _) ->
                if (!canScrollForward) {
                    when (uiState.selectedTab) {
                        CafeUiState.TabType.CASTS -> {
                            if (uiState.canLoadMoreCasts && !uiState.isLoadingMoreCasts) {
                                onAction(CafeAction.LoadMoreCasts)
                            }
                        }
                        CafeUiState.TabType.NOTICES -> {
                            if (uiState.canLoadMoreNotices && !uiState.isLoadingMoreNotices) {
                                onAction(CafeAction.LoadMoreNotices)
                            }
                        }
                        CafeUiState.TabType.REVIEWS -> {
                            if (uiState.canLoadMoreReviews && !uiState.isLoadingMoreReviews) {
                                onAction(CafeAction.LoadMoreReviews)
                            }
                        }
                        else -> Unit
                    }
                }
            }
    }
    Scaffold(
        containerColor = colorFromHex("FFFBFD"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isTopBarVisible) uiState.detail?.cafe?.name.orEmpty() else "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CafeAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cafe_accessibility_back),
                            tint = if (isTopBarVisible) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(CafeAction.ClickFavorite) }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(Res.string.cafe_accessibility_favorite),
                            tint = if (uiState.isFavorite) {
                                colorFromHex("EF6797")
                            } else {
                                if (isTopBarVisible) MaterialTheme.colorScheme.onSurface else Color.White
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isTopBarVisible) MaterialTheme.colorScheme.surface else Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = if (isTopBarVisible) MaterialTheme.colorScheme.onSurface else Color.White,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = if (isTopBarVisible) MaterialTheme.colorScheme.onSurface else Color.White
                )
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == CafeUiState.TabType.REVIEWS && uiState.detail != null && uiState.isLoggedIn) {
                ExtendedFloatingActionButton(
                    onClick = { onAction(CafeAction.ClickWriteReview) },
                    containerColor = colorFromHex("FFD1DC"),
                    contentColor = colorFromHex("2B2330"),
                    text = {
                        Text(
                            text = stringResource(Res.string.cafe_action_write_review),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                )
            }
        }
    ) { innerPadding ->
        val topBarInset = innerPadding.calculateTopPadding()
        val topBarInsetPx = with(LocalDensity.current) { topBarInset.roundToPx() }
        val tabHeaderItemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 2 }
        val isTabPinned = uiState.detail != null && (
                tabHeaderItemInfo == null || tabHeaderItemInfo.offset <= topBarInsetPx
                )

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorFromHex("FFFBFD")),
                contentPadding = PaddingValues(
                    top = 0.dp,
                    bottom = innerPadding.calculateBottomPadding() + 32.dp
                )
            ) {
                if (uiState.detail != null) {
                    item {
                        CafeHeroSection(
                            detail = uiState.detail
                        )
                    }
                    item {
                        CafeSummarySection(detail = uiState.detail)
                    }
                    item {
                        ScrollableConCafeTabBar(
                            labels = tabLabels,
                            selectedIndex = CafeUiState.TabType.entries.indexOf(uiState.selectedTab),
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            onTabSelected = { index ->
                                onAction(CafeAction.ChangeTab(CafeUiState.TabType.entries[index]))
                            }
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CafeTabContent(
                                uiState = uiState,
                                onAction = onAction
                            )
                        }
                    }
                } else if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.cafe_error_detail_load_failed),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(Res.string.cafe_error_retry_prompt),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(Res.string.cafe_action_refresh),
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorFromHex("EF6797"))
                                    .clickable { onAction(CafeAction.Refresh) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topBarInset)
                    .zIndex(1f)
                    .align(Alignment.TopCenter)
                    .alpha(if (isTabPinned) 1f else 0f)
            ) {
                ScrollableConCafeTabBar(
                    labels = tabLabels,
                    selectedIndex = CafeUiState.TabType.entries.indexOf(uiState.selectedTab),
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    onTabSelected = { index ->
                        onAction(CafeAction.ChangeTab(CafeUiState.TabType.entries[index]))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CafeHeroSection(
    detail: CafeDetail
) {
    val heroHeight = 330.dp
    val heroImages = remember(detail) {
        val normalized = detail.images
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val fallbackThumbnail = detail.cafe.thumbnailImage?.trim().orEmpty()

        when {
            normalized.isNotEmpty() -> normalized
            fallbackThumbnail.isNotEmpty() -> listOf(fallbackThumbnail)
            else -> listOf("")
        }
    }
    val pagerState = rememberPagerState(pageCount = { heroImages.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val imageUrl = heroImages[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (imageUrl.isBlank()) {
                                listOf(colorFromHex("FFE2D2"), colorFromHex("FFC9A9"))
                            } else {
                                listOf(colorFromHex("FFC6DB"), colorFromHex("F7A6C5"))
                            }
                        )
                    )
            ) {
                if (imageUrl.isNotBlank()) {
                    CompatImageDisplay(
                        imageUrl = imageUrl,
                        modifier = Modifier.fillMaxSize(),
                        applyRoundedClip = false
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LocalCafe,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(heroImages.size) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (pagerState.currentPage == index) 18.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.45f))
                )
            }
        }
    }
}

@Composable
private fun CafeSummarySection(detail: CafeDetail) {
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = detail.cafe.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (detail.cafe.ownerIds.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = colorFromHex("2563EB"),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = colorFromHex("FFC107"),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = RatingUtils.formatOneDecimalTruncated(detail.cafe.ratingAvg),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${detail.cafe.reviewCount})",
                    color = if (isDarkMode) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isDarkMode) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = detail.cafe.region.city,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CafeTabContent(
    uiState: CafeUiState,
    onAction: (CafeAction) -> Unit
) {
    val detail = uiState.detail ?: return

    when (uiState.selectedTab) {
        CafeUiState.TabType.INFO -> CafeInfoScreen(detail)
        CafeUiState.TabType.CASTS -> CafeCastScreen(
            casts = uiState.casts,
            canLoadMore = uiState.canLoadMoreCasts,
            isLoadingMore = uiState.isLoadingMoreCasts,
            onAction = onAction
        )
        CafeUiState.TabType.MENU -> CafeMenuScreen(
            menus = detail.menus,
            goods = detail.goods,
            isLoading = uiState.isLoadingMenuGoods
        )
        CafeUiState.TabType.REVIEWS -> CafeReviewScreen(
            detail = detail,
            reviews = uiState.reviews,
            canLoadMore = uiState.canLoadMoreReviews,
            isLoadingMore = uiState.isLoadingMoreReviews,
            currentUserId = uiState.currentUserId,
            onAction = onAction
        )
        CafeUiState.TabType.NOTICES -> CafeNoticeScreen(
            events = uiState.events,
            notices = uiState.notices,
            canLoadMore = uiState.canLoadMoreNotices,
            isLoadingMore = uiState.isLoadingMoreNotices,
            onEventClick = { onAction(CafeAction.ClickEvent(it)) }
        )
    }
}



