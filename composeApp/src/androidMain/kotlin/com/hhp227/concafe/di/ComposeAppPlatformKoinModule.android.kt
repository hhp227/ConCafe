package com.hhp227.concafe.di

import android.app.Application
import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.hhp227.concafe.push.AndroidPushTokenClient
import org.koin.core.module.Module
import org.koin.dsl.module

internal fun androidPlatformModules(application: Application): List<Module> {
    return listOf(
        module {
            single<Context> { application }
            single<FirebaseMessaging> { FirebaseMessaging.getInstance() }
            single<AndroidPushTokenClient> {
                AndroidPushTokenClient(
                    context = get(),
                    firebaseMessaging = get()
                )
            }
        }
    )
}
