package com.hhp227.concafe.di

import com.hhp227.concafe.data.source.JvmNativeAdDataSource
import com.hhp227.concafe.data.source.NativeAdDataSource
import com.hhp227.concafe.data.source.local.CafeExternalLinkLocalStore
import com.hhp227.concafe.data.source.local.JvmCafeExternalLinkLocalStore
import com.hhp227.concafe.data.source.local.JvmUserPreferenceLocalDataSource
import com.hhp227.concafe.data.source.local.UserPreferenceLocalDataSource
import com.hhp227.concafe.data.source.firestore.FirebaseAuthRestTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreRestApi
import com.hhp227.concafe.data.source.auth.GoogleIdTokenProvider
import com.hhp227.concafe.data.source.auth.JvmGoogleIdTokenProvider
import com.hhp227.concafe.data.source.auth.JvmKakaoIdTokenProvider
import com.hhp227.concafe.data.source.auth.JvmNativeFirebaseAuthDataSource
import com.hhp227.concafe.data.source.auth.KakaoIdTokenProvider
import com.hhp227.concafe.data.source.auth.NativeFirebaseAuthDataSource
import com.hhp227.concafe.data.source.firestore.JvmFirebaseAuthSessionStore
import com.hhp227.concafe.data.source.firestore.KtorFirebaseAuthRestClient
import com.hhp227.concafe.data.source.firestore.KtorFirestoreRestApi
import com.hhp227.concafe.data.source.firestore.PersistedFirebaseAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.createPlatformHttpClient
import com.hhp227.concafe.data.source.location.DeviceLocationDataSource
import com.hhp227.concafe.data.source.location.JvmDeviceLocationDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun sharedPlatformModules(): List<Module> {
    return listOf(
        module {
            single { createPlatformHttpClient() }
            single<CafeExternalLinkLocalStore> { JvmCafeExternalLinkLocalStore() }
            single<UserPreferenceLocalDataSource> { JvmUserPreferenceLocalDataSource() }
            single<FirestoreRestApi> {
                KtorFirestoreRestApi(get(), FIREBASE_WEB_API_KEY)
            }
            single<FirestoreAuthTokenProvider> {
                PersistedFirebaseAuthTokenProvider(
                    delegate = FirebaseAuthRestTokenProvider(
                        apiKey = FIREBASE_WEB_API_KEY,
                        kakaoOidcProviderId = desktopKakaoOidcProviderId(),
                        restClient = KtorFirebaseAuthRestClient(get())
                    ),
                    sessionStore = JvmFirebaseAuthSessionStore()
                )
            }
            single<NativeFirebaseAuthDataSource> { JvmNativeFirebaseAuthDataSource() }
            single<GoogleIdTokenProvider> { JvmGoogleIdTokenProvider() }
            single<KakaoIdTokenProvider> { JvmKakaoIdTokenProvider() }
            single<DeviceLocationDataSource> { JvmDeviceLocationDataSource() }
            single<NativeAdDataSource> { JvmNativeAdDataSource() }
        }
    )
}

private fun desktopKakaoOidcProviderId(): String {
    val configuredProviderId = System.getProperty("concafe.kakao.oidcProviderId")

    return if (configuredProviderId.isNullOrBlank()) {
        "oidc.kakao_web"
    } else {
        configuredProviderId
    }
}
