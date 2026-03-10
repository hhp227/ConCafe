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
import org.hhp227.concafe.presentation.auth.signin.SignInScreen
import org.hhp227.concafe.presentation.auth.signup.SignUpScreen
import org.hhp227.concafe.presentation.cafe.CafeScreen
import org.hhp227.concafe.presentation.cast.CastScreen
import org.hhp227.concafe.presentation.main.MainScreen
import org.hhp227.concafe.presentation.notification.NotificationScreen
import org.hhp227.concafe.presentation.settings.SettingsScreen

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
        composable<Route.Cast> { backStackEntry ->
            val castRoute = backStackEntry.toRoute<Route.Cast>()
            CastScreen(
                castId = castRoute.param,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.Cafe> { backStackEntry ->
            val cafeRoute = backStackEntry.toRoute<Route.Cafe>()

            CafeScreen(
                cafeId = cafeRoute.param,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.SignIn> { backStackEntry ->
            backStackEntry.toRoute<Route.SignIn>()
            SignInScreen(onNavigate = viewModel::onAction)
        }
        composable<Route.SignUp> { backStackEntry ->
            backStackEntry.toRoute<Route.SignUp>()
            SignUpScreen(onNavigate = viewModel::onAction)
        }
        composable<Route.Notification> {
            NotificationScreen(
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.Settings> {
            SettingsScreen(
                onNavigationAction = viewModel::onAction
            )
        }
    }
}
