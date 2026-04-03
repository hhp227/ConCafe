package com.hhp227.concafe.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.di.resolveGetMainNavigationUseCase
import com.hhp227.concafe.di.resolveObserveCurrentUserUseCase
import com.hhp227.concafe.di.resolveRestoreSessionUseCase
import com.hhp227.concafe.domain.model.MainNavigationTab
import com.hhp227.concafe.presentation.component.ConCafeLogo
import com.hhp227.concafe.presentation.main.admin.AdminOperationsScreen
import com.hhp227.concafe.presentation.main.cafemanagement.CafeManagementScreen
import com.hhp227.concafe.presentation.main.checkin.CheckInScreen
import com.hhp227.concafe.presentation.main.explore.ExploreScreen
import com.hhp227.concafe.presentation.main.fanmanagement.FanManagementScreen
import com.hhp227.concafe.presentation.main.home.HomeScreen
import com.hhp227.concafe.presentation.main.myinfo.MyInfoScreen
import com.hhp227.concafe.presentation.main.ranking.RankingScreen
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.common_notification
import concafe.composeapp.generated.resources.common_settings
import concafe.composeapp.generated.resources.main_tab_admin_operations
import concafe.composeapp.generated.resources.main_tab_cafe_management
import concafe.composeapp.generated.resources.main_tab_checkin
import concafe.composeapp.generated.resources.main_tab_explore
import concafe.composeapp.generated.resources.main_tab_fan_management
import concafe.composeapp.generated.resources.main_tab_home
import concafe.composeapp.generated.resources.main_tab_my_info
import concafe.composeapp.generated.resources.main_tab_ranking
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialTab: String? = null,
    hasUnreadNotifications: Boolean = false,
    viewModel: MainViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                MainViewModel(
                    resolveGetMainNavigationUseCase(),
                    resolveObserveCurrentUserUseCase(),
                    resolveRestoreSessionUseCase()
                )
            }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        onNavigationAction(NavigationAction.RefreshUnreadNotificationCount)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is MainEvent.ShowError -> Unit
                MainEvent.NavigateToSignUp -> onNavigationAction(NavigationAction.NavigateToSignUp)
            }
        }
    }
    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            viewModel.onAction(MainAction.SelectTab(initialTab))
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ConCafeLogo() },
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
                        MainNavigationTab.HOME.route -> HomeScreen(onNavigate = onNavigationAction)
                        MainNavigationTab.EXPLORE.route -> ExploreScreen(onNavigate = onNavigationAction)
                        MainNavigationTab.CHECK_IN.route -> CheckInScreen(onNavigate = onNavigationAction)
                        MainNavigationTab.FAN_MANAGEMENT.route -> FanManagementScreen(onNavigationAction = onNavigationAction)
                        MainNavigationTab.CAFE_MANAGEMENT.route -> CafeManagementScreen(onNavigate = onNavigationAction)
                        MainNavigationTab.ADMIN_OPERATIONS.route -> AdminOperationsScreen(onNavigationAction = onNavigationAction)
                        MainNavigationTab.RANKING.route -> RankingScreen(onNavigate = onNavigationAction)
                        MainNavigationTab.MY_INFO.route -> MyInfoScreen(onNavigate = onNavigationAction)
                        else -> HomeScreen(onNavigate = onNavigationAction)
                    }
                }
            }
        }
    }
}

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
    MainNavigationTab.MY_INFO -> stringResource(Res.string.main_tab_my_info)
}
