package com.hhp227.concafe.data.source.firestore

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(): HttpClient
