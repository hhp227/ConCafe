package org.hhp227.concafe.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import org.hhp227.concafe.presentation.cafedetail.CafeDetailScreen
import org.hhp227.concafe.presentation.castdetail.CastDetailScreen
import org.hhp227.concafe.presentation.main.MainScreen
import org.hhp227.concafe.presentation.notification.NotificationScreen

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
                selected = currentMainTab == "home",
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
            NavigationRailItem(
                selected = currentMainTab == "ranking",
                onClick = {
                    viewModel.onAction(NavigationAction.NavigateToMain("ranking"))
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "랭킹"
                    )
                },
                label = { Text("랭킹") }
            )
            NavigationRailItem(
                selected = currentMainTab == "checkin",
                onClick = {
                    viewModel.onAction(NavigationAction.NavigateToMain("checkin"))
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "체크인"
                    )
                },
                label = { Text("체크인") }
            )
            NavigationRailItem(
                selected = currentMainTab == "myinfo",
                onClick = {
                    viewModel.onAction(NavigationAction.NavigateToMain("myinfo"))
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "내 정보"
                    )
                },
                label = { Text("내 정보") }
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
                            when (currentRoute) {
                                is Route.CastDetail -> {
                                    CastDetailScreen(
                                        onNavigationAction = viewModel::onAction
                                    )
                                }
                                is Route.CafeDetail -> {
                                    CafeDetailScreen(
                                        onNavigationAction = viewModel::onAction
                                    )
                                }
                                Route.Notification -> {
                                    NotificationScreen(
                                        onNavigationAction = viewModel::onAction
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}
