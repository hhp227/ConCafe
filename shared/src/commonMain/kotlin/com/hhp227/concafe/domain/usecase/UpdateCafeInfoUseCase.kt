package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.repository.CafeRepository

class UpdateCafeInfoUseCase(
    private val cafeRepository: CafeRepository,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher
) {
    suspend operator fun invoke(update: CafeInfoUpdate): AppResult<CafeDetail> {
        return try {
            if (update.name.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("카페명은 비어 있을 수 없습니다."))
            } else {
                val cafeDetail = cafeRepository.updateCafeInfo(update)

                cafeDetailEventPublisher.publish(
                    CafeDetailEvent.CafeInfoUpdated(update.cafeId, cafeDetail.cafe)
                )
                AppResult.Success(cafeDetail)
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
