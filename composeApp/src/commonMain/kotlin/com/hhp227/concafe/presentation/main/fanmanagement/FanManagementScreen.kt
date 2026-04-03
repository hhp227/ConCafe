package com.hhp227.concafe.presentation.main.fanmanagement

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.core.util.TimeUtils
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.FanFollower
import com.hhp227.concafe.domain.model.FanManagementData
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.keyboardBottomInsets
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FanManagementScreen(
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: FanManagementViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<FanManagementViewModel>() }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val claimSheet = uiState.castClaimSheet
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.onAction(FanManagementAction.Refresh)
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
                    onLoadMore = { viewModel.onAction(FanManagementAction.LoadMoreClaimCandidates) },
                    onSubmit = { viewModel.onAction(FanManagementAction.SubmitCastClaim) },
                    onDismiss = { viewModel.onAction(FanManagementAction.DismissClaimSheet) }
                )
            }
        }
        if (uiState.isAnnouncementSheetVisible) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { viewModel.onAction(FanManagementAction.DismissAnnouncementSheet) },
                sheetState = sheetState
            ) {
                FanAnnouncementSheetContent(
                    uiState = uiState,
                    onAction = viewModel::onAction
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
                EmptySectionCard(message = uiState.errorMessage ?: stringResource(Res.string.fanmanagement_error_cast_not_found))
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
            followers = uiState.fanManagementData?.followers.orEmpty(),
            onFollowerClick = { onAction(FanManagementAction.ClickRecentFollower(it)) }
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
                    text = stringResource(Res.string.fanmanagement_action_profile_link_status),
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
                                contentDescription = stringResource(Res.string.fanmanagement_accessibility_edit_profile),
                                tint = Color(0xFF7C3F67),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(Res.string.fanmanagement_action_edit),
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
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B5320),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(Res.string.common_close),
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
        colors = ButtonDefaults.elevatedButtonColors(
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
                    text = stringResource(Res.string.fanmanagement_announcement_title),
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
                border = BorderStroke(1.dp, Color(0x1AFFD1DC))
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
                            text = when (quickAction) {
                                FanManagementUiState.QuickAction.WORK_SCHEDULE -> stringResource(Res.string.fanmanagement_quick_action_schedule_title)
                                FanManagementUiState.QuickAction.CAFE_DASHBOARD -> stringResource(Res.string.fanmanagement_quick_action_profile_title)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF24161E)
                        )
                        Text(
                            text = when (quickAction) {
                                FanManagementUiState.QuickAction.WORK_SCHEDULE -> stringResource(Res.string.fanmanagement_quick_action_schedule_subtitle)
                                FanManagementUiState.QuickAction.CAFE_DASHBOARD -> stringResource(Res.string.fanmanagement_quick_action_profile_subtitle)
                            },
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
    onLoadMore: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .keyboardBottomInsets()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
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
                        Text(stringResource(Res.string.common_close))
                    }
                }
            }
            item {
                Text(sheet.body, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6C6270))
            }
            if (sheet.requestableCasts.isNotEmpty()) {
                itemsIndexed(sheet.requestableCasts, key = { _, candidate -> candidate.castId }) { index, candidate ->
                    if (index == sheet.requestableCasts.lastIndex && sheet.canLoadMore && !sheet.isLoadingMore) {
                        LaunchedEffect(candidate.castId, sheet.requestableCasts.size) {
                            onLoadMore()
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(candidate.castId) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (sheet.selectedCastId == candidate.castId) Color(0xFFFFD1DC) else Color.White,
                        border = BorderStroke(1.dp, Color(0x1AFFD1DC))
                    ) {
                        Text(
                            text = candidate.castName,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF24161E)
                        )
                    }
                }
            }
            if (sheet.canLoadMore || sheet.isLoadingMore) {
                item {
                    Text(
                        text = if (sheet.isLoadingMore) stringResource(Res.string.fanmanagement_claim_load_more_loading) else stringResource(Res.string.fanmanagement_claim_load_more_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A707A)
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
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
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFFFFD1DC),
                        contentColor = Color(0xFF24161E)
                    )
                ) {
                    Text(
                        if (sheet.isSubmitting) stringResource(Res.string.fanmanagement_claim_submitting) else stringResource(Res.string.fanmanagement_claim_submit),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentFollowersSection(
    followers: List<FanFollower>,
    onFollowerClick: (String) -> Unit
) {
    SectionCard(
        title = stringResource(Res.string.fanmanagement_section_recent_followers)
    ) {
        if (followers.isEmpty()) {
            EmptySectionCard(message = stringResource(Res.string.fanmanagement_followers_empty))
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                followers.take(10).forEachIndexed { index, follower ->
                    val accent = index == 0
                    val joinedLabel = follower.followedAt.toRelativeFollowerTimeLabel()
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
                                    if (accent) Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFD7E5), Color(0xFFF2ADC2))
                                    ) else Brush.linearGradient(
                                        colors = listOf(Color(0xFFF2EEF1), Color(0xFFE3D9E2))
                                    )
                                )
                                .border(
                                    width = if (accent) 2.dp else 0.dp,
                                    color = if (accent) Color(0xFFFFD1DC) else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = follower.nickname.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6E5566)
                            )
                        }
                        Text(
                            text = follower.nickname,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF24161E),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = joinedLabel,
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
private fun String.toRelativeFollowerTimeLabel(): String {
    val followedAt = runCatching {
        Instant.parse(this)
    }.getOrNull()
    if (followedAt == null) {
        return stringResource(Res.string.fanmanagement_relative_recent)
    }
    val now = Clock.System.now()
    val diffSeconds = (now.epochSeconds - followedAt.epochSeconds).coerceAtLeast(0)
    return when {
        diffSeconds < 60 -> stringResource(Res.string.fanmanagement_relative_just_now)
        diffSeconds < 3600 -> stringResource(Res.string.fanmanagement_relative_minutes_ago, diffSeconds / 60)
        diffSeconds < 86_400 -> stringResource(Res.string.fanmanagement_relative_hours_ago, diffSeconds / 3600)
        diffSeconds < 2_592_000 -> stringResource(Res.string.fanmanagement_relative_days_ago, diffSeconds / 86_400)
        else -> stringResource(Res.string.fanmanagement_relative_long_ago)
    }
}

