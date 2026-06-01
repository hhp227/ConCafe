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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
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
import com.hhp227.concafe.core.util.CastScheduleAttendanceUtils
import com.hhp227.concafe.core.util.RatingUtils
import com.hhp227.concafe.core.util.TimeUtils
import com.hhp227.concafe.domain.model.CastAttendanceStatus
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastRecentReview
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.DetailTooltipBox
import com.hhp227.concafe.presentation.component.ImageDisplaySize
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.cast_accessibility_back
import concafe.composeapp.generated.resources.cast_action_refresh
import concafe.composeapp.generated.resources.cast_activity_follower
import concafe.composeapp.generated.resources.cast_activity_rating
import concafe.composeapp.generated.resources.cast_activity_visit_cert
import concafe.composeapp.generated.resources.cast_error_detail_load_failed
import concafe.composeapp.generated.resources.cast_follow
import concafe.composeapp.generated.resources.cast_follower_count
import concafe.composeapp.generated.resources.cast_follower_label
import concafe.composeapp.generated.resources.cast_following
import concafe.composeapp.generated.resources.cast_follow_tooltip
import concafe.composeapp.generated.resources.cast_schedule_off
import concafe.composeapp.generated.resources.cast_schedule_title
import concafe.composeapp.generated.resources.cast_schedule_work
import concafe.composeapp.generated.resources.cast_section_intro
import concafe.composeapp.generated.resources.cast_section_recent_activity
import concafe.composeapp.generated.resources.cast_section_tagged_reviews
import concafe.composeapp.generated.resources.cast_tagged_reviews_empty_desc
import concafe.composeapp.generated.resources.cast_tagged_reviews_empty_title
import concafe.composeapp.generated.resources.cast_today_check_schedule
import concafe.composeapp.generated.resources.cast_today_finished
import concafe.composeapp.generated.resources.cast_today_off
import concafe.composeapp.generated.resources.cast_today_status_title
import concafe.composeapp.generated.resources.cast_today_upcoming
import concafe.composeapp.generated.resources.cast_today_working
import concafe.composeapp.generated.resources.cast_weekday_fri
import concafe.composeapp.generated.resources.cast_weekday_mon
import concafe.composeapp.generated.resources.cast_weekday_sat
import concafe.composeapp.generated.resources.cast_weekday_sun
import concafe.composeapp.generated.resources.cast_weekday_thu
import concafe.composeapp.generated.resources.cast_weekday_tue
import concafe.composeapp.generated.resources.cast_weekday_wed
import kotlinx.coroutines.delay
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import org.jetbrains.compose.resources.stringResource

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
                is CastEvent.NavigateToPicture -> onNavigationAction(NavigationAction.NavigateToPicture(event.imageUrl))
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
        containerColor = MaterialTheme.colorScheme.background,
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cast_accessibility_back),
                            tint = if (topBarVisible) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (topBarVisible) MaterialTheme.colorScheme.surface else Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = if (topBarVisible) MaterialTheme.colorScheme.onSurface else Color.White,
                    actionIconContentColor = if (topBarVisible) MaterialTheme.colorScheme.onSurface else Color.White
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
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(
                        bottom = innerPadding.calculateBottomPadding() + 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        CastHeroSection(
                            detail = uiState.detail,
                            scrollOffset = scrollOffset,
                            onAction = onAction
                        )
                    }
                    item {
                        CastSummarySection(
                            detail = uiState.detail,
                            isFollowing = uiState.isFollowing,
                            isSelfCast = uiState.isSelfCast,
                            shouldShowFollowTooltip = uiState.shouldShowFollowTooltip,
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
                            text = stringResource(Res.string.cast_error_detail_load_failed),
                            color = MaterialTheme.colorScheme.error
                        )
                        FilledTonalButton(onClick = { onAction(CastAction.Refresh) }) {
                            Text(stringResource(Res.string.cast_action_refresh))
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
    scrollOffset: Int,
    onAction: (CastAction) -> Unit
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
                    .clickable(enabled = imageUrl.isNotBlank()) { onAction(CastAction.ClickImage(imageUrl)) }
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
                        applyRoundedClip = false,
                        displaySize = ImageDisplaySize.MEDIUM
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastSummarySection(
    detail: CastDetail,
    isFollowing: Boolean,
    isSelfCast: Boolean,
    shouldShowFollowTooltip: Boolean,
    onAction: (CastAction) -> Unit
) {
    LaunchedEffect(shouldShowFollowTooltip) {
        if (shouldShowFollowTooltip) {
            onAction(CastAction.MarkFollowTooltipShown)
            delay(DETAIL_TOOLTIP_DURATION_MILLIS)
            onAction(CastAction.DismissFollowTooltip)
        }
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = detail.cast.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!detail.cast.linkedUserId.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = colorFromHex("9333EA"),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${detail.cafe.name} · ${detail.cafe.region.city}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                DetailTooltipBox(
                    visible = shouldShowFollowTooltip,
                    text = stringResource(Res.string.cast_follow_tooltip)
                ) {
                    Button(
                        onClick = { onAction(CastAction.ClickFollow) },
                        enabled = !isSelfCast,
                        colors = if (isFollowing) {
                            ButtonDefaults.buttonColors(
                                containerColor = colorFromHex("F1E3EB"),
                                contentColor = colorFromHex("6A4960")
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = colorFromHex("EF6797"),
                                contentColor = Color.White
                            )
                        }
                    ) {
                        Text(
                            stringResource(if (isFollowing) {
                                Res.string.cast_following
                            } else {
                                Res.string.cast_follow
                            })
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CastStatItem(
                    icon = Icons.Default.Groups,
                    label = stringResource(Res.string.cast_follower_label),
                    value = stringResource(Res.string.cast_follower_count, detail.cast.followerCount)
                )
            }
        }
    }
}

private const val DETAIL_TOOLTIP_DURATION_MILLIS = 5_000L

@Composable
private fun CastTodaySection(detail: CastDetail) {
    val todaySchedule = CastScheduleAttendanceUtils.todaySchedule(detail.schedule)
    val attendanceStatus = CastScheduleAttendanceUtils.attendanceStatus(todaySchedule)
    val statusText = castAttendanceStatusText(attendanceStatus)
    val timeText = todaySchedule?.let { schedule ->
        "${schedule.startTime} - ${schedule.endTime}"
    } ?: stringResource(Res.string.cast_today_check_schedule)
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
                    text = stringResource(Res.string.cast_today_status_title),
                    color = Color.White.copy(alpha = 0.82f)
                )
                Text(
                    text = statusText,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeText,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun castAttendanceStatusText(status: CastAttendanceStatus): String {
    return stringResource(
        when (status) {
            CastAttendanceStatus.UPCOMING -> Res.string.cast_today_upcoming
            CastAttendanceStatus.ON_SHIFT -> Res.string.cast_today_working
            CastAttendanceStatus.COMPLETED -> Res.string.cast_today_finished
            CastAttendanceStatus.OFF -> Res.string.cast_today_off
        }
    )
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
                text = stringResource(Res.string.cast_schedule_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
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
        color = if (isWorking) colorFromHex("EF6797") else MaterialTheme.colorScheme.surface,
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
                color = if (isWorking) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(if (isWorking) {
                    Res.string.cast_schedule_work
                } else {
                    Res.string.cast_schedule_off
                }),
                fontSize = 12.sp,
                color = if (isWorking) Color.White.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onSurfaceVariant
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
            text = stringResource(Res.string.cast_section_intro),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = detail.cast.desc,
                color = MaterialTheme.colorScheme.onSurface,
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
            text = stringResource(Res.string.cast_section_recent_activity),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CastActivityCard(
                modifier = Modifier
                    .weight(1f),
                value = detail.visitCertificationCount.toString(),
                label = stringResource(Res.string.cast_activity_visit_cert)
            )
            CastActivityCard(
                modifier = Modifier
                    .weight(1f),
                value = detail.cast.followerCount.toString(),
                label = stringResource(Res.string.cast_activity_follower)
            )
            CastActivityCard(
                modifier = Modifier
                    .weight(1f),
                value = RatingUtils.formatOneDecimal(detail.cast.rating),
                label = stringResource(Res.string.cast_activity_rating)
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
        color = MaterialTheme.colorScheme.surface,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            text = stringResource(Res.string.cast_section_tagged_reviews),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (reviews.isEmpty()) {
            CastRecentReviewEmptyView()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                reviews.forEach { review ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
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
                                        text = RatingUtils.formatOneDecimal(review.rating),
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                color = MaterialTheme.colorScheme.onSurface
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
        color = MaterialTheme.colorScheme.surface,
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
                text = stringResource(Res.string.cast_tagged_reviews_empty_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(Res.string.cast_tagged_reviews_empty_desc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val orderedDays = listOf(
        "월" to stringResource(Res.string.cast_weekday_mon),
        "화" to stringResource(Res.string.cast_weekday_tue),
        "수" to stringResource(Res.string.cast_weekday_wed),
        "목" to stringResource(Res.string.cast_weekday_thu),
        "금" to stringResource(Res.string.cast_weekday_fri),
        "토" to stringResource(Res.string.cast_weekday_sat),
        "일" to stringResource(Res.string.cast_weekday_sun)
    )
    return orderedDays.map { day ->
        WeeklyScheduleItem(
            dayLabel = day.second,
            isWorking = workingDays.contains(day.first)
        )
    }
}

private data class WeeklyScheduleItem(
    val dayLabel: String,
    val isWorking: Boolean
)
