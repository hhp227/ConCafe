package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.AppUpdateRemoteDataSource
import com.hhp227.concafe.domain.model.AppUpdateInfo
import com.hhp227.concafe.domain.repository.AppUpdateRepository

class AppUpdateRepositoryImpl(
    private val remoteDataSource: AppUpdateRemoteDataSource
) : AppUpdateRepository {
    override suspend fun getAvailableUpdate(
        storePlatform: String,
        storeId: String,
        currentVersion: String
    ): AppUpdateInfo? {
        val latest = remoteDataSource.fetchLatestVersion(storePlatform, storeId) ?: return null
        return if (isVersionNewer(latest.latestVersion, currentVersion)) {
            AppUpdateInfo(
                latestVersion = latest.latestVersion,
                storeUrl = latest.storeUrl
            )
        } else {
            null
        }
    }

    private fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        val remoteParts = remoteVersion.versionParts()
        val currentParts = currentVersion.versionParts()
        val maxSize = maxOf(remoteParts.size, currentParts.size)

        for (index in 0 until maxSize) {
            val remote = remoteParts.getOrElse(index) { 0 }
            val current = currentParts.getOrElse(index) { 0 }

            if (remote != current) {
                return remote > current
            }
        }
        return false
    }

    private fun String.versionParts(): List<Int> =
        split('.', '-', '_')
            .mapNotNull { part -> part.takeWhile(Char::isDigit).toIntOrNull() }
}
