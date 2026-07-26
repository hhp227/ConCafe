package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.StorageRepository

class DeleteImageUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(imageUrl: String): AppResult<Unit> {
        return try {
            if (imageUrl.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("이미지 URL이 비어 있습니다."))
            } else {
                storageRepository.deleteImageByUrl(imageUrl)
                AppResult.Success(Unit)
            }
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid image url"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