@Composable
private fun FanAnnouncementSheetContent(
    uiState: FanManagementUiState,
    onAction: (FanManagementAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.82f)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.fanmanagement_announcement_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onAction(FanManagementAction.DismissAnnouncementSheet) }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(Res.string.fanmanagement_accessibility_close),
                    tint = Color(0xFF7A707A)
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                ConCafeFormField(
                    label = stringResource(Res.string.fanmanagement_announcement_label_title),
                    value = uiState.announcementTitle,
                    onValueChange = { onAction(FanManagementAction.ChangeAnnouncementTitle(it)) },
                    placeholder = stringResource(Res.string.fanmanagement_announcement_placeholder_title)
                )
            }
            item {
                ConCafeFormField(
                    label = stringResource(Res.string.fanmanagement_announcement_label_body),
                    value = uiState.announcementBody,
                    onValueChange = { onAction(FanManagementAction.ChangeAnnouncementBody(it)) },
                    placeholder = stringResource(Res.string.fanmanagement_announcement_placeholder_body),
                    minLines = 7,
                    singleLine = false
                )
            }
            item {
                Text(
                    text = stringResource(Res.string.fanmanagement_announcement_hint_push),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A8087)
                )
            }
        }
        Surface(
            color = Color.Transparent,
            modifier = Modifier.imePadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFF8F5F6), Color(0xFFF8F5F6))
                        )
                    )
            ) {
                Button(
                    onClick = { onAction(FanManagementAction.SubmitAnnouncement) },
                    enabled = uiState.isAnnouncementSubmitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                        .navigationBarsPadding()
                        .height(60.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD1DC),
                        contentColor = Color(0xFF2B2330),
                        disabledContainerColor = Color(0xFFFFE6EE),
                        disabledContentColor = Color(0xFFBCAAB3)
                    )
                ) {
                    if (uiState.isSendingAnnouncement) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp),
                            color = Color(0xFF2B2330)
                        )
                    } else {
                        Text(
                            stringResource(Res.string.fanmanagement_announcement_submit),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyScheduleSection(
    schedule: List<CastSchedule>
) {
    val weeklyStatus = rememberWeeklySchedule(schedule)

    SectionCard(title = stringResource(Res.string.fanmanagement_section_weekly_work)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, Color(0x1AFFD1DC))
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
                            text = stringResource(Res.string.fanmanagement_weekly_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF24161E)
                        )
                        Text(
                            text = stringResource(Res.string.fanmanagement_weekly_subtitle),
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
        border = BorderStroke(
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
                text = if (isWorking) stringResource(Res.string.schedule_status_work) else stringResource(Res.string.schedule_status_off),
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
            text = stringResource(Res.string.fanmanagement_fan_expand),
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

@Composable
private fun rememberWeeklySchedule(schedule: List<CastSchedule>): List<WeeklyScheduleStatus> {
    val workingDays = schedule.mapNotNull { TimeUtils.weekdayLabelFromIsoDateOrNull(it.date) }.toSet()
    return listOf("월", "화", "수", "목", "금", "토", "일").map { dayLabel ->
        WeeklyScheduleStatus(
            dayLabel = when (dayLabel) {
                "월" -> stringResource(Res.string.fanmanagement_weekday_mon)
                "화" -> stringResource(Res.string.fanmanagement_weekday_tue)
                "수" -> stringResource(Res.string.fanmanagement_weekday_wed)
                "목" -> stringResource(Res.string.fanmanagement_weekday_thu)
                "금" -> stringResource(Res.string.fanmanagement_weekday_fri)
                "토" -> stringResource(Res.string.fanmanagement_weekday_sat)
                "일" -> stringResource(Res.string.fanmanagement_weekday_sun)
                else -> dayLabel
            },
            isWorking = workingDays.contains(dayLabel)
        )
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
                text = stringResource(Res.string.fanmanagement_loading),
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
        border = BorderStroke(1.dp, Color(0x1AFFD1DC))
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
