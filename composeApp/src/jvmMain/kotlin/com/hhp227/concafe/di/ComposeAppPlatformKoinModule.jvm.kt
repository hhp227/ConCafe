package com.hhp227.concafe.di

import com.hhp227.concafe.presentation.auth.signin.GoogleIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.JvmGoogleIdTokenProvider
import com.hhp227.concafe.presentation.main.checkin.CheckInLocationProvider
import com.hhp227.concafe.presentation.main.checkin.JvmCheckInLocationProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModules(): List<Module> {
    return listOf(
        module {
            single<GoogleIdTokenProvider> { JvmGoogleIdTokenProvider() }
            single<CheckInLocationProvider> { JvmCheckInLocationProvider() }
        }
    )
}
