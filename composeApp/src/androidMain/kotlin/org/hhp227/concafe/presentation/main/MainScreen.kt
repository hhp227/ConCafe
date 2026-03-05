package org.hhp227.concafe.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.hhp227.concafe.presentation.main.explore.ExploreScreen
import org.hhp227.concafe.presentation.main.home.HomeScreen
import org.hhp227.concafe.presentation.navigation.NavigationAction

@Composable
fun MainScreen(
    initialTab: String? = null,
    bottomNavController: NavHostController = rememberNavController(),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val items = listOf(
        Triple("home", Icons.Default.Home, "홈"),
        Triple("explore", Icons.Default.Search, "탐색"),
    )
    val currentRoute by bottomNavController.currentBackStackEntryAsState()
    val isSelected = { route: String -> currentRoute?.destination?.route == route }

    Scaffold(
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
            composable("home") { HomeScreen(
                onNavigate = {
                    onNavigationAction()
                }
            ) }
            composable("explore") { ExploreScreen() }
        }
    }
}