package com.hhp227.concafe.presentation.cast

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.core.util.TimeUtils
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastRecentReview
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import kotlin.collections.List
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.getOrNull
import kotlin.collections.isNotEmpty
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.toSet
import kotlin.sequences.firstOrNull
import kotlin.sequences.ifEmpty
import kotlin.sequences.mapNotNull
import kotlin.sequences.toSet
import kotlin.text.contains
import kotlin.text.firstOrNull
import kotlin.text.format
import kotlin.text.isNotEmpty
import kotlin.text.mapNotNull
import kotlin.text.orEmpty
import kotlin.text.replaceFirstChar
import kotlin.text.split
import kotlin.text.toIntOrNull
import kotlin.text.toSet
import kotlin.text.uppercase

private const val CURRENT_DATE = "2026-03-08"
private val SummaryTitleTriggerOffset = 22.dp

@Composable
fun CastScreen(
    castId: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: CastViewModel = viewModel(
        key = "cast-$castId",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<CastViewModel> { parametersOf(castId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CastEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is CastEvent.NavigateToCafe -> onNavigationAction(NavigationAction.NavigateToCafe(event.id))
                CastEvent.NavigateToSignIn -> onNavigationAction(NavigationAction.NavigateToSignIn)
            }
        }
    }
    CastContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastContentScreen(
    uiState: CastUiState,
    onAction: (CastAction) -> Unit
) {
    val listState = rememberLazyListState()
    val scrollOffset = if (listState.firstVisibleItemIndex == 0) {
        listState.firstVisibleItemScrollOffset
    } else {
        Int.MAX_VALUE
    }
    val topBarVisible = uiState.detail != null && (
        listState.firstVisibleItemIndex > 1 ||
            (listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 1 }?.offset?.let { summaryOffset ->
                summaryOffset <= with(LocalDensity.current) { SummaryTitleTriggerOffset.roundToPx() }
            } == true)
        )

    Scaffold(
        containerColor = colorFromHex("FFF9FC"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (topBarVisible) uiState.detail?.cast?.name.orEmpty() else "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CastAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = if (topBarVisible) Color(0xFF222222) else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (topBarVisible) Color.White else Color.Transparent,
                    scrolledContainerColor = Color.White,
                    titleContentColor = Color(0xFF222222),
                    navigationIconContentColor = if (topBarVisible) Color(0xFF222222) else Color.White,
                    actionIconContentColor = if (topBarVisible) Color(0xFF222222) else Color.White
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.detail != null -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorFromHex("FFF9FC")),
                    contentPadding = PaddingValues(
                        bottom = innerPadding.calculateBottomPadding() + 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        CastHeroSection(
                            detail = uiState.detail,
                            scrollOffset = scrollOffset
                        )
                    }
                    item {
                        CastSummarySection(
                            detail = uiState.detail,
                            isFollowing = uiState.isFollowing,
                            onAction = onAction
                        )
                    }
                    item {
                        CastTodaySection(detail = uiState.detail)
                    }
                    item {
                        CastScheduleSection(detail = uiState.detail)
                    }
                    item {
                        CastIntroductionSection(detail = uiState.detail)
                    }
                    item {
                        CastRecentActivitySection(detail = uiState.detail)
                    }
                    item {
                        CastRecentReviewSection(reviews = uiState.recentReviews)
                    }
                }
            }
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "캐스트 상세 데이터를 불러오지 못했습니다.",
                            color = MaterialTheme.colorScheme.error
                        )
                        FilledTonalButton(onClick = { onAction(CastAction.Refresh) }) {
                            Text("새로고침")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CastHeroSection(
    detail: CastDetail,
    scrollOffset: Int
) {
    val heroHeight = 330.dp
    val heroImages = resolveHeroImages(
        images = detail.images,
        fallbackProfileImage = detail.cast.profileImage
    )
    val pagerState = rememberPagerState(pageCount = { heroImages.size })
    val parallaxOffset = if (scrollOffset == Int.MAX_VALUE) {
        120f
    } else {
        scrollOffset * 0.35f
    }

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
                    .graphicsLayer { translationY = parallaxOffset }
                    .background(
                        if (imageUrl.isBlank()) {
                            heroBrush(page)
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(colorFromHex("FFC6DB"), colorFromHex("F7A6C5"))
                            )
                        }
                    )
            ) {
                if (imageUrl.isNotBlank()) {
                    CompatImageDisplay(
                        imageUrl = imageUrl,
                        modifier = Modifier.fillMaxSize(),
                        applyRoundedClip = false
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x66000000)
                                )
                            )
                        )
                )
                if (imageUrl.isBlank()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = detail.cast.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                Text(
                    text = detail.cafe.name,
                    color = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                )
            }
        }
        if (heroImages.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(heroImages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(width = if (pagerState.currentPage == index) 18.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                                if (pagerState.currentPage == index) Color.White
                                else Color.White.copy(alpha = 0.45f)
                            )
                    )
                }
            }
        }
    }
}

private fun resolveHeroImages(
    images: List<String>,
    fallbackProfileImage: String?
): List<String> {
    val normalized = images
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (normalized.isNotEmpty()) {
        return normalized
    }
    val fallback = fallbackProfileImage?.trim().orEmpty()
    return if (fallback.isNotEmpty()) listOf(fallback) else listOf("")
}

