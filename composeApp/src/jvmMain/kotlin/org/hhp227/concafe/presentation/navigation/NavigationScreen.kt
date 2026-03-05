package org.hhp227.concafe.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import org.hhp227.concafe.presentation.detail.DetailScreen
import org.hhp227.concafe.presentation.main.MainScreen

@Composable
fun NavigationScreen(
    viewModel: NavigationViewModel = viewModel()
) {
    var currentRoute by remember { mutableStateOf<Route>(Route.Entry) }
    val backStack = remember { mutableStateListOf(currentRoute) }
    val currentMainTab =
        (currentRoute as? Route.Main)?.initialTab
            ?: backStack.asReversed().filterIsInstance<Route.Main>().firstOrNull()?.initialTab
            ?: "home"

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateTo -> {
                    if (event.route is Route.Main) {
                        backStack.clear()
                    }
                    backStack.add(event.route)
                    currentRoute = event.route
                }
                is NavigationEvent.NavigateBack -> {
                    if (backStack.size > 1) {
                        backStack.removeLast()
                        currentRoute = backStack.last()
                    }
                }
            }
        }
    }
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .fillMaxSize()
    ) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight()
        ) {
            NavigationRailItem(
                selected = currentMainTab != "explore",
                onClick = {
                    viewModel.onAction(NavigationAction.NavigateToMain("home"))
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "홈"
                    )
                },
                label = { Text("홈") }
            )
            NavigationRailItem(
                selected = currentMainTab == "explore",
                onClick = {
                    viewModel.onAction(NavigationAction.NavigateToMain("explore"))
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "탐색"
                    )
                },
                label = { Text("탐색") }
            )
        }
        if (currentRoute is Route.Entry) {
            LaunchedEffect(Unit) {
                val target: Route = Route.Main()

                currentRoute = target
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Row {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        MainScreen(
                            initialTab = currentMainTab,
                            onNavigationAction = viewModel::onAction
                        )
                    }
                    if (currentRoute !is Route.Main) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            DetailScreen(
                                onNavigationAction = viewModel::onAction
                            )
                        }
                    }
                }
            }
        }
    }
}
