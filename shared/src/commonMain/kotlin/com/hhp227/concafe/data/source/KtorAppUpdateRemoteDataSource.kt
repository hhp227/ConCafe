package com.hhp227.concafe.data.source

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

class KtorAppUpdateRemoteDataSource(
    private val httpClient: HttpClient
) : AppUpdateRemoteDataSource {
    override suspend fun fetchLatestVersion(storePlatform: String, storeId: String): StoreVersionResponse? {
        return when (storePlatform.uppercase()) {
            STORE_PLATFORM_ANDROID -> fetchPlayStoreVersion(storeId)
            STORE_PLATFORM_IOS -> fetchAppStoreVersion(storeId)
            else -> null
        }
    }

    private suspend fun fetchPlayStoreVersion(applicationId: String): StoreVersionResponse? {
        val storeUrl = "https://play.google.com/store/apps/details?id=$applicationId"
        val body = httpClient.get("$storeUrl&hl=en&gl=US") {
            header("User-Agent", "Mozilla/5.0")
        }.bodyAsText()
        val versionPattern = Regex("""\[\[\["([0-9]+(?:\.[0-9A-Za-z_-]+)+)"\]\]""")
        val latestVersion = versionPattern.findAll(body)
            .map { it.groupValues[1] }
            .firstOrNull { it.any(Char::isDigit) }
            ?: return null

        return StoreVersionResponse(latestVersion = latestVersion, storeUrl = storeUrl)
    }

    private suspend fun fetchAppStoreVersion(bundleId: String): StoreVersionResponse? {
        val body = httpClient.get("https://itunes.apple.com/lookup?bundleId=$bundleId").bodyAsText()
        val latestVersion = Regex(""""version"\s*:\s*"([^"]+)"""")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?: return null
        val storeUrl = Regex(""""trackViewUrl"\s*:\s*"([^"]+)"""")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\/", "/")
            ?: return null

        return StoreVersionResponse(latestVersion = latestVersion, storeUrl = storeUrl)
    }

    private companion object {
        private const val STORE_PLATFORM_ANDROID = "ANDROID"
        private const val STORE_PLATFORM_IOS = "IOS"
    }
}
