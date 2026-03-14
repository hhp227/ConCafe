package com.hhp227.concafe.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import com.hhp227.concafe.presentation.auth.signin.SignInScreen
import com.hhp227.concafe.presentation.auth.signup.SignUpScreen
import com.hhp227.concafe.presentation.banner.BannerEditScreen
import com.hhp227.concafe.presentation.cafe.CafeScreen
import com.hhp227.concafe.presentation.cast.CastScreen
import com.hhp227.concafe.presentation.castedit.CastEditScreen
import com.hhp227.concafe.presentation.main.MainScreen
import com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard.CafeDashboardScreen
import com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo.CafeInfoEditScreen
import com.hhp227.concafe.presentation.main.cafemanagement.menugoods.MenuGoodsScreen
import com.hhp227.concafe.presentation.main.cafemanagement.menugoodsedit.MenuGoodsEditScreen
import com.hhp227.concafe.presentation.main.cafemanagement.noticeevent.NoticeEventScreen
import com.hhp227.concafe.presentation.main.cafemanagement.schedule.ScheduleScreen
import com.hhp227.concafe.presentation.notification.NotificationScreen
import com.hhp227.concafe.presentation.review.ReviewEditScreen
import com.hhp227.concafe.presentation.settings.SettingsScreen

private const val DESKTOP_TWO_PANE_MIN_WIDTH_DP = 800

@Composable
fun NavigationScreen(
    viewModel: NavigationViewModel = viewModel()
) {
    var currentMainTab by remember { mutableStateOf("home") }
    val detailStack = remember { mutableStateListOf<Pair<Int, Route>>() }
    var nextDetailEntryId by remember { mutableIntStateOf(0) }
    val currentDetailEntry = detailStack.lastOrNull()

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateTo -> {
                    if (event.route is Route.Main) {
                        currentMainTab = event.route.initialTab ?: "home"
                        detailStack.clear()
                    } else {
                        detailStack.add(
                            nextDetailEntryId++ to event.route
                        )
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
            Box(
                modifier = if (isTwoPaneMode && currentDetailEntry != null) {
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.5f)
                        .align(Alignment.CenterStart)
                } else {
                    Modifier.fillMaxSize()
                }
            ) {
                MainScreen(
                    initialTab = currentMainTab,
                    onNavigationAction = viewModel::onAction
                )
            }
            if (currentDetailEntry != null) {
                Box(
                    modifier = if (isTwoPaneMode) {
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                            .align(Alignment.CenterEnd)
                            .background(MaterialTheme.colorScheme.background)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    }
                ) {
                    DetailStackPane(
                        detailStack = detailStack,
                        currentDetailEntry = currentDetailEntry
                    ) { route ->
                        DetailRoutePane(
                            route = route,
                            onNavigationAction = viewModel::onAction
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStackPane(
    detailStack: List<Pair<Int, Route>>,
    currentDetailEntry: Pair<Int, Route>,
    content: @Composable (Route) -> Unit
) {
    val (currentDetailEntryId, _) = currentDetailEntry

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        detailStack.forEachIndexed { index, (detailEntryId, route) ->
            key(detailEntryId) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(index.toFloat())
                        .alpha(if (detailEntryId == currentDetailEntryId) 1f else 0f)
                ) {
                    content(route)
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
        is Route.CafeDashboard -> {
            CafeDashboardScreen(
                cafeId = route.param,
                onNavigationAction = onNavigationAction
            )
        }
        Route.BannerEdit -> {
            BannerEditScreen(onNavigationAction = onNavigationAction)
        }
        is Route.CafeInfoEdit -> {
            CafeInfoEditScreen(
                cafeId = route.param,
                isRegistrationMode = route.isRegistrationMode,
                onNavigationAction = onNavigationAction
            )
        }
        is Route.NoticeEvent -> {
            NoticeEventScreen(
                cafeId = route.param,
                onNavigationAction = onNavigationAction
            )
        }
        is Route.CastEdit -> {
            CastEditScreen(
                cafeId = route.cafeId,
                castId = route.castId,
                onNavigationAction = onNavigationAction
            )
        }
        is Route.Schedule -> {
            ScheduleScreen(
                castId = route.castId,
                onNavigationAction = onNavigationAction
            )
        }
        is Route.MenuGoods -> {
            MenuGoodsScreen(
                cafeId = route.param,
                onNavigationAction = onNavigationAction
            )
        }
        is Route.MenuGoodsEdit -> {
            MenuGoodsEditScreen(
                cafeId = route.cafeId,
                itemId = route.itemId,
                onNavigationAction = onNavigationAction
            )
        }
        is Route.ReviewEdit -> {
            ReviewEditScreen(
                cafeId = route.cafeId,
                onNavigationAction = onNavigationAction
            )
        }
        Route.SignIn -> {
            SignInScreen(onNavigate = onNavigationAction)
        }
        Route.SignUp -> {
            SignUpScreen(onNavigate = onNavigationAction)
        }
        Route.Notification -> {
            NotificationScreen(
                onNavigationAction = onNavigationAction
            )
        }
        Route.Settings -> {
            SettingsScreen(
                onNavigationAction = onNavigationAction
            )
        }
        else -> {
            Unit
        }
    }
}
