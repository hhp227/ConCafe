package com.hhp227.concafe.presentation.main.fanmanagement

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.FanManagementData
import com.hhp227.concafe.presentation.navigation.NavigationAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FanManagementScreen(
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: FanManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val claimSheet = uiState.castClaimSheet
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is FanManagementEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is FanManagementEvent.NavigateToCastEdit -> {
                    onNavigationAction(
                        NavigationAction.NavigateToCastEdit(
                            cafeId = event.cafeId,
                            castId = event.castId
                        )
                    )
                }
                FanManagementEvent.NavigateToSchedule -> {
                    onNavigationAction(NavigationAction.NavigateToSchedule())
                }
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        FanManagementContentScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState,
            onAction = viewModel::onAction
        )
        if (uiState.isClaimSheetVisible && claimSheet != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { viewModel.onAction(FanManagementAction.DismissClaimSheet) },
                sheetState = sheetState
            ) {
                CastClaimSheet(
                    sheet = claimSheet,
                    onSelect = { viewModel.onAction(FanManagementAction.SelectClaimCandidate(it)) },
                    onSubmit = { viewModel.onAction(FanManagementAction.SubmitCastClaim) },
                    onDismiss = { viewModel.onAction(FanManagementAction.DismissClaimSheet) }
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun FanManagementContentScreen(
    modifier: Modifier = Modifier,
    uiState: FanManagementUiState,
    onAction: (FanManagementAction) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8F5F6), Color(0xFFFFF8FB), Color(0xFFFFEFF5))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            uiState.isLoading -> {
                LoadingState()
            }
            uiState.fanManagementData == null && uiState.castClaimStatus == null -> {
                EmptySectionCard(message = uiState.errorMessage ?: "로그인한 캐스트 정보를 찾을 수 없습니다.")
            }
            else -> Unit
        }
        if (uiState.infoMessage != null) {
            InfoBanner(
                message = uiState.infoMessage,
                onDismiss = { onAction(FanManagementAction.DismissInfoMessage) }
            )
        }
        uiState.castClaimStatus?.let { status ->
            CastClaimStatusCard(
                status = status,
                onClick = { onAction(FanManagementAction.ClickClaimProfile) }
            )
        }
        PrimaryAnnouncementButton(
            onClick = { onAction(FanManagementAction.ClickPrimaryAnnouncement) }
        )
        QuickActionGrid(
            onActionClick = { onAction(FanManagementAction.ClickQuickAction(it)) }
        )
        WeeklyScheduleSection(
            schedule = uiState.fanManagementData?.detail?.schedule.orEmpty()
        )
        RecentFollowersSection(
            followers = uiState.recentFollowers,
            onFollowerClick = { onAction(FanManagementAction.ClickRecentFollower(it)) }
        )
        TopFansSection(
            topFans = uiState.topFans,
            onFanClick = { onAction(FanManagementAction.ClickTopFan(it)) }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun CastClaimStatusCard(
    status: FanManagementUiState.CastClaimStatusCard,
    onClick: () -> Unit
) {
    val accent = when (status.accent) {
        FanManagementUiState.Accent.PENDING -> Color(0xFFFFD1DC)
        FanManagementUiState.Accent.LINKED -> Color(0xFFEAF8EF)
        FanManagementUiState.Accent.REJECTED -> Color(0xFFF8E9EE)
    }
    val contentColor = when (status.accent) {
        FanManagementUiState.Accent.PENDING -> Color(0xFF6B3050)
        FanManagementUiState.Accent.LINKED -> Color(0xFF2E8B57)
        FanManagementUiState.Accent.REJECTED -> Color(0xFF8B4A5A)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accent
            ) {
                Text(
                    text = status.affiliatedCafeName,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = status.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF24161E)
            )
            Text(
                text = status.body,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6C6270)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "프로필 연결 상태 보기",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEF6797)
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFEF6797))
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    fanManagementData: FanManagementData,
    onAction: (FanManagementAction) -> Unit
) {
    val cast = fanManagementData.detail.cast
    val cafe = fanManagementData.detail.cafe
    val localizedName = fanManagementData.user.nickname.takeIf { it != cast.name }.orEmpty()
    val profileAccent = cast.name.take(2).uppercase()
    val isOnline = fanManagementData.detail.schedule.isNotEmpty()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD7E5), Color(0xFFF2ADC2))
                            )
                        )
                        .border(2.dp, Color(0xFFFFD1DC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profileAccent,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3F67)
                    )
                }
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF37B26C))
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = cast.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF24161E)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (localizedName.isNotBlank()) {
                            Text(
                                text = localizedName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF7A707A)
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.clickable { onAction(FanManagementAction.ClickEditProfile) },
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0x14FFD1DC)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "프로필 수정",
                                tint = Color(0xFF7C3F67),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "수정",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3F67)
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = Color(0xFFEF6797),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = cafe.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A57)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    stats: List<FanManagementUiState.StatCard>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.forEach { stat ->
            val isPrimary = stat.highlight == FanManagementUiState.Highlight.PRIMARY

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = if (isPrimary) Color(0x1AFFD1DC) else Color.White.copy(alpha = 0.92f),
                tonalElevation = if (isPrimary) 0.dp else 2.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isPrimary) Color(0x33FFB3C6) else Color(0x1AFFD1DC)
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stat.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7A707A),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stat.value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isPrimary) Color(0xFFD94A82) else Color(0xFF24161E)
                    )
                }
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
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1D88D))
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
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B5320),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "닫기",
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B5320)
            )
        }
    }
}

