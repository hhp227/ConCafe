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
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.notification_quiet_all_day_desc
import concafe.composeapp.generated.resources.notification_quiet_all_day_title
import concafe.composeapp.generated.resources.notification_quiet_night_desc
import concafe.composeapp.generated.resources.notification_quiet_night_title
import concafe.composeapp.generated.resources.notification_quiet_off_desc
import concafe.composeapp.generated.resources.notification_quiet_off_title
import concafe.composeapp.generated.resources.notification_settings_basic_title
import concafe.composeapp.generated.resources.notification_settings_birthday_desc
import concafe.composeapp.generated.resources.notification_settings_birthday_title
import concafe.composeapp.generated.resources.notification_settings_community_desc
import concafe.composeapp.generated.resources.notification_settings_community_title
import concafe.composeapp.generated.resources.notification_settings_event_desc
import concafe.composeapp.generated.resources.notification_settings_event_title
import concafe.composeapp.generated.resources.notification_settings_follow_desc
import concafe.composeapp.generated.resources.notification_settings_follow_title
import concafe.composeapp.generated.resources.notification_settings_hero_summary
import concafe.composeapp.generated.resources.notification_settings_hero_title
import concafe.composeapp.generated.resources.notification_settings_notice_desc
import concafe.composeapp.generated.resources.notification_settings_notice_title
import concafe.composeapp.generated.resources.notification_settings_push_desc
import concafe.composeapp.generated.resources.notification_settings_push_title
import concafe.composeapp.generated.resources.notification_settings_quiet_desc
import concafe.composeapp.generated.resources.notification_settings_quiet_title
import concafe.composeapp.generated.resources.notification_settings_shift_desc
import concafe.composeapp.generated.resources.notification_settings_shift_title
import concafe.composeapp.generated.resources.notification_settings_title
import concafe.composeapp.generated.resources.notification_settings_type_title
import org.jetbrains.compose.resources.stringResource
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
                title = { Text(stringResource(Res.string.notification_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(NotificationSettingsAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
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
            .background(colorFromHex("FFFBFD")),
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
            NotificationSettingCard(title = stringResource(Res.string.notification_settings_basic_title)) {
                NotificationToggleRow(
                    icon = Icons.Default.Notifications,
                    iconBackground = colorFromHex("FFE6F1"),
                    iconTint = colorFromHex("EB5F97"),
                    title = stringResource(Res.string.notification_settings_push_title),
                    description = stringResource(Res.string.notification_settings_push_desc),
                    checked = uiState.isPushNotificationsEnabled,
                    onCheckedChange = { onAction(NotificationSettingsAction.TogglePushNotifications(it)) },
                    enabled = !uiState.isSaving
                )
            }
        }
        item {
            NotificationSettingCard(title = stringResource(Res.string.notification_settings_type_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NotificationToggleRow(
                        icon = Icons.Default.WorkHistory,
                        iconBackground = colorFromHex("E4F7EC"),
                        iconTint = colorFromHex("2E9E5B"),
                        title = stringResource(Res.string.notification_settings_shift_title),
                        description = stringResource(Res.string.notification_settings_shift_desc),
                        checked = uiState.isShiftNotificationsEnabled,
                        onCheckedChange = { onAction(NotificationSettingsAction.ToggleShiftNotifications(it)) },
                        enabled = !uiState.isSaving
                    )
                    NotificationToggleRow(
                        icon = Icons.Default.Cake,
                        iconBackground = colorFromHex("FFE6F1"),
                        iconTint = colorFromHex("EB5F97"),
                        title = stringResource(Res.string.notification_settings_birthday_title),
                        description = stringResource(Res.string.notification_settings_birthday_desc),
                        checked = uiState.isBirthdayNotificationsEnabled,
                        onCheckedChange = { onAction(NotificationSettingsAction.ToggleBirthdayNotifications(it)) },
                        enabled = !uiState.isSaving
                    )
                    NotificationToggleRow(
                        icon = Icons.Default.Campaign,
                        iconBackground = colorFromHex("E8F0FF"),
                        iconTint = colorFromHex("4A79E8"),
                        title = stringResource(Res.string.notification_settings_notice_title),
                        description = stringResource(Res.string.notification_settings_notice_desc),
                        checked = uiState.isNoticeNotificationsEnabled,
                        onCheckedChange = { onAction(NotificationSettingsAction.ToggleNoticeNotifications(it)) },
                        enabled = !uiState.isSaving
                    )
                    if (uiState.isCastRole) {
                        NotificationToggleRow(
                            icon = Icons.Default.PersonAddAlt1,
                            iconBackground = colorFromHex("F1E8FF"),
                            iconTint = colorFromHex("8A52E2"),
                            title = stringResource(Res.string.notification_settings_follow_title),
                            description = stringResource(Res.string.notification_settings_follow_desc),
                            checked = uiState.isFollowNotificationsEnabled,
                            onCheckedChange = { onAction(NotificationSettingsAction.ToggleFollowNotifications(it)) },
                            enabled = !uiState.isSaving
                        )
                    }
                    NotificationToggleRow(
                        icon = Icons.Default.Celebration,
                        iconBackground = colorFromHex("FFF4E2"),
                        iconTint = colorFromHex("E29B35"),
                        title = stringResource(Res.string.notification_settings_event_title),
                        description = stringResource(Res.string.notification_settings_event_desc),
                        checked = uiState.isEventNotificationsEnabled,
                        onCheckedChange = { onAction(NotificationSettingsAction.ToggleEventNotifications(it)) },
                        enabled = !uiState.isSaving
                    )
                    NotificationToggleRow(
                        icon = Icons.Default.Forum,
                        iconBackground = colorFromHex("E9F7F8"),
                        iconTint = colorFromHex("228B96"),
                        title = stringResource(Res.string.notification_settings_community_title),
                        description = stringResource(Res.string.notification_settings_community_desc),
                        checked = uiState.isCommunityNotificationsEnabled,
                        onCheckedChange = { onAction(NotificationSettingsAction.ToggleCommunityNotifications(it)) },
                        enabled = !uiState.isSaving
                    )
                }
            }
        }
        item {
            NotificationSettingCard(title = stringResource(Res.string.notification_settings_quiet_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(Res.string.notification_settings_quiet_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = colorFromHex("C33E6A")
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
                        listOf(colorFromHex("EF6797"), colorFromHex("F7A0C1"))
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.notification_settings_hero_title),
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colorFromHex("EF6797")
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
                color = colorFromHex("FFD6E5"),
                shape = RoundedCornerShape(18.dp)
            )
            .background(colorFromHex("FFF6FA"), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = option.titleText(),
                fontWeight = FontWeight.Bold,
                color = colorFromHex("B84473")
            )
            Text(
                text = option.descriptionText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun heroSummary(uiState: NotificationSettingsUiState): String {
    val toggles = mutableListOf(
        uiState.isPushNotificationsEnabled,
        uiState.isShiftNotificationsEnabled,
        uiState.isBirthdayNotificationsEnabled,
        uiState.isNoticeNotificationsEnabled,
        uiState.isEventNotificationsEnabled,
        uiState.isCommunityNotificationsEnabled
    )

    if (uiState.isCastRole) {
        toggles.add(uiState.isFollowNotificationsEnabled)
    }
    val enabledCount = toggles.count { it }
    return stringResource(
        Res.string.notification_settings_hero_summary,
        enabledCount,
        uiState.quietHoursOption.titleText()
    )
}

@Composable
private fun NotificationQuietHoursMode.titleText(): String {
    return when (this) {
        NotificationQuietHoursMode.OFF -> stringResource(Res.string.notification_quiet_off_title)
        NotificationQuietHoursMode.NIGHT -> stringResource(Res.string.notification_quiet_night_title)
        NotificationQuietHoursMode.ALL_DAY -> stringResource(Res.string.notification_quiet_all_day_title)
    }
}

@Composable
private fun NotificationQuietHoursMode.descriptionText(): String {
    return when (this) {
        NotificationQuietHoursMode.OFF -> stringResource(Res.string.notification_quiet_off_desc)
        NotificationQuietHoursMode.NIGHT -> stringResource(Res.string.notification_quiet_night_desc)
        NotificationQuietHoursMode.ALL_DAY -> stringResource(Res.string.notification_quiet_all_day_desc)
    }
}
