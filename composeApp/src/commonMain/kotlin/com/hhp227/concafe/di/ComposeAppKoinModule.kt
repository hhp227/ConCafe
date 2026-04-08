package com.hhp227.concafe.di

import com.hhp227.concafe.presentation.AppViewModel
import com.hhp227.concafe.presentation.auth.signin.SignInViewModel
import com.hhp227.concafe.presentation.auth.resetpassword.ResetPasswordViewModel
import com.hhp227.concafe.presentation.auth.signup.SignUpViewModel
import com.hhp227.concafe.presentation.cafe.CafeViewModel
import com.hhp227.concafe.presentation.cast.CastViewModel
import com.hhp227.concafe.presentation.castedit.CastEditViewModel
import com.hhp227.concafe.presentation.main.MainViewModel
import com.hhp227.concafe.presentation.main.admin.AdminOperationsViewModel
import com.hhp227.concafe.presentation.main.cafemanagement.CafeManagementViewModel
import com.hhp227.concafe.presentation.main.cafemanagement.banner.BannerViewModel
import com.hhp227.concafe.presentation.main.cafemanagement.banneredit.BannerEditViewModel
import com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard.CafeDashboardViewModel
import com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo.CafeInfoEditViewModel
import com.hhp227.concafe.presentation.main.cafemanagement.externallink.ExternalLinkViewModel
import com.hhp227.concafe.presentation.main.cafemanagement.menugoods.MenuGoodsViewModel
import com.hhp227.concafe.presentation.main.cafemanagement.menugoodsedit.MenuGoodsEditViewModel
import com.hhp227.concafe.presentation.main.cafemanagement.noticeevent.NoticeEventViewModel
import com.hhp227.concafe.presentation.main.cafemanagement.schedule.ScheduleViewModel
import com.hhp227.concafe.presentation.main.checkin.CheckInViewModel
import com.hhp227.concafe.presentation.main.home.HomeViewModel
import com.hhp227.concafe.presentation.main.myinfo.MyInfoViewModel
import com.hhp227.concafe.presentation.main.explore.ExploreViewModel
import com.hhp227.concafe.presentation.main.fanmanagement.FanManagementViewModel
import com.hhp227.concafe.presentation.main.ranking.RankingViewModel
import com.hhp227.concafe.presentation.notification.NotificationViewModel
import com.hhp227.concafe.presentation.review.ReviewEditViewModel
import com.hhp227.concafe.presentation.settings.SettingsViewModel
import com.hhp227.concafe.presentation.settings.account.AccountSettingsViewModel
import com.hhp227.concafe.presentation.settings.changepassword.ChangePasswordViewModel
import com.hhp227.concafe.presentation.settings.inquiry.InquiryLinkViewModel
import com.hhp227.concafe.presentation.settings.notification.NotificationSettingsViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

private val composeAppPresentationModule = module {
    factory { AppViewModel(get(), get(), get(), get()) }
    factory { SignInViewModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { ResetPasswordViewModel(get()) }
    factory { SignUpViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { MainViewModel(get(), get(), get()) }
    factory { HomeViewModel(get(), get(), get(), get(), get(), get()) }
    factory { ExploreViewModel(get(), get(), get(), get(), get(), get()) }
    factory { CheckInViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { RankingViewModel(get(), get(), get(), get(), get(), get()) }
    factory { MyInfoViewModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { NotificationViewModel(get(), get(), get()) }
    factory { SettingsViewModel(get()) }
    factory { (cafeId: String) -> CafeViewModel(cafeId, get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { (castId: String) -> CastViewModel(castId, get(), get(), get(), get()) }
    factory { (cafeId: String, castId: String) -> CastEditViewModel(cafeId, castId, get(), get(), get(), get()) }
    factory { AdminOperationsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { (cafeId: String?) -> BannerViewModel(cafeId, get(), get(), get()) }
    factory { (initialCafeId: String?, initialBannerId: String?) ->
        BannerEditViewModel(initialCafeId, initialBannerId, get(), get(), get(), get(), get(), get(), get(), get())
    }
    factory { CafeManagementViewModel(get(), get(), get(), get(), get()) }
    factory { (cafeId: String) -> CafeDashboardViewModel(cafeId, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { (cafeId: String, isRegistrationMode: Boolean) -> CafeInfoEditViewModel(cafeId, isRegistrationMode, get(), get(), get(), get()) }
    factory { (title: String, url: String) -> ExternalLinkViewModel(title, url) }
    factory { (cafeId: String) -> MenuGoodsViewModel(cafeId, get(), get(), get(), get()) }
    factory { (cafeId: String, itemId: String) -> MenuGoodsEditViewModel(cafeId, itemId, get(), get(), get()) }
    factory { (cafeId: String) -> NoticeEventViewModel(cafeId, get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { (castId: String) -> ScheduleViewModel(castId, get(), get(), get(), get(), get()) }
    factory { FanManagementViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { NotificationViewModel(get(), get(), get()) }
    factory { (cafeId: String?, reviewId: String?) -> ReviewEditViewModel(cafeId, reviewId, get(), get(), get(), get(), get()) }
    factory { AccountSettingsViewModel(get(), get(), get(), get(), get(), get()) }
    factory { ChangePasswordViewModel(get()) }
    factory { InquiryLinkViewModel(get()) }
    factory { NotificationSettingsViewModel(get(), get(), get()) }
}

private val composeAppModules = listOf(
    composeAppPresentationModule
)

fun doInitConCafeAppKoin() {
    doInitConCafeAppKoin(emptyList())
}

fun doInitConCafeAppKoin(extraPlatformModules: List<Module>) {
    doInitKoin(composeAppModules + platformModules() + extraPlatformModules)
}

expect fun platformModules(): List<Module>
