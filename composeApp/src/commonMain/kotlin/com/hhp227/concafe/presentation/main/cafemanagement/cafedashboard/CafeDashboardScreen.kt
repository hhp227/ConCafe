package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

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
import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.PendingCastClaimPreview
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeDashboardScreen(
    cafeId: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: CafeDashboardViewModel = viewModel(
        key = "cafe-dashboard-$cafeId",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<CafeDashboardViewModel> { parametersOf(cafeId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val externalLinkSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CafeDashboardEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                CafeDashboardEvent.NavigateToBanner -> {
                    onNavigationAction(NavigationAction.NavigateToBanner(cafeId))
                }
                CafeDashboardEvent.NavigateToBannerEdit -> {
                    onNavigationAction(NavigationAction.NavigateToBannerEdit(cafeId))
                }
                is CafeDashboardEvent.NavigateToCafeInfoEdit -> {
                    onNavigationAction(NavigationAction.NavigateToCafeInfoEdit(event.cafeId))
                }
                is CafeDashboardEvent.NavigateToNoticeEvent -> {
                    onNavigationAction(NavigationAction.NavigateToNoticeEvent(event.cafeId))
                }
                is CafeDashboardEvent.NavigateToMenuGoods -> {
                    onNavigationAction(NavigationAction.NavigateToMenuGoods(event.cafeId))
                }
                is CafeDashboardEvent.NavigateToCastEdit -> {
                    onNavigationAction(NavigationAction.NavigateToCastEdit(event.cafeId, event.castId))
                }
                is CafeDashboardEvent.NavigateToSchedule -> {
                    onNavigationAction(NavigationAction.NavigateToSchedule(event.castId))
                }
                is CafeDashboardEvent.NavigateToExternalLink -> {
                    onNavigationAction(
                        NavigationAction.NavigateToExternalLink(
                            title = event.title,
                            url = event.url
                        )
                    )
                }
            }
        }
    }
    if (uiState.isDeleteCastDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(CafeDashboardAction.DismissDeleteCastDialog) },
            title = { Text("캐스트 프로필 삭제") },
            text = { Text("캐스트 프로필을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(CafeDashboardAction.ConfirmDeleteCast) }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(CafeDashboardAction.DismissDeleteCastDialog) }) {
                    Text("취소")
                }
            }
        )
    }
    if (uiState.isExternalLinkSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(CafeDashboardAction.DismissExternalLinkSheet) },
            containerColor = Color(0xFFFFFBFD),
            sheetState = externalLinkSheetState
        ) {
            ExternalLinkSheetContent(
                uiState = uiState,
                onAction = viewModel::onAction,
                onSubmit = { viewModel.onAction(CafeDashboardAction.SubmitExternalLink) }
            )
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
    val cafe = uiState.cafe

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(cafe?.name ?: "카페 관리")
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
                cafe?.let {
                    item {
                        DashboardHeroCard(cafe = it)
                    }
                }
                uiState.infoMessage?.let { message ->
                    item {
                        InfoBanner(
                            message = message,
                            onDismiss = { onAction(CafeDashboardAction.DismissInfoMessage) }
                        )
                    }
                }
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (cafe != null) {
                    item {
                        DashboardMetricGrid(cafe = cafe)
                    }
                    if (uiState.pendingCastClaims.isNotEmpty()) {
                        item {
                            PendingCastClaimSection(
                                claims = uiState.pendingCastClaims,
                                onApprove = { onAction(CafeDashboardAction.ClickApproveCastClaim(it)) },
                                onReject = { onAction(CafeDashboardAction.ClickRejectCastClaim(it)) }
                            )
                        }
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
                            casts = uiState.castPreviews,
                            hasMoreCasts = uiState.hasMoreCasts,
                            isLoadingMoreCasts = uiState.isLoadingMoreCasts,
                            onCastManagementClick = {
                                onAction(CafeDashboardAction.ClickShortcut(CafeDashboardShortcut.CAST_MANAGEMENT))
                            },
                            onDeleteClick = {
                                onAction(CafeDashboardAction.ClickDeleteCast)
                            },
                            canDelete = uiState.selectedCastId != null,
                            onScheduleClick = {
                                onAction(CafeDashboardAction.ClickShortcut(CafeDashboardShortcut.CAST_SCHEDULE))
                            },
                            selectedCastId = uiState.selectedCastId,
                            onCastScheduleSelect = { castId ->
                                onAction(CafeDashboardAction.ClickCastSchedule(castId))
                            },
                            onLoadMoreClick = {
                                onAction(CafeDashboardAction.ClickLoadMoreCasts)
                            }
                        )
                    }
                    if (uiState.externalLinks.isNotEmpty()) {
                        item {
                            ExternalLinkSection(
                                links = uiState.externalLinks,
                                onAddClick = {
                                    onAction(CafeDashboardAction.ClickShortcut(CafeDashboardShortcut.EXTERNAL_LINKS))
                                },
                                onItemClick = { linkId ->
                                    onAction(CafeDashboardAction.ClickExternalLinkItem(linkId))
                                },
                                onDeleteClick = { linkId ->
                                    onAction(CafeDashboardAction.ClickDeleteExternalLink(linkId))
                                }
                            )
                        }
                    }
                    item {
                        HomeBannerSection(
                            banner = cafe.homeBannerPreview,
                            onBannerClick = {
                                onAction(CafeDashboardAction.ClickShortcut(CafeDashboardShortcut.HOME_BANNER))
                            },
                            onCreateBannerClick = {
                                onAction(CafeDashboardAction.ClickCreateBanner)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalLinkSheetContent(
    uiState: CafeDashboardUiState,
    onAction: (CafeDashboardAction) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "외부 링크 추가",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "홈이나 카페 화면에서 연결할 외부 링크를 간단히 등록합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF7A707A)
        )
        ConCafeFormField(
            label = "제목",
            value = uiState.externalLinkTitle,
            onValueChange = { onAction(CafeDashboardAction.ChangeExternalLinkTitle(it)) },
            placeholder = "예: 공식 X 계정"
        )
        ConCafeFormField(
            label = "링크 URL",
            value = uiState.externalLinkUrl,
            onValueChange = { onAction(CafeDashboardAction.ChangeExternalLinkUrl(it)) },
            placeholder = "https://"
        )
        Button(
            onClick = onSubmit,
            enabled = uiState.isExternalLinkSubmitEnabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD1DC),
                contentColor = Color(0xFF2B2330),
                disabledContainerColor = Color(0xFFF4D7DF),
                disabledContentColor = Color(0xFF7F7078)
            )
        ) {
            Text("외부 링크 추가", fontWeight = FontWeight.Bold)
        }
        TextButton(
            onClick = { onAction(CafeDashboardAction.DismissExternalLinkSheet) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("닫기")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExternalLinkSection(
    links: List<CafeDashboardExternalLink>,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "외부 링크",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8C7A83)
                    )
                    Text(
                        text = "앱 외부로 연결할 링크를 관리합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7E7480)
                    )
                }
                TextButton(onClick = onAddClick) {
                    Text("추가", color = Color(0xFFEF6797))
                }
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFFBFD),
                border = BorderStroke(1.dp, Color(0xFFF0E6EC))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    links.forEach { link ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFD)),
                                border = BorderStroke(1.dp, Color(0xFFF0E6EC)),
                                onClick = { onItemClick(link.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(Color(0xFFFFD1DC), Color(0xFFFFE4EC))
                                                ),
                                                RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                    Text(
                                        text = link.title,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2B2330)
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteClick(link.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "외부 링크 삭제",
                                    tint = Color(0xFF8F848F)
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onAddClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD1DC),
                            contentColor = Color(0xFF2B2330)
                        )
                    ) {
                        Text("외부 링크 추가", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
private fun PendingCastClaimSection(
    claims: List<PendingCastClaimPreview>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("프로필 연결 요청", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        claims.take(3).forEach { claim ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${claim.requesterNickname} → ${claim.castName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(claim.requestedAtLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A808A))
                    }
                    claim.message?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5C5760))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onApprove(claim.claimId) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD1DC),
                                contentColor = Color(0xFF2B2330)
                            )
                        ) {
                            Text("승인", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onReject(claim.claimId) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("반려", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeroCard(
    cafe: CafeDashboardData
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
    cafe: CafeDashboardData
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
    onShortcutClick: (CafeDashboardShortcut) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = "관리 메뉴",
            subtitle = "선택한 카페 컨텍스트로 이동"
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    shortcut = CafeDashboardShortcut.EVENT_MANAGEMENT,
                    onClick = { onShortcutClick(CafeDashboardShortcut.EVENT_MANAGEMENT) }
                )
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    shortcut = CafeDashboardShortcut.CAFE_SETTINGS,
                    onClick = { onShortcutClick(CafeDashboardShortcut.CAFE_SETTINGS) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    shortcut = CafeDashboardShortcut.MENU_GOODS,
                    onClick = { onShortcutClick(CafeDashboardShortcut.MENU_GOODS) }
                )
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    shortcut = CafeDashboardShortcut.EXTERNAL_LINKS,
                    onClick = { onShortcutClick(CafeDashboardShortcut.EXTERNAL_LINKS) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutCard(
    modifier: Modifier = Modifier,
    shortcut: CafeDashboardShortcut,
    onClick: () -> Unit
) {
    val icon = when (shortcut) {
        CafeDashboardShortcut.CAST_MANAGEMENT -> Icons.Default.Groups
        CafeDashboardShortcut.CAST_SCHEDULE -> Icons.Default.CalendarMonth
        CafeDashboardShortcut.EVENT_MANAGEMENT -> Icons.Default.AutoAwesome
        CafeDashboardShortcut.CAFE_SETTINGS -> Icons.Default.Settings
        CafeDashboardShortcut.MENU_GOODS -> Icons.Default.RestaurantMenu
        CafeDashboardShortcut.HOME_BANNER -> Icons.Default.Campaign
        CafeDashboardShortcut.EXTERNAL_LINKS -> Icons.Default.Link
    }

    Card(
        modifier = modifier,
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
    casts: List<CafeCastPreview>,
    hasMoreCasts: Boolean,
    isLoadingMoreCasts: Boolean,
    onCastManagementClick: () -> Unit,
    onDeleteClick: () -> Unit,
    canDelete: Boolean,
    onScheduleClick: () -> Unit,
    selectedCastId: String?,
    onCastScheduleSelect: (String) -> Unit,
    onLoadMoreClick: () -> Unit
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDeleteClick,
                        enabled = canDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFFCE6EF),
                            contentColor = Color(0xFFEF6797),
                            disabledContainerColor = Color(0xFFF6EEF2),
                            disabledContentColor = Color(0xFFC8B7C0)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "캐스트 삭제"
                        )
                    }
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
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AddCastItem(onClick = onCastManagementClick)
                }
                items(casts, key = { it.id }) { cast ->
                    CastPreviewItem(
                        cast = cast,
                        isSelected = selectedCastId == cast.id,
                        onClick = { onCastScheduleSelect(cast.id) }
                    )
                }
                if (hasMoreCasts) {
                    item {
                        LoadMoreCastItem(
                            isLoading = isLoadingMoreCasts,
                            onClick = onLoadMoreClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CastPreviewItem(
    cast: CafeCastPreview,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(78.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(
                    if (isSelected) 2.dp else 0.dp,
                    if (isSelected) Color(0xFFEF6797) else Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .size(72.dp)
                )
            }
            Box(
                    modifier = Modifier
                        .matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD7E3), Color(0xFFFFF0F5))
                            ),
                            shape = CircleShape
                        )
                )
                if (isSelected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp),
                        shape = CircleShape,
                        color = Color(0xFFEF6797)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(12.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(16.dp)
                        .background(
                            if (cast.isOnShift) Color(0xFF35C26B) else Color(0xFFC7CBD3),
                            CircleShape
                        )
                )
            }
        }
        Text(
            text = cast.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFFEF6797) else Color(0xFF2B2330)
        )
    }
}

@Composable
private fun LoadMoreCastItem(
    isLoading: Boolean,
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
            color = Color(0xFFF7F2F6),
            border = BorderStroke(1.dp, Color(0xFFE3DCE3)),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFB8AEB7)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "캐스트 더 보기",
                        tint = Color(0xFF8F848F)
                    )
                }
            }
        }
        Text(
            text = if (isLoading) "불러오는 중" else "더 보기",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8F848F),
            fontWeight = FontWeight.Bold
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
    banner: CafeDashboardData.HomeBannerPreview,
    onBannerClick: () -> Unit,
    onCreateBannerClick: () -> Unit
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
                        onClick = onCreateBannerClick,
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
