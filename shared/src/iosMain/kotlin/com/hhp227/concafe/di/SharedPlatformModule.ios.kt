package com.hhp227.concafe.di

import com.hhp227.concafe.data.source.NativeAdDataSource
import com.hhp227.concafe.data.source.local.CafeExternalLinkLocalStore
import com.hhp227.concafe.data.source.local.IosUserPreferenceLocalDataSource
import com.hhp227.concafe.data.source.local.UserPreferenceLocalDataSource
import org.koin.core.KoinApplication
import com.hhp227.concafe.data.source.local.IosCafeExternalLinkLocalStore
import com.hhp227.concafe.data.source.firestore.FirebaseAuthRestTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreRestApi
import com.hhp227.concafe.data.source.firestore.IosFirebaseAuthSessionStore
import com.hhp227.concafe.data.source.firestore.KtorFirebaseAuthRestClient
import com.hhp227.concafe.data.source.firestore.KtorFirestoreRestApi
import com.hhp227.concafe.data.source.firestore.PersistedFirebaseAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.createPlatformHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

fun doInitKoinForIos(nativeAdDataSource: NativeAdDataSource): KoinApplication? {
    return doInitKoin(
        listOf(module { single<NativeAdDataSource> { nativeAdDataSource } })
    )
}

actual fun sharedPlatformModules(): List<Module> {
    return listOf(
        module {
            single { createPlatformHttpClient() }
            single<CafeExternalLinkLocalStore> { IosCafeExternalLinkLocalStore() }
            single<UserPreferenceLocalDataSource> { IosUserPreferenceLocalDataSource() }
            single<FirestoreRestApi> {
                KtorFirestoreRestApi(get(), FIREBASE_IOS_API_KEY)
            }
            single<FirestoreAuthTokenProvider> {
                PersistedFirebaseAuthTokenProvider(
                    delegate = FirebaseAuthRestTokenProvider(
                        apiKey = FIREBASE_IOS_API_KEY,
                        restClient = KtorFirebaseAuthRestClient(get())
                    ),
                    sessionStore = IosFirebaseAuthSessionStore()
                )
            }
        }
    )
}
