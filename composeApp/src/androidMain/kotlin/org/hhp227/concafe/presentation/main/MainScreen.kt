package org.hhp227.concafe.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.hhp227.concafe.di.resolveGetMainNavigationUseCase
import org.hhp227.concafe.di.resolveObserveCurrentUserUseCase
import org.hhp227.concafe.domain.model.MainNavigationTab
import org.hhp227.concafe.presentation.main.admin.AdminOperationsScreen
import org.hhp227.concafe.presentation.main.cafemanagement.CafeManagementScreen
import org.hhp227.concafe.presentation.main.checkin.CheckInScreen
import org.hhp227.concafe.presentation.main.explore.ExploreScreen
import org.hhp227.concafe.presentation.main.home.HomeScreen
import org.hhp227.concafe.presentation.main.fanmanagement.FanManagementScreen
import org.hhp227.concafe.presentation.main.myinfo.MyInfoScreen
import org.hhp227.concafe.presentation.main.ranking.RankingScreen
import org.hhp227.concafe.presentation.navigation.NavigationAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialTab: String? = null,
    bottomNavController: NavHostController = rememberNavController(),
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
    val currentBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.selectedTab, currentRoute) {
        if (
            uiState.selectedTab.isNotBlank() &&
            uiState.tabs.any { it.route == uiState.selectedTab } &&
            MainNavigationTab.fromRoute(currentRoute) != null &&
            currentRoute != uiState.selectedTab
        ) {
            bottomNavController.navigate(uiState.selectedTab) {
                popUpTo(bottomNavController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ConCafe",
                        textAlign = TextAlign.Center
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (currentRoute == MainNavigationTab.MY_INFO.route && uiState.currentUser != null) {
                                onNavigationAction(NavigationAction.NavigateToSettings)
                            } else {
                                onNavigationAction(NavigationAction.NavigateToNotification)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (currentRoute == MainNavigationTab.MY_INFO.route && uiState.currentUser != null) Icons.Default.Settings else Icons.Default.Notifications,
                            contentDescription = if (currentRoute == MainNavigationTab.MY_INFO.route && uiState.currentUser != null) "설정" else "알림"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                uiState.tabs.forEach { tab ->
                    val selected = currentRoute == tab.route

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            viewModel.onAction(MainAction.SelectTab(tab.route))
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon(),
                                contentDescription = tab.label(),
                                tint = if (selected) Color.Gray else Color.DarkGray
                            )
                        },
                        label = {
                            Text(
                                text = tab.label(),
                                color = if (selected) Color.Gray else Color.DarkGray,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = uiState.selectedTab,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainNavigationTab.HOME.route) {
                HomeScreen(onNavigate = onNavigationAction)
            }
            composable(MainNavigationTab.EXPLORE.route) {
                ExploreScreen(onNavigate = onNavigationAction)
            }
            composable(MainNavigationTab.CHECK_IN.route) {
                CheckInScreen(onNavigate = onNavigationAction)
            }
            composable(MainNavigationTab.FAN_MANAGEMENT.route) {
                FanManagementScreen()
            }
            composable(MainNavigationTab.CAFE_MANAGEMENT.route) {
                CafeManagementScreen(onNavigate = onNavigationAction)
            }
            composable(MainNavigationTab.ADMIN_OPERATIONS.route) {
                AdminOperationsScreen()
            }
            composable(MainNavigationTab.RANKING.route) {
                RankingScreen(onNavigate = onNavigationAction)
            }
            composable(MainNavigationTab.MY_INFO.route) {
                MyInfoScreen(onNavigate = onNavigationAction)
            }
        }
    }
}

private fun MainNavigationTab.icon(): ImageVector {
    return when (this) {
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

private fun MainNavigationTab.label(): String {
    return when (this) {
        MainNavigationTab.HOME -> "홈"
        MainNavigationTab.EXPLORE -> "탐색"
        MainNavigationTab.CHECK_IN -> "체크인"
        MainNavigationTab.FAN_MANAGEMENT -> "팬관리"
        MainNavigationTab.CAFE_MANAGEMENT -> "카페관리"
        MainNavigationTab.ADMIN_OPERATIONS -> "운영관리"
        MainNavigationTab.RANKING -> "랭킹"
        MainNavigationTab.MY_INFO -> "내 정보"
    }
}
