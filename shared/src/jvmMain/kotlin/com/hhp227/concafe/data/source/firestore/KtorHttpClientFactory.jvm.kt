package com.hhp227.concafe.data.source.firestore

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createPlatformHttpClient(): HttpClient {
    return HttpClient(CIO) {
        expectSuccess = false
    }
}
