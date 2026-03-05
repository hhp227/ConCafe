package org.hhp227.concafe.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import org.hhp227.concafe.presentation.detail.DetailScreen
import org.hhp227.concafe.presentation.main.MainScreen

private const val TAB_HOME = "home"
private const val TAB_EXPLORE = "explore"

@Composable
fun NavigationScreen(
    viewModel: NavigationViewModel = viewModel()
) {
    var currentRoute by remember { mutableStateOf<Route>(Route.Entry) }
    val backStack = remember { mutableStateListOf(currentRoute) }

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
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp)
        ) {
            NavigationRailItem(
                selected = (currentRoute as? Route.Main)?.initialTab != TAB_EXPLORE,
                onClick = {
                    viewModel.onAction(NavigationAction.NavigateToMain(TAB_HOME))
                },
                icon = { Text("⌂") },
                label = { Text("홈") }
            )
            NavigationRailItem(
                selected = (currentRoute as? Route.Main)?.initialTab == TAB_EXPLORE,
                onClick = {
                    viewModel.onAction(NavigationAction.NavigateToMain(TAB_EXPLORE))
                },
                icon = { Text("⌕") },
                label = { Text("탐색") }
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val route = currentRoute) {
                is Route.Main -> {
                    MainScreen(
                        initialTab = route.initialTab,
                        onNavigationAction = viewModel::onAction
                    )
                }
                is Route.Detail -> {
                    DetailScreen()
                }
                Route.Entry -> {
                    LaunchedEffect(Unit) {
                        viewModel.onAction(NavigationAction.NavigateToMain())
                    }
                }
            }
        }
    }
}
