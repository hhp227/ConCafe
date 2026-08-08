package com.hhp227.concafe.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import com.hhp227.concafe.presentation.theme.AppContentLayout
import com.hhp227.concafe.presentation.theme.AppBrandTheme
import com.hhp227.concafe.presentation.theme.AppThemeMode
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.settings_account_desc
import concafe.composeapp.generated.resources.settings_account_title
import concafe.composeapp.generated.resources.settings_app_info_desc
import concafe.composeapp.generated.resources.settings_app_info_title
import concafe.composeapp.generated.resources.settings_general_title
import concafe.composeapp.generated.resources.settings_inquiry_desc
import concafe.composeapp.generated.resources.settings_inquiry_title
import concafe.composeapp.generated.resources.settings_notification_desc
import concafe.composeapp.generated.resources.settings_notification_title
import concafe.composeapp.generated.resources.settings_preferences_title
import concafe.composeapp.generated.resources.settings_privacy_desc
import concafe.composeapp.generated.resources.settings_privacy_title
import concafe.composeapp.generated.resources.settings_sign_out_desc
import concafe.composeapp.generated.resources.settings_sign_out_title
import concafe.composeapp.generated.resources.settings_content_layout_desc
import concafe.composeapp.generated.resources.settings_content_layout_full_bleed
import concafe.composeapp.generated.resources.settings_content_layout_legacy
import concafe.composeapp.generated.resources.settings_content_layout_title
import concafe.composeapp.generated.resources.settings_brand_theme_desc
import concafe.composeapp.generated.resources.settings_brand_theme_maid
import concafe.composeapp.generated.resources.settings_brand_theme_mens
import concafe.composeapp.generated.resources.settings_brand_theme_title
import concafe.composeapp.generated.resources.settings_theme_dark
import concafe.composeapp.generated.resources.settings_theme_desc
import concafe.composeapp.generated.resources.settings_theme_light
import concafe.composeapp.generated.resources.settings_theme_title
import concafe.composeapp.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import com.hhp227.concafe.presentation.component.ConCafeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<SettingsViewModel>() }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                SettingsEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                SettingsEvent.NavigateToAccountSettings -> {
                    onNavigationAction(NavigationAction.NavigateToAccountSettings)
                }
                SettingsEvent.NavigateToNotificationSettings -> {
                    onNavigationAction(NavigationAction.NavigateToNotificationSettings)
                }
                SettingsEvent.NavigateToInquiryLink -> {
                    onNavigationAction(NavigationAction.NavigateToInquiry)
                }
                is SettingsEvent.NavigateToExternalLink -> {
                    onNavigationAction(
                        NavigationAction.NavigateToExternalLink(event.title, event.url)
                    )
                }
            }
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(SettingsAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        SettingsContentScreen(
            uiState = uiState,
            innerPadding = innerPadding,
            onAction = viewModel::onAction
        )
    }
}

@Composable
private fun SettingsContentScreen(
    uiState: SettingsUiState,
    innerPadding: PaddingValues,
    onAction: (SettingsAction) -> Unit
) {
    val settingsItems = settingsItems(uiState.appVersion)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = innerPadding.calculateTopPadding() + 16.dp,
            end = 16.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.errorMessage != null) {
            item {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = ConCafeColors.primary
                )
            }
        }
        item {
            SettingsSectionTitle(stringResource(Res.string.settings_preferences_title))
        }
        item {
            SettingsThemeCard(
                selectedThemeMode = uiState.themeMode,
                selectedBrandTheme = uiState.brandTheme,
                onAction = onAction
            )
        }
        item {
            SettingsContentLayoutCard(
                selectedContentLayout = uiState.contentLayout,
                onAction = onAction
            )
        }
        item {
            SettingsSectionTitle(stringResource(Res.string.settings_general_title))
        }
        items(settingsItems, key = { it.id }) { item ->
            SettingsItemCard(
                item = item,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsThemeCard(
    selectedThemeMode: AppThemeMode,
    selectedBrandTheme: AppBrandTheme,
    onAction: (SettingsAction) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = ConCafeColors.primary
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.settings_theme_title),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(Res.string.settings_theme_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionButton(
                    text = stringResource(Res.string.settings_theme_light),
                    selected = selectedThemeMode == AppThemeMode.LIGHT,
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(SettingsAction.SelectThemeMode(AppThemeMode.LIGHT)) }
                )
                ThemeOptionButton(
                    text = stringResource(Res.string.settings_theme_dark),
                    selected = selectedThemeMode == AppThemeMode.DARK,
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(SettingsAction.SelectThemeMode(AppThemeMode.DARK)) }
                )
            }
            Text(
                text = stringResource(Res.string.settings_brand_theme_title),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(Res.string.settings_brand_theme_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionButton(
                    text = stringResource(Res.string.settings_brand_theme_maid),
                    selected = selectedBrandTheme == AppBrandTheme.MAID_CAFE,
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(SettingsAction.SelectBrandTheme(AppBrandTheme.MAID_CAFE)) }
                )
                ThemeOptionButton(
                    text = stringResource(Res.string.settings_brand_theme_mens),
                    selected = selectedBrandTheme == AppBrandTheme.MENS_CON_CAFE,
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(SettingsAction.SelectBrandTheme(AppBrandTheme.MENS_CON_CAFE)) }
                )
            }
        }
    }
}

