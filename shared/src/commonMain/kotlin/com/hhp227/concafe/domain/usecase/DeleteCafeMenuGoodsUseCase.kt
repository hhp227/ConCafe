package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.repository.CafeRepository

class DeleteCafeMenuGoodsUseCase(
    private val cafeRepository: CafeRepository,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher
) {
    suspend operator fun invoke(cafeId: String, itemId: String): AppResult<CafeDetail> {
        return try {
            cafeDetailEventPublisher.publish(
                if (itemId.startsWith("goods")) {
                    CafeDetailEvent.GoodsDeleted(cafeId, itemId)
                } else {
                    CafeDetailEvent.MenuDeleted(cafeId, itemId)
                }
            )
            AppResult.Success(cafeRepository.deleteCafeMenuGoods(cafeId, itemId))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
