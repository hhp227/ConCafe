package org.hhp227.concafe.presentation.cafe

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.hhp227.concafe.di.resolveGetCafeDetailUseCase
import org.hhp227.concafe.di.resolveToggleFavoriteCafeUseCase
import org.hhp227.concafe.domain.model.CafeDetail
import org.hhp227.concafe.domain.model.CafeDetailCast
import org.hhp227.concafe.domain.model.CafeDetailReview
import org.hhp227.concafe.domain.model.Menu
import org.hhp227.concafe.domain.model.Notice
import org.hhp227.concafe.presentation.component.ScrollableConCafeTabBar
import org.hhp227.concafe.presentation.component.colorFromHex
import org.hhp227.concafe.presentation.navigation.NavigationAction

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CafeContentScreen(
    uiState: CafeUiState,
    onAction: (CafeAction) -> Unit
) {
    val listState = rememberLazyListState()
    val detail = uiState.detail
    val isTopBarVisible = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 140

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFromHex("FFF9FC"))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (detail != null) {
                item {
                    CafeHeroSection(
                        detail = detail
                    )
                }
                item {
                    CafeSummarySection(detail = detail)
                }
                stickyHeader {
                    Surface(
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
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
                item {
                    CafeTabContent(
                        uiState = uiState,
                        onAction = onAction
                    )
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

        if (detail != null) {
            if (isTopBarVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onAction(CafeAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                    Text(
                        text = detail.cafe.name,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { onAction(CafeAction.ClickFavorite) }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "즐겨찾기",
                            tint = if (uiState.isFavorite) colorFromHex("EF6797") else Color(0xFF444444)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FloatingCircleButton(
                        onClick = { onAction(CafeAction.ClickBack) },
                        icon = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기"
                    )
                    FloatingCircleButton(
                        onClick = { onAction(CafeAction.ClickFavorite) },
                        icon = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "즐겨찾기",
                        tint = if (uiState.isFavorite) colorFromHex("EF6797") else Color(0xFF444444)
                    )
                }
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

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (uiState.selectedTab) {
            CafeUiState.TabType.INFO -> {
                InfoCard(detail = detail)
                DescriptionCard(detail = detail)
            }
            CafeUiState.TabType.MAIDS -> {
                MaidGrid(
                    casts = uiState.casts,
                    onAction = onAction
                )
            }
            CafeUiState.TabType.MENU -> {
                MenuList(detail.menus)
            }
            CafeUiState.TabType.REVIEWS -> {
                ReviewList(
                    rating = detail.cafe.ratingAvg,
                    reviewCount = detail.cafe.reviewCount,
                    reviews = uiState.reviews
                )
            }
            CafeUiState.TabType.NOTICES -> {
                NoticeList(detail.notices)
            }
        }
    }
}

@Composable
private fun InfoCard(detail: CafeDetail) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                value = detail.businessHours
            )
            InfoRow(
                icon = Icons.Default.Phone,
                title = "전화번호",
                value = detail.phoneNumber
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "소개",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail.cafe.description,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun MaidGrid(
    casts: List<CafeDetailCast>,
    onAction: (CafeAction) -> Unit
) {
    if (casts.isEmpty()) {
        EmptyContent(text = "등록된 메이드가 없습니다.")
    } else {
        val rows = casts.chunked(2)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { castItem ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAction(CafeAction.ClickMaid(castItem.cast.id)) },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(colorFromHex("FFDFEA"), colorFromHex("FFBED5"))
                                            )
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (castItem.isWorking) {
                                            Text(
                                                text = "출근중",
                                                color = Color.White,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(Color(0xFF35B56A))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Text(
                                            text = castItem.cast.conceptRole.uppercase(),
                                            color = Color.White.copy(alpha = 0.88f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = castItem.cast.name,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = castItem.cast.description,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color(0xFF777777),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuList(menus: List<Menu>) {
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
                        )
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
                                text = menu.description,
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
private fun ReviewList(
    rating: Double,
    reviewCount: Int,
    reviews: List<CafeDetailReview>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = formatRating(rating),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${reviewCount}개 리뷰",
                        color = Color(0xFF777777)
                    )
                }
            }
        }
        if (reviews.isEmpty()) {
            EmptyContent(text = "아직 등록된 리뷰가 없습니다.")
        } else {
            reviews.forEach { review ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = review.userNickname,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (review.verified) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(colorFromHex("EF6797"))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "방문인증",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            Text(
                                text = review.createdDate,
                                color = Color(0xFF999999),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        StarRating(review.rating)
                        Text(text = review.content)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeList(notices: List<Notice>) {
    if (notices.isEmpty()) {
        EmptyContent(text = "등록된 공지가 없습니다.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            notices.forEach { notice ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = notice.title,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = notice.createdAt.take(10),
                                color = Color(0xFF999999),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = notice.content,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StarRating(rating: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (index < rating.toInt()) Color(0xFFFFC107) else Color(0xFFE1E1E1),
                modifier = Modifier.size(16.dp)
            )
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

@Composable
private fun FloatingCircleButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: Color = Color(0xFF333333)
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

private fun formatRating(rating: Double): String {
    val scaled = (rating * 10).toInt()
    val whole = scaled / 10
    val decimal = scaled % 10
    return "$whole.$decimal"
}
