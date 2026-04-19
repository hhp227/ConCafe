package com.hhp227.concafe.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hhp227.concafe.domain.model.MainNavigationTab
import com.hhp227.concafe.presentation.auth.resetpassword.ResetPasswordScreen
import com.hhp227.concafe.presentation.auth.signin.SignInScreen
import com.hhp227.concafe.presentation.auth.signup.SignUpScreen
import com.hhp227.concafe.presentation.cafe.CafeScreen
import com.hhp227.concafe.presentation.cast.CastScreen
import com.hhp227.concafe.presentation.castedit.CastEditScreen
import com.hhp227.concafe.presentation.main.MainScreen
import com.hhp227.concafe.presentation.main.cafemanagement.banner.BannerScreen
import com.hhp227.concafe.presentation.main.cafemanagement.banneredit.BannerEditScreen
import com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard.CafeDashboardScreen
import com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo.CafeInfoEditScreen
import com.hhp227.concafe.presentation.main.cafemanagement.externallink.ExternalLinkScreen
import com.hhp227.concafe.presentation.main.cafemanagement.menugoods.MenuGoodsScreen
import com.hhp227.concafe.presentation.main.cafemanagement.menugoodsedit.MenuGoodsEditScreen
import com.hhp227.concafe.presentation.main.cafemanagement.noticeevent.NoticeEventScreen
import com.hhp227.concafe.presentation.main.cafemanagement.castmanagement.CastManagementScreen
import com.hhp227.concafe.presentation.main.cafemanagement.schedule.ScheduleScreen
import com.hhp227.concafe.presentation.notification.NotificationScreen
import com.hhp227.concafe.presentation.picture.PictureAction
import com.hhp227.concafe.presentation.picture.PictureScreen
import com.hhp227.concafe.presentation.review.ReviewEditScreen
import com.hhp227.concafe.presentation.settings.SettingsScreen
import com.hhp227.concafe.presentation.settings.account.AccountSettingsScreen
import com.hhp227.concafe.presentation.settings.changepassword.ChangePasswordScreen
import com.hhp227.concafe.presentation.settings.inquiry.InquiryLinkScreen
import com.hhp227.concafe.presentation.settings.notification.NotificationSettingsScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NavigationScreen(
    navController: NavHostController = rememberNavController(),
    viewModel: NavigationViewModel = viewModel(),
    hasUnreadNotifications: Boolean = false,
    onRefreshUnreadNotificationCount: () -> Unit = {}
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
                NavigationEvent.RefreshUnreadNotificationCount -> {
                    onRefreshUnreadNotificationCount()
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
                val target: Route = Route.Main(initialTab = MainNavigationTab.HOME.route)

                navController.navigate(target) {
                    popUpTo(Route.Entry) { inclusive = true }
                }
            }
        }
        composable<Route.Main> { backStackEntry ->
            val mainRoute: Route.Main = backStackEntry.toRoute()

            MainScreen(
                initialTab = mainRoute.initialTab,
                hasUnreadNotifications = hasUnreadNotifications,
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
        composable<Route.CafeDashboard> { backStackEntry ->
            val cafeDashboardRoute = backStackEntry.toRoute<Route.CafeDashboard>()

            CafeDashboardScreen(
                cafeId = cafeDashboardRoute.param,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.Banner> { backStackEntry ->
            val bannerRoute = backStackEntry.toRoute<Route.Banner>()

            BannerScreen(
                cafeId = bannerRoute.cafeId,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.BannerEdit> { backStackEntry ->
            val bannerEditRoute = backStackEntry.toRoute<Route.BannerEdit>()

            BannerEditScreen(
                initialCafeId = bannerEditRoute.cafeId,
                initialBannerId = bannerEditRoute.bannerId,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.ExternalLink> { backStackEntry ->
            val externalLinkRoute = backStackEntry.toRoute<Route.ExternalLink>()

            ExternalLinkScreen(
                title = externalLinkRoute.title,
                url = externalLinkRoute.url,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.CafeInfoEdit> { backStackEntry ->
            val cafeInfoEditRoute = backStackEntry.toRoute<Route.CafeInfoEdit>()

            CafeInfoEditScreen(
                cafeId = cafeInfoEditRoute.param,
                isRegistrationMode = cafeInfoEditRoute.isRegistrationMode,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.NoticeEvent> { backStackEntry ->
            val noticeEventRoute = backStackEntry.toRoute<Route.NoticeEvent>()

            NoticeEventScreen(
                cafeId = noticeEventRoute.param,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.CastEdit> { backStackEntry ->
            val castEditRoute = backStackEntry.toRoute<Route.CastEdit>()

            CastEditScreen(
                cafeId = castEditRoute.cafeId,
                castId = castEditRoute.castId,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.Schedule> { backStackEntry ->
            val scheduleRoute = backStackEntry.toRoute<Route.Schedule>()

            ScheduleScreen(
                castId = scheduleRoute.castId,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.CastManagement> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.CastManagement>()

            CastManagementScreen(
                cafeId = route.cafeId,
                cafeName = route.cafeName,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.MenuGoods> { backStackEntry ->
            val menuGoodsRoute = backStackEntry.toRoute<Route.MenuGoods>()

            MenuGoodsScreen(
                cafeId = menuGoodsRoute.param,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.MenuGoodsEdit> { backStackEntry ->
            val menuGoodsEditRoute = backStackEntry.toRoute<Route.MenuGoodsEdit>()

            MenuGoodsEditScreen(
                cafeId = menuGoodsEditRoute.cafeId,
                itemId = menuGoodsEditRoute.itemId,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.ReviewEdit> { backStackEntry ->
            val reviewEditRoute = backStackEntry.toRoute<Route.ReviewEdit>()

            ReviewEditScreen(
                cafeId = reviewEditRoute.cafeId,
                reviewId = reviewEditRoute.reviewId,
                onNavigationAction = viewModel::onAction
            )
        }
        composable<Route.Picture> { backStackEntry ->
            val pictureRoute = backStackEntry.toRoute<Route.Picture>()

            PictureScreen(
                imageUrl = pictureRoute.imageUrl,
                onAction = { action ->
                    when (action) {
                        PictureAction.ClickBack -> viewModel.onAction(NavigationAction.NavigateBack)
                    }
                }
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
        composable<Route.ResetPassword> { backStackEntry ->
            backStackEntry.toRoute<Route.ResetPassword>()
            ResetPasswordScreen(onNavigationAction = viewModel::onAction)
        }
        composable<Route.Notification> {
            NotificationScreen(onNavigationAction = viewModel::onAction)
        }
        composable<Route.Settings> {
            SettingsScreen(onNavigationAction = viewModel::onAction)
        }
        composable<Route.NotificationSettings> {
            NotificationSettingsScreen(onNavigationAction = viewModel::onAction)
        }
        composable<Route.AccountSettings> {
            AccountSettingsScreen(onNavigationAction = viewModel::onAction)
        }
        composable<Route.Inquiry> {
            InquiryLinkScreen(onNavigationAction = viewModel::onAction)
        }
        composable<Route.ChangePassword> {
            ChangePasswordScreen(onNavigationAction = viewModel::onAction)
        }
    }
}
