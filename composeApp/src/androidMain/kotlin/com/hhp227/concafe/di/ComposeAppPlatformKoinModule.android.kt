package com.hhp227.concafe.di

import android.app.Application
import android.content.Context
import com.hhp227.concafe.presentation.auth.signin.AndroidGoogleIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.GoogleIdTokenProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModules(): List<Module> {
    return emptyList()
}

internal fun androidPlatformModules(application: Application): List<Module> {
    return listOf(
        module {
            single<Context> { application }
            single {
                AndroidCurrentActivityProvider(application)
            }
            single<GoogleIdTokenProvider> {
                val currentActivityProvider = get<AndroidCurrentActivityProvider>()
                AndroidGoogleIdTokenProvider(activityProvider = currentActivityProvider::getCurrentActivity)
            }
        }
    )
}
