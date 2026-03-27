package com.hhp227.concafe.di

import android.content.Context
import com.hhp227.concafe.data.source.local.AndroidCafeExternalLinkLocalStore
import com.hhp227.concafe.data.source.local.CafeExternalLinkLocalStore
import com.hhp227.concafe.data.source.firestore.AndroidFirebaseAuthSessionStore
import com.hhp227.concafe.data.source.firestore.FirebaseAuthRestTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreRestApi
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
            single<CafeExternalLinkLocalStore> { AndroidCafeExternalLinkLocalStore(get<Context>()) }
            single<FirestoreRestApi> { KtorFirestoreRestApi(get(), FIREBASE_WEB_API_KEY) }
            single<FirestoreAuthTokenProvider> {
                PersistedFirebaseAuthTokenProvider(
                    delegate = FirebaseAuthRestTokenProvider(
                        apiKey = FIREBASE_WEB_API_KEY,
                        restClient = KtorFirebaseAuthRestClient(get())
                    ),
                    sessionStore = AndroidFirebaseAuthSessionStore(get<Context>())
                )
            }
        }
    )
}
