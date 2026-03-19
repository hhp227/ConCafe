package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.repository.CompressedImageData
import com.hhp227.concafe.domain.repository.ImageCompressionRepository

class PlatformImageCompressionRepository : ImageCompressionRepository {
    override suspend fun compressIfNeeded(localPath: String, maxBytes: Int): CompressedImageData {
        return compressImageForUpload(localPath = localPath, maxBytes = maxBytes)
    }
}

expect fun compressImageForUpload(localPath: String, maxBytes: Int): CompressedImageData
