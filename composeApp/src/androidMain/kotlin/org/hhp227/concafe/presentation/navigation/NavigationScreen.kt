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
import org.hhp227.concafe.presentation.detail.DetailScreen
import org.hhp227.concafe.presentation.main.MainScreen

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
        composable<Route.Detail> { backStackEntry ->
            val detail: Route.Detail = backStackEntry.toRoute()

            DetailScreen(onNavigationAction = viewModel::onAction)
        }
    }
}