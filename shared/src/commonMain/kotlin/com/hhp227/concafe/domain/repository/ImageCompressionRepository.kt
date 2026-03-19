package com.hhp227.concafe.domain.repository

data class CompressedImageData(
    val bytes: ByteArray,
    val fileName: String
)

interface ImageCompressionRepository {
    suspend fun compressIfNeeded(localPath: String, maxBytes: Int): CompressedImageData
}
