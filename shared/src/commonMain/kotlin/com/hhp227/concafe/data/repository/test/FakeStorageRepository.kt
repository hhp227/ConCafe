package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.domain.repository.StorageRepository
import kotlin.random.Random

class FakeStorageRepository : StorageRepository {
    override suspend fun uploadImage(localPath: String, folder: String): String {
        val normalizedPath = localPath.trim()
        require(normalizedPath.isNotEmpty()) { "image path is required" }
        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            return normalizedPath
        }

        val fileToken = normalizedPath.substringAfterLast('/').ifEmpty { "image" }
        return buildMockUrl(folder = folder, fileToken = fileToken)
    }

    override suspend fun uploadImageData(bytes: ByteArray, folder: String, fileName: String?): String {
        require(bytes.isNotEmpty()) { "image data is required" }
        val token = (fileName ?: "image-${bytes.size}bytes").ifBlank { "image" }
        return buildMockUrl(folder = folder, fileToken = token)
    }

    private fun buildMockUrl(folder: String, fileToken: String): String {
        val safeFolder = folder.trim().ifEmpty { "uploads" }
        val normalizedToken = fileToken
            .trim()
            .ifEmpty { "image" }
            .replace('.', '-')
            .replace('_', '-')
        val randomToken = Random.Default.nextInt(100000, 999999)
        return "https://mock-storage.concafe/$safeFolder/$normalizedToken-$randomToken.jpg"
    }
}