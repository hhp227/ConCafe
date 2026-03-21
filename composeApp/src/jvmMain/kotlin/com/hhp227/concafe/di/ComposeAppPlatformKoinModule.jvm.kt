package com.hhp227.concafe.di

import com.hhp227.concafe.presentation.auth.signin.GoogleIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.JvmGoogleIdTokenProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModules(): List<Module> {
    return listOf(
        module {
            single<GoogleIdTokenProvider> { JvmGoogleIdTokenProvider() }
        }
    )
}
