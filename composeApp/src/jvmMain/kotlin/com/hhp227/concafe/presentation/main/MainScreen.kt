package com.hhp227.concafe.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.MainNavigationTab
import com.hhp227.concafe.presentation.component.ConCafeLogo
import com.hhp227.concafe.presentation.main.admin.AdminOperationsScreen
import com.hhp227.concafe.presentation.main.cafemanagement.CafeManagementScreen
import com.hhp227.concafe.presentation.main.checkin.CheckInScreen
import com.hhp227.concafe.presentation.main.community.CommunityScreen
import com.hhp227.concafe.presentation.main.explore.ExploreScreen
import com.hhp227.concafe.presentation.main.fanmanagement.FanManagementScreen
import com.hhp227.concafe.presentation.main.home.HomeScreen
import com.hhp227.concafe.presentation.main.myinfo.MyInfoScreen
import com.hhp227.concafe.presentation.main.ranking.RankingScreen
import com.hhp227.concafe.presentation.navigation.NavigationAction
import com.hhp227.concafe.domain.model.AppUpdateInfo
import com.hhp227.concafe.presentation.settings.currentAppVersion
import concafe.composeapp.generated.resources.*
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import java.awt.Desktop
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialTab: String? = null,
    hasUnreadNotifications: Boolean = false,
    viewModel: MainViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GlobalContext.get().get<MainViewModel>()
            }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        onNavigationAction(NavigationAction.RefreshUnreadNotificationCount)
        viewModel.onAction(
            MainAction.CheckAppUpdate(
                storePlatform = STORE_PLATFORM_IOS,
                storeId = APP_STORE_BUNDLE_ID,
                currentVersion = currentAppVersion()
            )
        )
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is MainEvent.ShowError -> Unit
                is MainEvent.ShowAppUpdate -> availableUpdate = event.updateInfo
                MainEvent.NavigateToSignUp -> onNavigationAction(NavigationAction.NavigateToSignUp)
            }
        }
    }
    availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { availableUpdate = null },
            title = { Text("업데이트 안내") },
            text = { Text("새 버전 ${update.latestVersion}이 출시되었습니다. 스토어에서 업데이트할 수 있습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        availableUpdate = null
                        openStorePage(update.storeUrl)
                    }
                ) {
                    Text("업데이트")
                }
            },
            dismissButton = {
                TextButton(onClick = { availableUpdate = null }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            viewModel.onAction(MainAction.SelectTab(initialTab))
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.selectedTab == MainNavigationTab.COMMUNITY.route) {
                        Text(
                            text = stringResource(Res.string.community_title),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        ConCafeLogo()
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (uiState.selectedTab == MainNavigationTab.MY_INFO.route && uiState.currentUser != null) {
                                onNavigationAction(NavigationAction.NavigateToSettings)
                            } else {
                                onNavigationAction(NavigationAction.NavigateToNotification)
                            }
                        }
                    ) {
                        if (uiState.selectedTab == MainNavigationTab.MY_INFO.route && uiState.currentUser != null) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(Res.string.common_settings)
                            )
                        } else {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = stringResource(Res.string.common_notification)
                                )
                                if (hasUnreadNotifications) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .background(Color.Red, shape = CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight()
            ) {
                desktopMainTabs(uiState).forEach { (tab, icon) ->
                    NavigationRailItem(
                        selected = uiState.selectedTab == tab.route,
                        onClick = {
                            onNavigationAction(NavigationAction.NavigateToMain(tab.route))
                            viewModel.onAction(MainAction.SelectTab(tab.route))
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label()
                            )
                        },
                        label = { Text(tab.label()) }
                    )
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                key(uiState.currentUser?.id, uiState.selectedTab) {
                    when (uiState.selectedTab) {
                        MainNavigationTab.HOME.route -> HomeScreen(
                            onNavigate = { action ->
                                if (action == NavigationAction.NavigateToCommunity) {
                                    viewModel.onAction(MainAction.SelectTab(MainNavigationTab.COMMUNITY.route))
                                } else {
                                    onNavigationAction(action)
                                }
                            }
                        )
                        MainNavigationTab.EXPLORE.route -> ExploreScreen(onNavigate = onNavigationAction)
                        MainNavigationTab.CHECK_IN.route -> CheckInScreen(onNavigate = onNavigationAction)
                        MainNavigationTab.FAN_MANAGEMENT.route -> FanManagementScreen(onNavigationAction = onNavigationAction)
                        MainNavigationTab.CAFE_MANAGEMENT.route -> CafeManagementScreen(onNavigate = onNavigationAction)
                        MainNavigationTab.ADMIN_OPERATIONS.route -> AdminOperationsScreen(onNavigationAction = onNavigationAction)
                        MainNavigationTab.RANKING.route -> RankingScreen(onNavigate = onNavigationAction)
                        MainNavigationTab.COMMUNITY.route -> CommunityScreen(
                            onNavigationAction = onNavigationAction,
                            showTopBar = false
                        )
                        MainNavigationTab.MY_INFO.route -> MyInfoScreen(onNavigate = onNavigationAction)
                        else -> HomeScreen(onNavigate = onNavigationAction)
                    }
                }
            }
        }
    }
}

private fun openStorePage(storeUrl: String) {
    runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(storeUrl))
        }
    }
}

private const val APP_STORE_BUNDLE_ID = "com.hhp227.ConCafe"
private const val STORE_PLATFORM_IOS = "IOS"

fun desktopMainTabs(uiState: MainUiState): List<Pair<MainNavigationTab, ImageVector>> {
    return uiState.tabs.map { tab ->
        tab to when (tab) {
            MainNavigationTab.HOME -> Icons.Default.Home
            MainNavigationTab.EXPLORE -> Icons.Default.Search
            MainNavigationTab.CHECK_IN -> Icons.Default.CheckCircle
            MainNavigationTab.FAN_MANAGEMENT -> Icons.Default.Groups
            MainNavigationTab.CAFE_MANAGEMENT -> Icons.Default.ManageAccounts
            MainNavigationTab.ADMIN_OPERATIONS -> Icons.Default.AdminPanelSettings
            MainNavigationTab.RANKING -> Icons.Default.EmojiEvents
            MainNavigationTab.COMMUNITY -> Icons.Default.Article
            MainNavigationTab.MY_INFO -> Icons.Default.Person
        }
    }
}

@Composable
private fun MainNavigationTab.label(): String = when (this) {
    MainNavigationTab.HOME -> stringResource(Res.string.main_tab_home)
    MainNavigationTab.EXPLORE -> stringResource(Res.string.main_tab_explore)
    MainNavigationTab.CHECK_IN -> stringResource(Res.string.main_tab_checkin)
    MainNavigationTab.FAN_MANAGEMENT -> stringResource(Res.string.main_tab_fan_management)
    MainNavigationTab.CAFE_MANAGEMENT -> stringResource(Res.string.main_tab_cafe_management)
    MainNavigationTab.ADMIN_OPERATIONS -> stringResource(Res.string.main_tab_admin_operations)
    MainNavigationTab.RANKING -> stringResource(Res.string.main_tab_ranking)
    MainNavigationTab.COMMUNITY -> stringResource(Res.string.community_title)
    MainNavigationTab.MY_INFO -> stringResource(Res.string.main_tab_my_info)
}
