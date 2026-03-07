package org.hhp227.concafe.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
                        detailStack.add(event.route)
                    }
                }
                is NavigationEvent.NavigateBack -> {
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
                                    onNavigationAction = viewModel::onAction
                                )
                            }
                            is Route.Cafe -> {
                                CafeScreen(
                                    onNavigationAction = viewModel::onAction
                                )
                            }
                            Route.SignIn -> {
                                SignInScreen()
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
