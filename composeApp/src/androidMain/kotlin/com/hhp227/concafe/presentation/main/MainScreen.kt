package com.hhp227.concafe.presentation.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.BuildConfig
import com.hhp227.concafe.domain.model.AppUpdateInfo
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.hhp227.concafe.presentation.security.ScreenCaptureProtectionEffect
import concafe.composeapp.generated.resources.*
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialTab: String? = null, // initialTab은 추후 ViewModel에서 처리할예정 리팩토링 TODO
    hasUnreadNotifications: Boolean = false,
    bottomNavController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GlobalContext.get().get<MainViewModel>()
            }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val currentBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }

    ScreenCaptureProtectionEffect()
    LaunchedEffect(Unit) {
        onNavigationAction(NavigationAction.RefreshUnreadNotificationCount)
        viewModel.onAction(
            MainAction.CheckAppUpdate(
                storePlatform = STORE_PLATFORM_ANDROID,
                storeId = BuildConfig.APPLICATION_ID,
                currentVersion = BuildConfig.VERSION_NAME
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
                        openPlayStore(context, update.storeUrl)
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
    // Sync ViewModel when the NavController's current route changes (e.g. system Back press
    // pops a tab — without this, selectedTab stays stale and the next tap on that tab
    // produces no StateFlow emission, so navigation never fires).
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && currentRoute != uiState.selectedTab) {
            viewModel.onAction(MainAction.SelectTab(currentRoute))
        }
    }
    LaunchedEffect(uiState.selectedTab) {
        // Guard against null currentRoute during backstack restoration to avoid
        // navigating to an unintended tab while the navcontroller isn't ready yet.
        if (currentRoute != null && currentRoute != uiState.selectedTab) {
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
            startDestination = if (initialTab.isNullOrBlank()) MainNavigationTab.HOME.route else initialTab,
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
                FanManagementScreen(onNavigationAction = onNavigationAction)
            }
            composable(MainNavigationTab.CAFE_MANAGEMENT.route) {
                CafeManagementScreen(onNavigate = onNavigationAction)
            }
            composable(MainNavigationTab.ADMIN_OPERATIONS.route) {
                AdminOperationsScreen(onNavigationAction = onNavigationAction)
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

private fun openPlayStore(context: android.content.Context, storeUrl: String) {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(browserIntent)
    }
}

private const val STORE_PLATFORM_ANDROID = "ANDROID"

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

@Composable
private fun MainNavigationTab.label(): String {
    return when (this) {
        MainNavigationTab.HOME -> stringResource(Res.string.main_tab_home)
        MainNavigationTab.EXPLORE -> stringResource(Res.string.main_tab_explore)
        MainNavigationTab.CHECK_IN -> stringResource(Res.string.main_tab_checkin)
        MainNavigationTab.FAN_MANAGEMENT -> stringResource(Res.string.main_tab_fan_management)
        MainNavigationTab.CAFE_MANAGEMENT -> stringResource(Res.string.main_tab_cafe_management)
        MainNavigationTab.ADMIN_OPERATIONS -> stringResource(Res.string.main_tab_admin_operations)
        MainNavigationTab.RANKING -> stringResource(Res.string.main_tab_ranking)
        MainNavigationTab.MY_INFO -> stringResource(Res.string.main_tab_my_info)
    }
}
