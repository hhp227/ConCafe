package com.hhp227.concafe.di

import com.hhp227.concafe.presentation.main.home.HomeViewModel
import com.hhp227.concafe.presentation.main.myinfo.MyInfoViewModel
import com.hhp227.concafe.presentation.main.explore.ExploreViewModel
import com.hhp227.concafe.presentation.notification.NotificationViewModel
import com.hhp227.concafe.presentation.settings.SettingsViewModel
import org.koin.dsl.module

private val composeAppPresentationModule = module {
    factory { HomeViewModel(get(), get(), get(), get()) }
    factory { ExploreViewModel(get(), get(), get()) }
    factory { MyInfoViewModel(get(), get(), get(), get()) }
    factory { NotificationViewModel(get(), get(), get()) }
    factory { SettingsViewModel(get()) }
}

private val composeAppModules = listOf(
    composeAppPresentationModule
)

fun doInitConCafeAppKoin() {
    doInitKoin(composeAppModules)
}