@Composable
private fun PrimaryAnnouncementButton(
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
            containerColor = Color(0xFFFFD1DC),
            contentColor = Color(0xFF24161E)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Campaign, contentDescription = null)
                Text(
                    text = "팬 공지 작성하기",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun QuickActionGrid(
    onActionClick: (FanManagementUiState.QuickAction) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        FanManagementUiState.QuickAction.entries.forEach { quickAction ->
            val icon = when (quickAction) {
                FanManagementUiState.QuickAction.WORK_SCHEDULE -> Icons.Default.CalendarMonth
                FanManagementUiState.QuickAction.CAFE_DASHBOARD -> Icons.Default.Storefront
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onActionClick(quickAction) },
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFD1DC))
            ) {
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x14FFD1DC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color(0xFF5D525B)
                            )
                        }
                        Text(
                            text = quickAction.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF24161E)
                        )
                        Text(
                            text = quickAction.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF7A707A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CastClaimSheet(
    sheet: FanManagementUiState.CastClaimSheet,
    onSelect: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sheet.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(sheet.affiliatedCafeName, style = MaterialTheme.typography.labelLarge, color = Color(0xFFEF6797))
                }
                TextButton(onClick = onDismiss) {
                    Text("닫기")
                }
            }
            Text(sheet.body, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6C6270))
            if (sheet.requestableCasts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sheet.requestableCasts.forEach { candidate ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(candidate.id) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (sheet.selectedCastId == candidate.id) Color(0xFFFFD1DC) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFD1DC))
                        ) {
                            Text(
                                text = candidate.name,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF24161E)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (sheet.canSubmit) {
            Surface(
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                ElevatedButton(
                    onClick = onSubmit,
                    enabled = !sheet.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFFFFD1DC),
                        contentColor = Color(0xFF24161E)
                    )
                ) {
                    Text(if (sheet.isSubmitting) "요청 보내는 중..." else "연결 요청 보내기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RecentFollowersSection(
    followers: List<FanManagementUiState.RecentFollower>,
    onFollowerClick: (String) -> Unit
) {
    SectionCard(
        title = "최근 팔로워"
    ) {
        if (followers.isEmpty()) {
            EmptySectionCard(message = "최근 팔로워 데이터가 아직 없습니다.")
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                followers.forEach { follower ->
                    Column(
                        modifier = Modifier
                            .width(74.dp)
                            .clickable { onFollowerClick(follower.id) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(
                                    if (follower.accent) Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFD7E5), Color(0xFFF2ADC2))
                                    ) else Brush.linearGradient(
                                        colors = listOf(Color(0xFFF2EEF1), Color(0xFFE3D9E2))
                                    )
                                )
                                .border(
                                    width = if (follower.accent) 2.dp else 0.dp,
                                    color = if (follower.accent) Color(0xFFFFD1DC) else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = follower.initial,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6E5566)
                            )
                        }
                        Text(
                            text = follower.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF24161E),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = follower.joinedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9C8C98),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                AddFollowerButton()
            }
        }
    }
}

