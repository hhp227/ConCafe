package org.hhp227.concafe.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.hhp227.concafe.presentation.main.explore.ExploreScreen
import org.hhp227.concafe.presentation.main.home.HomeScreen
import org.hhp227.concafe.presentation.main.checkin.CheckInScreen
import org.hhp227.concafe.presentation.main.myinfo.MyInfoScreen
import org.hhp227.concafe.presentation.main.ranking.RankingScreen
import org.hhp227.concafe.presentation.navigation.NavigationAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialTab: String? = null,
    bottomNavController: NavHostController = rememberNavController(),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val items = listOf(
        Triple("home", Icons.Default.Home, "홈"),
        Triple("explore", Icons.Default.Search, "탐색"),
        Triple("ranking", Icons.Default.EmojiEvents, "랭킹"),
        Triple("checkin", Icons.Default.CheckCircle, "체크인"),
        Triple("myinfo", Icons.Default.Person, "내 정보"),
    )
    val currentRoute by bottomNavController.currentBackStackEntryAsState()
    val isSelected = { route: String -> currentRoute?.destination?.route == route }

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
                        onClick = { onNavigationAction(NavigationAction.NavigateToNotification) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "알림"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEach { (route, icon, label) ->
                    val selected = isSelected(route)

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            bottomNavController.navigate(route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if (selected) Color.Gray else Color.DarkGray
                            )
                        },
                        label = {
                            Text(
                                label,
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
            startDestination = initialTab ?: "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onNavigate = onNavigationAction
                )
            }
            composable("explore") {
                ExploreScreen(
                    onNavigate = onNavigationAction
                )
            }
            composable("ranking") { RankingScreen() }
            composable("checkin") { CheckInScreen() }
            composable("myinfo") { MyInfoScreen() }
        }
    }
}
