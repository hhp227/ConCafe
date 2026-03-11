package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.CafeDetail
import org.hhp227.concafe.domain.repository.CafeRepository

class DeleteCafeMenuGoodsUseCase(
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(cafeId: String, itemId: String): AppResult<CafeDetail> {
        return try {
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
