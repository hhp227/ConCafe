package com.hhp227.concafe.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.di.resolveGetMainNavigationUseCase
import com.hhp227.concafe.di.resolveObserveCurrentUserUseCase
import com.hhp227.concafe.domain.model.MainNavigationTab
import com.hhp227.concafe.presentation.main.admin.AdminOperationsScreen
import com.hhp227.concafe.presentation.main.cafemanagement.CafeManagementScreen
import com.hhp227.concafe.presentation.main.checkin.CheckInScreen
import com.hhp227.concafe.presentation.main.explore.ExploreScreen
import com.hhp227.concafe.presentation.main.fanmanagement.FanManagementScreen
import com.hhp227.concafe.presentation.main.home.HomeScreen
import com.hhp227.concafe.presentation.main.myinfo.MyInfoScreen
import com.hhp227.concafe.presentation.main.ranking.RankingScreen
import com.hhp227.concafe.presentation.navigation.NavigationAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialTab: String? = null,
    viewModel: MainViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                MainViewModel(
                    resolveGetMainNavigationUseCase(),
                    resolveObserveCurrentUserUseCase()
                )
            }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            viewModel.onAction(MainAction.SelectTab(initialTab))
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ConCafe") },
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
                        Icon(
                            imageVector = if (uiState.selectedTab == MainNavigationTab.MY_INFO.route && uiState.currentUser != null) Icons.Default.Settings else Icons.Default.Notifications,
                            contentDescription = if (uiState.selectedTab == MainNavigationTab.MY_INFO.route && uiState.currentUser != null) "설정" else "알림"
                        )
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
                        MainNavigationTab.FAN_MANAGEMENT.route -> FanManagementScreen()
                        MainNavigationTab.CAFE_MANAGEMENT.route -> CafeManagementScreen(onNavigate = onNavigationAction)
                        MainNavigationTab.ADMIN_OPERATIONS.route -> AdminOperationsScreen()
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

private fun MainNavigationTab.label(): String = when (this) {
    MainNavigationTab.HOME -> "홈"
    MainNavigationTab.EXPLORE -> "탐색"
    MainNavigationTab.CHECK_IN -> "체크인"
    MainNavigationTab.FAN_MANAGEMENT -> "팬관리"
    MainNavigationTab.CAFE_MANAGEMENT -> "카페관리"
    MainNavigationTab.ADMIN_OPERATIONS -> "운영관리"
    MainNavigationTab.RANKING -> "랭킹"
    MainNavigationTab.MY_INFO -> "내 정보"
}