@Composable
private fun CastSummarySection(
    detail: CastDetail,
    isFollowing: Boolean,
    onAction: (CastAction) -> Unit
) {
    Surface(color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = detail.cast.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = colorFromHex("FFE7F1")
                    ) {
                        Text(
                            text = detail.cast.conceptRole.replaceFirstChar { it.uppercase() },
                            color = colorFromHex("C9527E"),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAction(CastAction.ClickCafe) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF7A7A7A),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${detail.cafe.name} · ${detail.cafe.region.city}",
                            color = Color(0xFF6F6A70),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Button(
                    onClick = { onAction(CastAction.ClickFollow) },
                    colors = if (isFollowing) {
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1E3EB),
                            contentColor = Color(0xFF6A4960)
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = colorFromHex("EF6797"),
                            contentColor = Color.White
                        )
                    }
                ) {
                    Text(if (isFollowing) "팔로잉" else "팔로우")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CastStatItem(
                    icon = Icons.Default.Groups,
                    label = "팔로워",
                    value = "${detail.cast.followerCount}명"
                )
            }
        }
    }
}

@Composable
private fun CastTodaySection(detail: CastDetail) {
    val todaySchedule = detail.schedule.firstOrNull { it.date == CURRENT_DATE }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colorFromHex("EF6797"),
                            colorFromHex("F8A3C5")
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "오늘의 출근 상태",
                    color = Color.White.copy(alpha = 0.82f)
                )
                Text(
                    text = if (todaySchedule != null) "출근 예정" else "오늘은 휴무",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = todaySchedule?.let { "${it.startTime} - ${it.endTime}" } ?: "다음 스케줄을 확인해 주세요.",
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun CastScheduleSection(detail: CastDetail) {
    val weeklyStatus = rememberWeeklySchedule(detail.schedule)

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = colorFromHex("EF6797")
            )
            Text(
                text = "출근 일정",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weeklyStatus.forEach { item ->
                CastScheduleCard(
                    modifier = Modifier.weight(1f),
                    dayLabel = item.dayLabel,
                    isWorking = item.isWorking
                )
            }
        }
    }
}

@Composable
private fun CastScheduleCard(
    modifier: Modifier = Modifier,
    dayLabel: String,
    isWorking: Boolean
) {
    Surface(
        color = if (isWorking) colorFromHex("EF6797") else Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = dayLabel,
                fontWeight = FontWeight.SemiBold,
                color = if (isWorking) Color.White else Color(0xFF4E4750)
            )
            Text(
                text = if (isWorking) "출근" else "휴무",
                fontSize = 12.sp,
                color = if (isWorking) Color.White.copy(alpha = 0.92f) else Color(0xFF8A8087)
            )
        }
    }
}

@Composable
private fun CastIntroductionSection(detail: CastDetail) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "소개",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = detail.cast.desc,
                color = Color(0xFF4E4750),
                modifier = Modifier.padding(18.dp),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun CastRecentActivitySection(detail: CastDetail) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "최근 활동",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CastActivityCard(
                modifier = Modifier
                    .weight(1f),
                value = detail.visitCertificationCount.toString(),
                label = "방문 인증"
            )
            CastActivityCard(
                modifier = Modifier
                    .weight(1f),
                value = detail.cast.followerCount.toString(),
                label = "팔로워"
            )
            CastActivityCard(
                modifier = Modifier
                    .weight(1f),
                value = String.format("%.1f", detail.cast.rating),
                label = "평점"
            )
        }
    }
}

@Composable
private fun CastActivityCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorFromHex("EF6797")
            )
            Text(
                text = label,
                color = Color(0xFF6F6A70),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CastRecentReviewSection(reviews: List<CastRecentReview>) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "함께 언급된 후기",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (reviews.isEmpty()) {
            CastRecentReviewEmptyView()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                reviews.forEach { review ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Text(
                                        text = "${review.rating}",
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0x1AFFD1DC))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = colorFromHex("EF6797"),
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = review.createdDateLabel,
                                    color = Color(0xFF8A8087),
                                    fontSize = 12.sp
                                )
                            }
                            if (review.taggedCastNames.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    review.taggedCastNames.forEach { castName ->
                                        Text(
                                            text = castName,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(0x1AFFD1DC))
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            color = colorFromHex("C9527E"),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                text = review.content,
                                color = Color(0xFF4E4750)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CastRecentReviewEmptyView() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "아직 함께 언급된 후기가 없어요.",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4E4750)
            )
            Text(
                text = "이 캐스트가 태그된 카페 리뷰가 표시됩니다.",
                color = Color(0xFF8A8087),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CastStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorFromHex("EF6797"),
            modifier = Modifier.size(18.dp)
        )
        Row(
            modifier = Modifier.padding(start = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color(0xFF8A8087),
                fontSize = 12.sp
            )
            Text(
                text = value,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun heroBrush(index: Int): Brush {
    val gradients = listOf(
        listOf(colorFromHex("F8A3C5"), colorFromHex("EF6797")),
        listOf(colorFromHex("FFC6C7"), colorFromHex("FF8E9E")),
        listOf(colorFromHex("F8D6E9"), colorFromHex("D98AB7"))
    )
    return Brush.verticalGradient(gradients[index % gradients.size])
}

@Composable
private fun rememberWeeklySchedule(schedule: List<CastSchedule>): List<WeeklyScheduleItem> {
    val workingDays = schedule.mapNotNull { TimeUtils.weekdayLabelFromIsoDateOrNull(it.date) }.toSet()
    val orderedDays = listOf("월", "화", "수", "목", "금", "토", "일")
    return orderedDays.map { dayLabel ->
        WeeklyScheduleItem(
            dayLabel = dayLabel,
            isWorking = workingDays.contains(dayLabel)
        )
    }
}

private data class WeeklyScheduleItem(
    val dayLabel: String,
    val isWorking: Boolean
)