@Composable
private fun SettingsContentLayoutCard(
    selectedContentLayout: AppContentLayout,
    onAction: (SettingsAction) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ViewCarousel,
                    contentDescription = null,
                    tint = ConCafeColors.primary
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.settings_content_layout_title),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(Res.string.settings_content_layout_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionButton(
                    text = stringResource(Res.string.settings_content_layout_full_bleed),
                    selected = selectedContentLayout == AppContentLayout.FULL_BLEED,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onAction(SettingsAction.SelectContentLayout(AppContentLayout.FULL_BLEED))
                    }
                )
                ThemeOptionButton(
                    text = stringResource(Res.string.settings_content_layout_legacy),
                    selected = selectedContentLayout == AppContentLayout.LEGACY,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onAction(SettingsAction.SelectContentLayout(AppContentLayout.LEGACY))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeOptionButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItemCard(
    item: SettingsItem,
    onAction: (SettingsAction) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        enabled = item.action != null,
        onClick = {
            item.action?.let(onAction)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (item.action == SettingsAction.ClickSignOut) ConCafeColors.primary else ConCafeColors.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(item.title, fontWeight = FontWeight.SemiBold)
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                item.trailingLabel != null -> {
                    Text(
                        text = item.trailingLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item.action != null -> {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = ConCafeColors.outlineStrong
                    )
                }
            }
        }
    }
}

@Composable
private fun settingsItems(appVersion: String): List<SettingsItem> = listOf(
    SettingsItem(
        id = "account",
        title = stringResource(Res.string.settings_account_title),
        description = stringResource(Res.string.settings_account_desc),
        icon = Icons.Default.PersonOutline,
        action = SettingsAction.ClickAccountSettings
    ),
    SettingsItem(
        id = "notification",
        title = stringResource(Res.string.settings_notification_title),
        description = stringResource(Res.string.settings_notification_desc),
        icon = Icons.Default.Notifications,
        action = SettingsAction.ClickNotificationSettings
    ),
    SettingsItem(
        id = "inquiry",
        title = stringResource(Res.string.settings_inquiry_title),
        description = stringResource(Res.string.settings_inquiry_desc),
        icon = Icons.Default.QuestionAnswer,
        action = SettingsAction.ClickInquiry
    ),
    SettingsItem(
        id = "privacyPolicy",
        title = stringResource(Res.string.settings_privacy_title),
        description = stringResource(Res.string.settings_privacy_desc),
        icon = Icons.Default.Policy,
        action = SettingsAction.ClickPrivacyPolicy
    ),
    SettingsItem(
        id = "appInfo",
        title = stringResource(Res.string.settings_app_info_title),
        description = stringResource(Res.string.settings_app_info_desc),
        icon = Icons.Default.Info,
        action = null,
        trailingLabel = "v$appVersion"
    ),
    SettingsItem(
        id = "signout",
        title = stringResource(Res.string.settings_sign_out_title),
        description = stringResource(Res.string.settings_sign_out_desc),
        icon = Icons.AutoMirrored.Filled.Logout,
        action = SettingsAction.ClickSignOut
    )
)

private data class SettingsItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: SettingsAction?,
    val trailingLabel: String? = null
)
