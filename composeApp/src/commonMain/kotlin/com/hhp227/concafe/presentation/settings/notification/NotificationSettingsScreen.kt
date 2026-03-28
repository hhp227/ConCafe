package com.hhp227.concafe.presentation.settings.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.NotificationQuietHoursMode
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<NotificationSettingsViewModel>() }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                NotificationSettingsEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is NotificationSettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("알림 설정") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(NotificationSettingsAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            NotificationSettingsContentScreen(
                uiState = uiState,
                innerPadding = innerPadding,
                onAction = viewModel::onAction
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsContentScreen(
    uiState: NotificationSettingsUiState,
    innerPadding: PaddingValues,
    onAction: (NotificationSettingsAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD)),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = innerPadding.calculateTopPadding() + 20.dp,
            end = 16.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            NotificationSettingsHeroCard(uiState = uiState)
        }
        item {
            NotificationSettingCard(title = "기본 수신") {
                NotificationToggleRow(
                    icon = Icons.Default.Notifications,
                    iconBackground = Color(0xFFFFE6F1),
                    iconTint = Color(0xFFEB5F97),
                    title = "푸시 알림 받기",
                    description = "새 공지와 팬 활동 업데이트를 앱 푸시로 받아요.",
                    checked = uiState.isPushNotificationsEnabled,
                    onCheckedChange = { onAction(NotificationSettingsAction.TogglePushNotifications(it)) },
                    enabled = !uiState.isSaving
                )
            }
        }
        item {
            NotificationSettingCard(title = "알림 종류") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NotificationToggleRow(
                        icon = Icons.Default.WorkHistory,
                        iconBackground = Color(0xFFE4F7EC),
                        iconTint = Color(0xFF2E9E5B),
                        title = "출근 알림",
                        description = "팔로우한 캐스트의 오늘 출근 소식을 빠르게 받아요.",
                        checked = uiState.isShiftNotificationsEnabled,
                        onCheckedChange = { onAction(NotificationSettingsAction.ToggleShiftNotifications(it)) },
                        enabled = !uiState.isSaving
                    )
                    NotificationToggleRow(
                        icon = Icons.Default.Cake,
                        iconBackground = Color(0xFFFFE6F1),
                        iconTint = Color(0xFFEB5F97),
                        title = "생일 알림",
                        description = "생일이 다가오는 캐스트와 당일 이벤트를 놓치지 않아요.",
                        checked = uiState.isBirthdayNotificationsEnabled,
                        onCheckedChange = { onAction(NotificationSettingsAction.ToggleBirthdayNotifications(it)) },
                        enabled = !uiState.isSaving
                    )
                    NotificationToggleRow(
                        icon = Icons.Default.Campaign,
                        iconBackground = Color(0xFFE8F0FF),
                        iconTint = Color(0xFF4A79E8),
                        title = "공지 알림",
                        description = "카페 공지와 이벤트 업데이트를 우선적으로 받아요.",
                        checked = uiState.isNoticeNotificationsEnabled,
                        onCheckedChange = { onAction(NotificationSettingsAction.ToggleNoticeNotifications(it)) },
                        enabled = !uiState.isSaving
                    )
                }
            }
        }
        item {
            NotificationSettingCard(title = "조용한 시간") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "밤 시간이나 하루 요약 모드를 선택해 알림 강도를 조절할 수 있어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7C7480)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NotificationQuietHoursMode.entries.forEach { option ->
                            FilterChip(
                                selected = uiState.quietHoursOption == option,
                                onClick = { onAction(NotificationSettingsAction.SelectQuietHours(option)) },
                                label = { Text(option.titleText()) },
                                enabled = !uiState.isSaving
                            )
                        }
                    }
                    QuietHoursDescriptionCard(option = uiState.quietHoursOption)
                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC33E6A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsHeroCard(uiState: NotificationSettingsUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFEF6797), Color(0xFFF7A0C1))
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "알림 스타일을 취향에 맞게 조절하세요",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = heroSummary(uiState),
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun NotificationSettingCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun NotificationToggleRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 12.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                color = Color(0xFF7C7480),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFEF6797)
            )
        )
    }
}

@Composable
private fun QuietHoursDescriptionCard(option: NotificationQuietHoursMode) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFFFD6E5),
                shape = RoundedCornerShape(18.dp)
            )
            .background(Color(0xFFFFF6FA), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = option.titleText(),
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB84473)
            )
            Text(
                text = option.descriptionText(),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7C7480)
            )
        }
    }
}

private fun heroSummary(uiState: NotificationSettingsUiState): String {
    val enabledCount = listOf(
        uiState.isPushNotificationsEnabled,
        uiState.isShiftNotificationsEnabled,
        uiState.isBirthdayNotificationsEnabled,
        uiState.isNoticeNotificationsEnabled
    ).count { it }
    return "현재 ${enabledCount}개 알림을 켜 두었고, ${uiState.quietHoursOption.titleText()} 모드로 받을 예정입니다."
}

private fun NotificationQuietHoursMode.titleText(): String {
    return when (this) {
        NotificationQuietHoursMode.OFF -> "즉시 받기"
        NotificationQuietHoursMode.NIGHT -> "밤 시간만 조용히"
        NotificationQuietHoursMode.ALL_DAY -> "요약만 받기"
    }
}

private fun NotificationQuietHoursMode.descriptionText(): String {
    return when (this) {
        NotificationQuietHoursMode.OFF -> "중요 알림을 포함해 들어오는 즉시 알려드려요."
        NotificationQuietHoursMode.NIGHT -> "밤 11시부터 오전 8시까지는 조용히 보관해요."
        NotificationQuietHoursMode.ALL_DAY -> "하루 동안 모아 저녁 시간에 한 번 정리해드려요."
    }
}
