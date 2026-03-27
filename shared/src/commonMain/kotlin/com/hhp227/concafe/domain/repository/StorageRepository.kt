package com.hhp227.concafe.domain.repository

interface StorageRepository {
    suspend fun uploadImage(localPath: String, folder: String): String
    suspend fun uploadImageData(bytes: ByteArray, folder: String, fileName: String? = null): String
    suspend fun deleteImageByUrl(imageUrl: String)
}
