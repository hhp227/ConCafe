package com.hhp227.concafe.di

import com.hhp227.concafe.data.source.local.CafeExternalLinkLocalStore
import com.hhp227.concafe.data.source.local.JvmCafeExternalLinkLocalStore
import com.hhp227.concafe.data.source.firestore.CachedFirestoreRestApi
import com.hhp227.concafe.data.source.firestore.FirebaseAuthRestTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreRestCacheStore
import com.hhp227.concafe.data.source.firestore.FirestoreRestApi
import com.hhp227.concafe.data.source.firestore.JvmFirestoreRestCacheStore
import com.hhp227.concafe.data.source.firestore.JvmFirebaseAuthSessionStore
import com.hhp227.concafe.data.source.firestore.KtorFirebaseAuthRestClient
import com.hhp227.concafe.data.source.firestore.KtorFirestoreRestApi
import com.hhp227.concafe.data.source.firestore.PersistedFirebaseAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.createPlatformHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun sharedPlatformModules(): List<Module> {
    return listOf(
        module {
            single { createPlatformHttpClient() }
            single<CafeExternalLinkLocalStore> { JvmCafeExternalLinkLocalStore() }
            single<FirestoreRestCacheStore> { JvmFirestoreRestCacheStore() }
            single<FirestoreRestApi> {
                CachedFirestoreRestApi(
                    delegate = KtorFirestoreRestApi(get(), FIREBASE_WEB_API_KEY),
                    cacheStore = get()
                )
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
