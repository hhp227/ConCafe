package com.hhp227.concafe.data.source.firestore

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout

actual fun createPlatformHttpClient(): HttpClient {
    return HttpClient(Android) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = HTTP_REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = HTTP_CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = HTTP_SOCKET_TIMEOUT_MILLIS
        }
    }
}

private const val HTTP_REQUEST_TIMEOUT_MILLIS = 120_000L
private const val HTTP_CONNECT_TIMEOUT_MILLIS = 30_000L
private const val HTTP_SOCKET_TIMEOUT_MILLIS = 120_000L
