package com.hhp227.concafe.di

import android.app.Application
import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.hhp227.concafe.push.AndroidPushTokenClient
import com.hhp227.concafe.presentation.auth.signin.AndroidGoogleIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.AndroidKakaoIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.GoogleIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.KakaoIdTokenProvider
import com.hhp227.concafe.presentation.auth.signup.AndroidPhoneAuthProvider
import com.hhp227.concafe.presentation.auth.signup.PhoneAuthProvider
import com.hhp227.concafe.presentation.main.checkin.AndroidCheckInLocationProvider
import com.hhp227.concafe.presentation.main.checkin.CheckInLocationProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModules(): List<Module> {
    return emptyList()
}

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
            single(createdAtStart = true) {
                AndroidCurrentActivityProvider(application)
            }
            single<GoogleIdTokenProvider> {
                val currentActivityProvider = get<AndroidCurrentActivityProvider>()
                AndroidGoogleIdTokenProvider(activityProvider = currentActivityProvider::getCurrentActivity)
            }
            single<KakaoIdTokenProvider> {
                val currentActivityProvider = get<AndroidCurrentActivityProvider>()
                AndroidKakaoIdTokenProvider(activityProvider = currentActivityProvider::getCurrentActivity)
            }
            single<CheckInLocationProvider> {
                val currentActivityProvider = get<AndroidCurrentActivityProvider>()
                AndroidCheckInLocationProvider(
                    context = get(),
                    activityProvider = currentActivityProvider::getCurrentActivity
                )
            }
            single<PhoneAuthProvider> {
                val currentActivityProvider = get<AndroidCurrentActivityProvider>()
                AndroidPhoneAuthProvider(activityProvider = currentActivityProvider::getCurrentActivity)
            }
        }
    )
}
