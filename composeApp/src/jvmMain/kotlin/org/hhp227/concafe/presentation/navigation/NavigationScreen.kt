package org.hhp227.concafe.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import org.hhp227.concafe.presentation.auth.signin.SignInScreen
import org.hhp227.concafe.presentation.cafe.CafeScreen
import org.hhp227.concafe.presentation.cast.CastScreen
import org.hhp227.concafe.presentation.main.MainScreen
import org.hhp227.concafe.presentation.notification.NotificationScreen

private const val DESKTOP_TWO_PANE_MIN_WIDTH_DP = 640

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
    BoxWithConstraints(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .fillMaxSize()
    ) {
        val isTwoPaneMode = maxWidth.value >= DESKTOP_TWO_PANE_MIN_WIDTH_DP

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = if (isTwoPaneMode) {
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    MainScreen(
                        initialTab = currentMainTab,
                        onNavigationAction = viewModel::onAction
                    )
                }
                if (isTwoPaneMode && currentDetailRoute != null) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        DetailRoutePane(
                            route = currentDetailRoute,
                            onNavigationAction = viewModel::onAction
                        )
                    }
                }
            }
            if (!isTwoPaneMode && currentDetailRoute != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .zIndex(1f)
                ) {
                    DetailRoutePane(
                        route = currentDetailRoute,
                        onNavigationAction = viewModel::onAction
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRoutePane(
    route: Route,
    onNavigationAction: (NavigationAction) -> Unit
) {
    when (route) {
        is Route.Cast -> {
            CastScreen(
                castId = route.param,
                onNavigationAction = onNavigationAction
            )
        }
        is Route.Cafe -> {
            CafeScreen(
                cafeId = route.param,
                onNavigationAction = onNavigationAction
            )
        }
        Route.SignIn -> {
            SignInScreen(onNavigate = onNavigationAction)
        }
        Route.Notification -> {
            NotificationScreen(
                onNavigationAction = onNavigationAction
            )
        }
        else -> {
            Unit
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
