package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.CafeDetail
import org.hhp227.concafe.domain.model.CafeInfoUpdate
import org.hhp227.concafe.domain.repository.CafeRepository

class UpdateCafeInfoUseCase(
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(update: CafeInfoUpdate): AppResult<CafeDetail> {
        return try {
            if (update.name.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("카페명은 비어 있을 수 없습니다."))
            } else {
                AppResult.Success(cafeRepository.updateCafeInfo(update))
            }
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
