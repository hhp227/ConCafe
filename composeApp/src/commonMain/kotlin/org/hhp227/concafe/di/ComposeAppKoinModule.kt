package org.hhp227.concafe.di

import org.hhp227.concafe.presentation.main.home.HomeViewModel
import org.koin.dsl.module

private val composeAppPresentationModule = module {
    factory { HomeViewModel(get()) }
}

private val composeAppModules = listOf(
    composeAppPresentationModule
)

fun doInitConCafeAppKoin() {
    doInitKoin(composeAppModules)
}
