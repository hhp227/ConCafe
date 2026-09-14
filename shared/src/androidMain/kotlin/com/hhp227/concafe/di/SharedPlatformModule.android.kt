package com.hhp227.concafe.di

import android.app.Application
import android.content.Context
import com.hhp227.concafe.data.source.AndroidNativeAdDataSource
import com.hhp227.concafe.data.source.NativeAdDataSource
import com.hhp227.concafe.data.source.local.AndroidCafeExternalLinkLocalStore
import com.hhp227.concafe.data.source.local.AndroidUserPreferenceLocalDataSource
import com.hhp227.concafe.data.source.local.CafeExternalLinkLocalStore
import com.hhp227.concafe.data.source.local.UserPreferenceLocalDataSource
import com.hhp227.concafe.data.source.auth.AndroidGoogleIdTokenProvider
import com.hhp227.concafe.data.source.auth.AndroidKakaoIdTokenProvider
import com.hhp227.concafe.data.source.auth.AndroidNativeFirebaseAuthDataSource
import com.hhp227.concafe.data.source.auth.GoogleIdTokenProvider
import com.hhp227.concafe.data.source.auth.KakaoIdTokenProvider
import com.hhp227.concafe.data.source.auth.NativeFirebaseAuthDataSource
import com.hhp227.concafe.data.source.firestore.AndroidFirebaseAuthSessionStore
import com.hhp227.concafe.data.source.firestore.FirebaseAuthRestTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreRestApi
import com.hhp227.concafe.data.source.firestore.KtorFirebaseAuthRestClient
import com.hhp227.concafe.data.source.firestore.KtorFirestoreRestApi
import com.hhp227.concafe.data.source.firestore.PersistedFirebaseAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.createPlatformHttpClient
import com.hhp227.concafe.data.source.location.AndroidDeviceLocationDataSource
import com.hhp227.concafe.data.source.location.DeviceLocationDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun sharedPlatformModules(): List<Module> {
    return listOf(
        module {
            single { createPlatformHttpClient() }
            single<CafeExternalLinkLocalStore> { AndroidCafeExternalLinkLocalStore(get<Context>()) }
            single<UserPreferenceLocalDataSource> { AndroidUserPreferenceLocalDataSource(get<Context>()) }
            single<FirestoreRestApi> {
                KtorFirestoreRestApi(get(), FIREBASE_WEB_API_KEY)
            }
            single<FirestoreAuthTokenProvider> {
                PersistedFirebaseAuthTokenProvider(
                    delegate = FirebaseAuthRestTokenProvider(
                        apiKey = FIREBASE_WEB_API_KEY,
                        restClient = KtorFirebaseAuthRestClient(get())
                    ),
                    sessionStore = AndroidFirebaseAuthSessionStore(get<Context>())
                )
            }
            single(createdAtStart = true) {
                AndroidCurrentActivityProvider(get<Context>().applicationContext as Application)
            }
            single<NativeFirebaseAuthDataSource> {
                val currentActivityProvider = get<AndroidCurrentActivityProvider>()
                AndroidNativeFirebaseAuthDataSource(activityProvider = currentActivityProvider::getCurrentActivity)
            }
            single<GoogleIdTokenProvider> {
                val currentActivityProvider = get<AndroidCurrentActivityProvider>()
                AndroidGoogleIdTokenProvider(activityProvider = currentActivityProvider::getCurrentActivity)
            }
            single<KakaoIdTokenProvider> {
                val currentActivityProvider = get<AndroidCurrentActivityProvider>()
                AndroidKakaoIdTokenProvider(activityProvider = currentActivityProvider::getCurrentActivity)
            }
            single<DeviceLocationDataSource> {
                val currentActivityProvider = get<AndroidCurrentActivityProvider>()
                AndroidDeviceLocationDataSource(
                    context = get(),
                    activityProvider = currentActivityProvider::getCurrentActivity
                )
            }
            single<NativeAdDataSource> { AndroidNativeAdDataSource(get<Context>()) }
        }
    )
}
