package com.hhp227.concafe.di

import android.content.Context
import org.koin.core.module.Module
import org.koin.dsl.module

private object AndroidAppContextHolder {
    var appContext: Context? = null
}

fun setComposeAppContext(context: Context) {
    AndroidAppContextHolder.appContext = context.applicationContext
}

actual fun platformModules(): List<Module> {
    val appContext = requireNotNull(AndroidAppContextHolder.appContext) {
        "Android Context is not initialized. Call setComposeAppContext() before doInitConCafeAppKoin()."
    }

    return listOf(
        module {
            single<Context> { appContext }
        }
    )
}
