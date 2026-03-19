package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.ImageCompressionRepository
import com.hhp227.concafe.domain.repository.StorageRepository

class UploadImageUseCase(
    private val storageRepository: StorageRepository,
    private val imageCompressionRepository: ImageCompressionRepository
) {
    suspend operator fun invoke(localPath: String, folder: String): AppResult<String> {
        return try {
            if (localPath.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("이미지 경로가 비어 있습니다."))
            } else if (localPath.startsWith("http://") || localPath.startsWith("https://")) {
                AppResult.Success(localPath)
            } else {
                val compressed = imageCompressionRepository.compressIfNeeded(
                    localPath = localPath,
                    maxBytes = MAX_UPLOAD_IMAGE_BYTES
                )
                AppResult.Success(
                    storageRepository.uploadImageData(
                        bytes = compressed.bytes,
                        folder = folder,
                        fileName = compressed.fileName
                    )
                )
            }
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid image input"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    suspend fun uploadData(bytes: ByteArray, folder: String, fileName: String? = null): AppResult<String> {
        return try {
            if (bytes.isEmpty()) {
                AppResult.Failure(AppError.ValidationFailed("이미지 데이터가 비어 있습니다."))
            } else {
                AppResult.Success(storageRepository.uploadImageData(bytes, folder, fileName))
            }
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid image input"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    companion object {
        private const val MAX_UPLOAD_IMAGE_BYTES = 1_048_576
    }
}
