package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.StorageRepository

class UploadImageUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(localPath: String, folder: String): AppResult<String> {
        return try {
            if (localPath.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("이미지 경로가 비어 있습니다."))
            } else {
                AppResult.Success(storageRepository.uploadImage(localPath, folder))
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
}
