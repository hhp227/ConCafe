package com.hhp227.concafe.data.source

data class StoreVersionResponse(
    val latestVersion: String,
    val storeUrl: String
)

interface AppUpdateRemoteDataSource {
    suspend fun fetchLatestVersion(storePlatform: String, storeId: String): StoreVersionResponse?
}
