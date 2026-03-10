package org.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.hhp227.concafe.presentation.navigation.NavigationAction

@Composable
fun CafeDashboardScreen(
    cafeId: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: CafeDashboardViewModel = viewModel(
        key = "cafe-dashboard-$cafeId",
        factory = viewModelFactory {
            initializer {
                CafeDashboardViewModel(cafeId = cafeId)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CafeDashboardEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
            }
        }
    }
    CafeDashboardContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CafeDashboardContentScreen(
    uiState: CafeDashboardUiState,
    onAction: (CafeDashboardAction) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.cafe.name)
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CafeDashboardAction.ClickBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF7FB), Color(0xFFFFEEF6), Color(0xFFFFFBFD))
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    DashboardHeroCard(cafe = uiState.cafe)
                }
                uiState.infoMessage?.let { message ->
                    item {
                        InfoBanner(
                            message = message,
                            onDismiss = { onAction(CafeDashboardAction.DismissInfoMessage) }
                        )
                    }
                }
                item {
                    DashboardMetricGrid(cafe = uiState.cafe)
                }
                item {
                    ShortcutGrid(
                        onShortcutClick = { shortcut ->
                            onAction(CafeDashboardAction.ClickShortcut(shortcut))
                        }
                    )
                }
                item {
                    CastManagementSection(
                        casts = uiState.cafe.castPreviews,
                        onCastManagementClick = {
                            onAction(CafeDashboardAction.ClickShortcut(CafeDashboardUiState.Shortcut.CAST_MANAGEMENT))
                        },
                        onScheduleClick = {
                            onAction(CafeDashboardAction.ClickShortcut(CafeDashboardUiState.Shortcut.CAST_SCHEDULE))
                        }
                    )
                }
                item {
                    HomeBannerSection(
                        banner = uiState.cafe.homeBannerPreview,
                        onBannerClick = {
                            onAction(CafeDashboardAction.ClickShortcut(CafeDashboardUiState.Shortcut.HOME_BANNER))
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun DashboardHeroCard(
    cafe: CafeDashboardUiState.CafeSummary
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF2F1B3A), Color(0xFF7C3F67), Color(0xFFF06A9D))
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column {
                        Text(
                            text = cafe.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = cafe.city,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Text(
                    text = "선택한 카페의 운영 수치와 관리 진입점을 한 화면에서 확인합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun InfoBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF6D7),
        border = BorderStroke(1.dp, Color(0xFFF1D88D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B5320)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "안내 닫기", tint = Color(0xFF6B5320))
            }
        }
    }
}

@Composable
private fun DashboardMetricGrid(
    cafe: CafeDashboardUiState.CafeSummary
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "운영 대시보드",
            subtitle = "오늘 기준 핵심 수치"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = "오늘 체크인",
                value = cafe.todayCheckIns.toString(),
                accent = Color(0xFFEF6797)
            )
            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = "오늘 리뷰",
                value = cafe.todayReviews.toString(),
                accent = Color(0xFF47A88B)
            )
            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = "평점",
                value = formatRating(cafe.rating),
                accent = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8DFE7))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accent, CircleShape)
            )
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A707A))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2B2330)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutGrid(
    onShortcutClick: (CafeDashboardUiState.Shortcut) -> Unit
) {
    val primaryShortcuts = listOf(
        CafeDashboardUiState.Shortcut.EVENT_MANAGEMENT,
        CafeDashboardUiState.Shortcut.CAFE_SETTINGS,
        CafeDashboardUiState.Shortcut.MENU_GOODS,
        CafeDashboardUiState.Shortcut.EXTERNAL_LINKS
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = "관리 메뉴",
            subtitle = "선택한 카페 컨텍스트로 이동"
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2
        ) {
            primaryShortcuts.forEach { shortcut ->
                ShortcutCard(
                    shortcut = shortcut,
                    onClick = { onShortcutClick(shortcut) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutCard(
    shortcut: CafeDashboardUiState.Shortcut,
    onClick: () -> Unit
) {
    val icon = when (shortcut) {
        CafeDashboardUiState.Shortcut.CAST_MANAGEMENT -> Icons.Default.Groups
        CafeDashboardUiState.Shortcut.CAST_SCHEDULE -> Icons.Default.CalendarMonth
        CafeDashboardUiState.Shortcut.EVENT_MANAGEMENT -> Icons.Default.AutoAwesome
        CafeDashboardUiState.Shortcut.CAFE_SETTINGS -> Icons.Default.Settings
        CafeDashboardUiState.Shortcut.MENU_GOODS -> Icons.Default.RestaurantMenu
        CafeDashboardUiState.Shortcut.HOME_BANNER -> Icons.Default.Campaign
        CafeDashboardUiState.Shortcut.EXTERNAL_LINKS -> Icons.Default.Link
    }

    Card(
        modifier = Modifier.fillMaxWidth(0.48f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8DFE7)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFCE6EF)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFEF6797),
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                text = shortcut.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2B2330)
            )
        }
    }
}

@Composable
private fun CastManagementSection(
    casts: List<CafeDashboardUiState.CastPreview>,
    onCastManagementClick: () -> Unit,
    onScheduleClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8DFE7))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "소속 캐스트 관리",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8C7A83)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFFCE6EF),
                    onClick = onScheduleClick
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFEF6797),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "출근표 관리",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFEF6797),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(casts, key = { it.id }) { cast ->
                    CastPreviewItem(cast = cast)
                }
                item {
                    AddCastItem(onClick = onCastManagementClick)
                }
            }
        }
    }
}

@Composable
private fun CastPreviewItem(
    cast: CafeDashboardUiState.CastPreview
) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFD7E3), Color(0xFFFFF0F5))
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        if (cast.isOnShift) Color(0xFF35C26B) else Color(0xFFC7CBD3),
                        CircleShape
                    )
            )
        }
        Text(
            text = cast.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B2330)
        )
    }
}

@Composable
private fun AddCastItem(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(2.dp, Color(0xFFE3DCE3)),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "캐스트 추가",
                    tint = Color(0xFFB8AEB7)
                )
            }
        }
        Text(
            text = "추가",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8F848F)
        )
    }
}

@Composable
private fun HomeBannerSection(
    banner: CafeDashboardUiState.HomeBannerPreview,
    onBannerClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8DFE7))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "홈 배너 관리",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8C7A83)
                )
                TextButton(onClick = onBannerClick) {
                    Text("전체 보기")
                }
            }
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFD)),
                border = BorderStroke(1.dp, Color(0xFFF0E6EC))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 96.dp, height = 64.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFD1DC), Color(0xFFFFE4EC))
                                    ),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = banner.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2B2330)
                            )
                            Text(
                                text = banner.period,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7E7480)
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color(0xFFE8F7EE)
                            ) {
                                Text(
                                    text = banner.statusLabel,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2F8B57),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onBannerClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD1DC),
                            contentColor = Color(0xFF2B2330)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null
                        )
                        Text(
                            text = "새 배너 등록하기",
                            modifier = Modifier.padding(start = 6.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B2330)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF786E7A)
        )
    }
}

private fun formatRating(rating: Double): String {
    return if (rating <= 0) {
        "-"
    } else {
        val normalized = (rating * 10).toInt() / 10.0
        normalized.toString()
    }
}
