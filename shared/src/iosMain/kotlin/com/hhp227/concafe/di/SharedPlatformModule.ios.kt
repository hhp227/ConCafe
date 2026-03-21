package com.hhp227.concafe.di

import com.hhp227.concafe.data.source.firestore.FirebaseAuthRestTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreRestApi
import com.hhp227.concafe.data.source.firestore.KtorFirebaseAuthRestClient
import com.hhp227.concafe.data.source.firestore.KtorFirestoreRestApi
import com.hhp227.concafe.data.source.firestore.createPlatformHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

private const val FIREBASE_AUTH_API_KEY = "AIzaSyDK81bVr6DKDI6F9OFXWColSbANyrcWSSM"

actual fun sharedPlatformModules(): List<Module> {
    return listOf(
        module {
            single { createPlatformHttpClient() }
            single<FirestoreRestApi> { KtorFirestoreRestApi(get()) }
            single<FirestoreAuthTokenProvider> {
                FirebaseAuthRestTokenProvider(
                    apiKey = FIREBASE_AUTH_API_KEY,
                    restClient = KtorFirebaseAuthRestClient(get())
                )
            }
        }
    )
}
