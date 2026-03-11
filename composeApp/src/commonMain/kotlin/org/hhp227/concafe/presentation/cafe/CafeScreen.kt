package org.hhp227.concafe.presentation.cafe

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
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
import org.hhp227.concafe.di.resolveGetCafeCastListPageUseCase
import org.hhp227.concafe.di.resolveGetCafeDetailUseCase
import org.hhp227.concafe.di.resolveToggleFavoriteCafeUseCase
import org.hhp227.concafe.domain.model.CafeDetail
import org.hhp227.concafe.presentation.cafe.tab.*
import org.hhp227.concafe.presentation.component.ScrollableConCafeTabBar
import org.hhp227.concafe.presentation.component.colorFromHex
import org.hhp227.concafe.presentation.navigation.NavigationAction
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CafeScreen(
    cafeId: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: CafeViewModel = viewModel(
        key = "cafe-$cafeId",
        factory = viewModelFactory {
            initializer {
                CafeViewModel(
                    cafeId = cafeId,
                    getCafeDetailUseCase = resolveGetCafeDetailUseCase(),
                    getCafeCastListPageUseCase = resolveGetCafeCastListPageUseCase(),
                    toggleFavoriteCafeUseCase = resolveToggleFavoriteCafeUseCase()
                )
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CafeEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is CafeEvent.NavigateToCast -> onNavigationAction(NavigationAction.NavigateToCast(event.id))
                CafeEvent.NavigateToSignIn -> onNavigationAction(NavigationAction.NavigateToSignIn)
            }
        }
    }
    CafeContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CafeContentScreen(
    uiState: CafeUiState,
    onAction: (CafeAction) -> Unit
) {
    val listState = rememberLazyListState()
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
        uiState.canLoadMoreCasts,
        uiState.isLoadingMoreCasts
    ) {
        if (uiState.selectedTab != CafeUiState.TabType.MAIDS) return@LaunchedEffect
        snapshotFlow { listState.canScrollForward }
            .distinctUntilChanged()
            .collect { canScrollForward ->
                if (!canScrollForward && uiState.canLoadMoreCasts && !uiState.isLoadingMoreCasts) {
                    onAction(CafeAction.LoadMoreCasts)
                }
            }
    }
    Scaffold(
        containerColor = colorFromHex("FFF9FC"),
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
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = if (isTopBarVisible) Color(0xFF222222) else Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(CafeAction.ClickFavorite) }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "즐겨찾기",
                            tint = if (uiState.isFavorite) {
                                colorFromHex("EF6797")
                            } else {
                                if (isTopBarVisible) Color(0xFF222222) else Color.White
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isTopBarVisible) Color.White else Color.Transparent,
                    scrolledContainerColor = Color.White,
                    navigationIconContentColor = if (isTopBarVisible) Color(0xFF222222) else Color.White,
                    titleContentColor = Color(0xFF222222),
                    actionIconContentColor = if (isTopBarVisible) Color(0xFF222222) else Color.White
                )
            )
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
                    .background(colorFromHex("FFF9FC")),
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
                            labels = CafeUiState.TabType.entries.map { it.label },
                            selectedIndex = CafeUiState.TabType.entries.indexOf(uiState.selectedTab),
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White,
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
                                text = uiState.errorMessage ?: "카페 상세 데이터를 불러오지 못했습니다.",
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "다시 시도해 주세요.",
                                color = Color(0xFF777777)
                            )
                            Text(
                                text = "새로고침",
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
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topBarInset)
                    .zIndex(1f)
                    .align(Alignment.TopCenter)
                    .alpha(if (isTabPinned) 1f else 0f)
            ) {
                ScrollableConCafeTabBar(
                    labels = CafeUiState.TabType.entries.map { it.label },
                    selectedIndex = CafeUiState.TabType.entries.indexOf(uiState.selectedTab),
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color.White,
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
    val pagerState = rememberPagerState(pageCount = { detail.images.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val imageUrl = detail.images[page]

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
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(detail.images.size) { index ->
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = detail.cafe.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatRating(detail.cafe.ratingAvg),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${detail.cafe.reviewCount})",
                    color = Color(0xFF777777)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF777777),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = detail.cafe.region.city,
                    color = Color(0xFF777777)
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
        CafeUiState.TabType.MAIDS -> CafeCastScreen(
            casts = uiState.casts,
            canLoadMore = uiState.canLoadMoreCasts,
            isLoadingMore = uiState.isLoadingMoreCasts,
            onAction = onAction
        )
        CafeUiState.TabType.MENU -> CafeMenuScreen(detail.menus)
        CafeUiState.TabType.REVIEWS -> CafeReviewScreen(detail, uiState.reviews)
        CafeUiState.TabType.NOTICES -> CafeNoticeScreen(detail.notices)
    }
}

private fun formatRating(rating: Double): String {
    val scaled = (rating * 10).toInt()
    val whole = scaled / 10
    val decimal = scaled % 10
    return "$whole.$decimal"
}
