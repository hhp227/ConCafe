package com.hhp227.concafe.data.source.firestore

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createPlatformHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        expectSuccess = false
    }
}
