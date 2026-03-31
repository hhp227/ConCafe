package com.hhp227.concafe.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.hhp227.concafe.presentation.navigation.NavigationEvent.NavigateTo
import com.hhp227.concafe.presentation.navigation.Route.Banner
import com.hhp227.concafe.presentation.navigation.Route.Cafe
import com.hhp227.concafe.presentation.navigation.Route.BannerEdit
import com.hhp227.concafe.presentation.navigation.Route.CafeDashboard
import com.hhp227.concafe.presentation.navigation.Route.CafeInfoEdit
import com.hhp227.concafe.presentation.navigation.Route.Cast
import com.hhp227.concafe.presentation.navigation.Route.CastEdit
import com.hhp227.concafe.presentation.navigation.Route.ExternalLink
import com.hhp227.concafe.presentation.navigation.Route.Main
import com.hhp227.concafe.presentation.navigation.Route.MenuGoods
import com.hhp227.concafe.presentation.navigation.Route.MenuGoodsEdit
import com.hhp227.concafe.presentation.navigation.Route.NoticeEvent
import com.hhp227.concafe.presentation.navigation.Route.ReviewEdit
import com.hhp227.concafe.presentation.navigation.Route.Schedule

class NavigationViewModel : ViewModel() {
    private val _event = MutableSharedFlow<NavigationEvent>()
    val event = _event.asSharedFlow()

    fun onAction(action: NavigationAction) {
        viewModelScope.launch {
            when (action) {
                is NavigationAction.NavigateToMain -> {
                    _event.emit(NavigateTo(Main(action.initialTab)))
                }
                is NavigationAction.NavigateToCast -> {
                    _event.emit(NavigateTo(Cast(action.id)))
                }
                is NavigationAction.NavigateToCafe -> {
                    _event.emit(NavigateTo(Cafe(action.id)))
                }
                is NavigationAction.NavigateToCafeDashboard -> {
                    _event.emit(NavigateTo(CafeDashboard(action.id)))
                }
                is NavigationAction.NavigateToBanner -> {
                    _event.emit(NavigateTo(Banner(action.cafeId)))
                }
                is NavigationAction.NavigateToBannerEdit -> {
                    _event.emit(
                        NavigateTo(
                            BannerEdit(
                                cafeId = action.cafeId,
                                bannerId = action.bannerId
                            )
                        )
                    )
                }
                is NavigationAction.NavigateToExternalLink -> {
                    _event.emit(NavigateTo(ExternalLink(action.title, action.url)))
                }
                is NavigationAction.NavigateToCafeInfoEdit -> {
                    _event.emit(
                        NavigateTo(
                            CafeInfoEdit(
                                param = action.id,
                                isRegistrationMode = action.isRegistrationMode
                            )
                        )
                    )
                }
                is NavigationAction.NavigateToNoticeEvent -> {
                    _event.emit(NavigateTo(NoticeEvent(action.id)))
                }
                is NavigationAction.NavigateToCastEdit -> {
                    _event.emit(NavigateTo(CastEdit(action.cafeId, action.castId)))
                }
                is NavigationAction.NavigateToSchedule -> {
                    _event.emit(NavigateTo(Schedule(action.castId)))
                }
                is NavigationAction.NavigateToMenuGoods -> {
                    _event.emit(NavigateTo(MenuGoods(action.id)))
                }
                is NavigationAction.NavigateToMenuGoodsEdit -> {
                    _event.emit(NavigateTo(MenuGoodsEdit(action.cafeId, action.itemId)))
                }
                is NavigationAction.NavigateToReviewEdit -> {
                    _event.emit(NavigateTo(ReviewEdit(action.cafeId)))
                }
                NavigationAction.NavigateToSignIn -> {
                    _event.emit(NavigateTo(Route.SignIn))
                }
                NavigationAction.NavigateToSignUp -> {
                    _event.emit(NavigateTo(Route.SignUp))
                }
                NavigationAction.NavigateToResetPassword -> {
                    _event.emit(NavigateTo(Route.ResetPassword))
                }
                is NavigationAction.NavigateToNotification -> {
                    _event.emit(NavigateTo(Route.Notification))
                }
                NavigationAction.NavigateToSettings -> {
                    _event.emit(NavigateTo(Route.Settings))
                }
                NavigationAction.NavigateToNotificationSettings -> {
                    _event.emit(NavigateTo(Route.NotificationSettings))
                }
                NavigationAction.NavigateToAccountSettings -> {
                    _event.emit(NavigateTo(Route.AccountSettings))
                }
                NavigationAction.NavigateToInquiry -> {
                    _event.emit(NavigateTo(Route.Inquiry))
                }
                NavigationAction.NavigateToChangePassword -> {
                    _event.emit(NavigateTo(Route.ChangePassword))
                }
                is NavigationAction.NavigateBack -> {
                    _event.emit(NavigationEvent.NavigateBack)
                }
                NavigationAction.RefreshUnreadNotificationCount -> {
                    _event.emit(NavigationEvent.RefreshUnreadNotificationCount)
                }
            }
        }
    }
}