@Composable
private fun WeeklyScheduleSection(
    schedule: List<CastSchedule>
) {
    val weeklyStatus = rememberWeeklySchedule(schedule)

    SectionCard(title = "주간 출근") {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White.copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFD1DC))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x14FFD1DC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFEF6797),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "이번 주 스케줄",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF24161E)
                        )
                        Text(
                            text = "출근 관리에서 일정을 바로 조정할 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7A707A)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    weeklyStatus.forEach { item ->
                        WeeklyScheduleItemCard(
                            modifier = Modifier.weight(1f),
                            dayLabel = item.dayLabel,
                            isWorking = item.isWorking
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyScheduleItemCard(
    modifier: Modifier = Modifier,
    dayLabel: String,
    isWorking: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isWorking) Color(0xFFEF6797) else Color(0xFFFDF8FA),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isWorking) Color.Transparent else Color(0x1AFFD1DC)
        )
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
                style = MaterialTheme.typography.labelLarge,
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
private fun AddFollowerButton() {
    Column(
        modifier = Modifier.width(74.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Color(0xFFF8F1F4))
                .border(1.dp, Color(0xFFD9CBD4), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = Color(0xFFA28E9B)
            )
        }
        Text(
            text = "팬 확장",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF7A707A),
            textAlign = TextAlign.Center
        )
    }
}

private data class WeeklyScheduleStatus(
    val dayLabel: String,
    val isWorking: Boolean
)

private fun rememberWeeklySchedule(schedule: List<CastSchedule>): List<WeeklyScheduleStatus> {
    val workingDays = schedule.mapNotNull { it.date.toWeekdayLabelOrNull() }.toSet()

    return listOf("월", "화", "수", "목", "금", "토", "일").map { dayLabel ->
        WeeklyScheduleStatus(
            dayLabel = dayLabel,
            isWorking = workingDays.contains(dayLabel)
        )
    }
}

private fun String.toWeekdayLabelOrNull(): String? {
    val parts = split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null

    return listOf("월", "화", "수", "목", "금", "토", "일").getOrNull(dayOfWeekIndex(year, month, day))
}

private fun dayOfWeekIndex(year: Int, month: Int, day: Int): Int {
    var adjustedYear = year
    var adjustedMonth = month
    if (adjustedMonth < 3) {
        adjustedMonth += 12
        adjustedYear -= 1
    }
    val k = adjustedYear % 100
    val j = adjustedYear / 100
    val h = (day + (13 * (adjustedMonth + 1)) / 5 + k + (k / 4) + (j / 4) + (5 * j)) % 7
    return when (h) {
        2 -> 0
        3 -> 1
        4 -> 2
        5 -> 3
        6 -> 4
        0 -> 5
        else -> 6
    }
}

@Composable
private fun TopFansSection(
    topFans: List<FanManagementUiState.TopFan>,
    onFanClick: (String) -> Unit
) {
    SectionCard(
        title = "이달의 TOP 팬"
    ) {
        if (topFans.isEmpty()) {
            EmptySectionCard(message = "TOP 팬 집계 데이터가 아직 없습니다.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topFans.forEach { fan ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFanClick(fan.id) },
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFD1DC))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = fan.rank.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (fan.rank) {
                                    1 -> Color(0xFFD99A00)
                                    2 -> Color(0xFF8E8896)
                                    else -> Color(0xFFDC8346)
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF6E3EC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fan.name.take(1),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3F67)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = fan.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF24161E)
                                )
                                Text(
                                    text = "포인트: ${fan.pointsLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF7A707A)
                                )
                            }
                            if (fan.isBest) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color(0x1AFFD1DC)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = Color(0xFFD94A82),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "BEST",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD94A82)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.9f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "팬관리 정보를 불러오는 중입니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7A707A)
            )
        }
    }
}

@Composable
private fun EmptySectionCard(message: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFD1DC))
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF7A707A),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF24161E)
                )
                if (actionLabel != null) {
                    Text(
                        text = actionLabel,
                        modifier = if (onActionClick != null) Modifier.clickable(onClick = onActionClick) else Modifier,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7A707A)
                    )
                }
            }
            content()
        }
    }
}
