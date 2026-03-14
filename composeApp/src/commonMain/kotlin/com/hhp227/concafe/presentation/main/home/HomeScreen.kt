package com.hhp227.concafe.presentation.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.presentation.component.ConCafeCastCard
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GlobalContext.get().get<HomeViewModel>()
            }
        }
    ),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { uiState.banners.size })
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is HomeEvent.OpenExternalLink -> uriHandler.openUri(event.url)
                is HomeEvent.NavigateToCafe -> onNavigate(NavigationAction.NavigateToCafe(event.id))
                is HomeEvent.NavigateToCast -> onNavigate(NavigationAction.NavigateToCast(event.id))
            }
        }
    }
    LaunchedEffect(uiState.banners.size) {
        if (!uiState.banners.isEmpty()) {
            val normalizedPage = pagerState.currentPage.coerceIn(0, uiState.banners.lastIndex)

            pagerState.scrollToPage(normalizedPage)
            if (uiState.banners.size != 1) {
                while (true) {
                    delay(3000)
                    val nextPage = (pagerState.settledPage + 1) % uiState.banners.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }
    HomeContentScreen(uiState, pagerState, viewModel::onAction)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeContentScreen(
    uiState: HomeUiState,
    pagerState: PagerState,
    onAction: (HomeAction) -> Unit
) {
    val screenBackgroundColor = Color(0xFFFFFBFD)
    val cafeNameById = uiState.nearbyCafes.associate { it.id to it.name }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundColor),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (uiState.banners.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 12.dp
                    ) { page ->
                        val banner = uiState.banners[page]

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAction(HomeAction.ClickBanner(banner)) },
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
                                    )
                                    .padding(18.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Text(
                                    text = banner.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
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
                                    .size(width = if (pagerState.currentPage == page) 18.dp else 8.dp, height = 8.dp)
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
        item {
            SectionTitle("인기 메이드", "❤")
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(uiState.popularCasts) { maid ->
                    ConCafeCastCard(
                        name = maid.name,
                        subtitle = cafeNameById[maid.cafeId] ?: maid.cafeId,
                        modifier = Modifier.width(132.dp),
                        metaText = "👥 ${maid.followerCount}",
                        onClick = { onAction(HomeAction.ClickMaid(maid.id)) }
                    )
                }
            }
        }
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val contentWidth = maxWidth
                val itemWidth = nearbyCafeItemWidth(contentWidth)

                Column {
                    SectionTitle(
                        text = "근처 메이드카페",
                        leading = "📍",
                        actionLabel = if (uiState.canLoadMoreNearbyCafes) "더보기" else null,
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
                        items(uiState.nearbyCafes) { cafe ->
                            NearByCafeItem(
                                cafe = cafe,
                                modifier = Modifier
                                    .width(itemWidth)
                                    .height(92.dp)
                                    .clickable { onAction(HomeAction.ClickCafe(cafe.id)) }
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("생일인 메이드", "🎂")
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
                        )
                        Text(maid.name, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            SectionTitle("최근 카페 공지", "📢")
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.notices.forEach { notice ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notice.cafeName, color = Color(0xFFEF6797), fontWeight = FontWeight.SemiBold)
                                Text(notice.content, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(notice.relativeTime, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A8A8A))
                        }
                    }
                }
            }
        }
    }
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
    leading: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val actionSlotWidth = 44.dp
    val actionSlotHeight = 24.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(actionSlotHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(leading)
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
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
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(cafe.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("⭐ ${cafe.ratingAvg}", style = MaterialTheme.typography.bodySmall)
            Text(cafe.region.city, color = Color(0xFF7E7E7E), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("📍 ${cafe.region.address}", color = Color(0xFFEF6797), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
