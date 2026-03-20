package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.repository.StorageRepository

class StorageRepositoryImpl : StorageRepository {
    override suspend fun uploadImage(localPath: String, folder: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun uploadImageData(
        bytes: ByteArray,
        folder: String,
        fileName: String?
    ): String {
        TODO("Not yet implemented")
    }
}