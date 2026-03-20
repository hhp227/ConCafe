package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.CompressedImageData

interface ImageCompressionRepository {
    suspend fun compressIfNeeded(localPath: String, maxBytes: Int): CompressedImageData
}
