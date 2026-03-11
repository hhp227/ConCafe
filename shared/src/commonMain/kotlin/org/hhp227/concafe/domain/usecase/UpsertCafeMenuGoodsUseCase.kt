package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.CafeDetail
import org.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import org.hhp227.concafe.domain.repository.CafeRepository

class UpsertCafeMenuGoodsUseCase(
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(update: CafeMenuGoodsUpsert): AppResult<CafeDetail> {
        return try {
            AppResult.Success(cafeRepository.upsertCafeMenuGoods(update))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
