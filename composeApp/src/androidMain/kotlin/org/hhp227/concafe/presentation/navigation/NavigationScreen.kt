package org.hhp227.concafe.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.collectLatest
import org.hhp227.concafe.presentation.cafe.CafeDetailScreen
import org.hhp227.concafe.presentation.cast.CastDetailScreen
import org.hhp227.concafe.presentation.main.MainScreen
import org.hhp227.concafe.presentation.notification.NotificationScreen

@Composable
fun NavigationScreen(
    navController: NavHostController = rememberNavController(),
    viewModel: NavigationViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateTo -> {
                    navController.navigate(event.route) {
                        if (event.route is Route.Main) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                is NavigationEvent.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }
    NavHost(
        navController = navController,
        startDestination = Route.Entry,
    ) {
        composable<Route.Entry> {
            LaunchedEffect(Unit) {
                val target: Route = Route.Main()

                navController.navigate(target) {
                    popUpTo(Route.Entry) { inclusive = true }
                }
            }
        }
        composable<Route.Main> { backStackEntry ->
            val mainRoute: Route.Main = backStackEntry.toRoute()

            MainScreen(
                initialTab = mainRoute.initialTab,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.CastDetail> { backStackEntry ->
            backStackEntry.toRoute<Route.CastDetail>()
            CastDetailScreen(onNavigationAction = viewModel::onAction)
        }
        composable<Route.CafeDetail> { backStackEntry ->
            backStackEntry.toRoute<Route.CafeDetail>()
            CafeDetailScreen(onNavigationAction = viewModel::onAction)
        }
        composable<Route.Notification> {
            NotificationScreen(
                onNavigationAction = viewModel::onAction
            )
        }
    }
}
