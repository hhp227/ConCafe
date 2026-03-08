package org.hhp227.concafe.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import org.hhp227.concafe.presentation.auth.signin.SignInScreen
import org.hhp227.concafe.presentation.cafe.CafeScreen
import org.hhp227.concafe.presentation.cast.CastScreen
import org.hhp227.concafe.presentation.main.MainScreen
import org.hhp227.concafe.presentation.notification.NotificationScreen

@Composable
fun NavigationScreen(
    viewModel: NavigationViewModel = viewModel()
) {
    var currentMainTab by remember { mutableStateOf("home") }
    val detailStack = remember { mutableStateListOf<Route>() }
    val currentDetailRoute = detailStack.lastOrNull()

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateTo -> {
                    if (event.route is Route.Main) {
                        currentMainTab = event.route.initialTab ?: "home"
                        detailStack.clear()
                    } else {
                        detailStack.upsertDetailRoute(event.route)
                    }
                }
                NavigationEvent.NavigateBack -> {
                    if (detailStack.isNotEmpty()) {
                        detailStack.removeLast()
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
                if (currentDetailRoute != null) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (currentDetailRoute) {
                            is Route.Cast -> {
                                CastScreen(
                                    castId = currentDetailRoute.param,
                                    onNavigationAction = viewModel::onAction
                                )
                            }
                            is Route.Cafe -> {
                                CafeScreen(
                                    cafeId = currentDetailRoute.param,
                                    onNavigationAction = viewModel::onAction
                                )
                            }
                            Route.SignIn -> {
                                SignInScreen(onNavigate = viewModel::onAction)
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

private fun SnapshotStateList<Route>.upsertDetailRoute(route: Route) {
    val lastRoute = lastOrNull()

    when {
        lastRoute == null -> {
            add(route)
        }
        lastRoute is Route.Cafe && route is Route.Cafe -> {
            this[lastIndex] = route
        }
        lastRoute is Route.Cast && route is Route.Cast -> {
            this[lastIndex] = route
        }
        else -> {
            add(route)
        }
    }
}
