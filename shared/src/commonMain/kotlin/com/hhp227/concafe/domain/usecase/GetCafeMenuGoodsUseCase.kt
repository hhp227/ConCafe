package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeMenuGoodsSection
import com.hhp227.concafe.domain.repository.CafeRepository

class GetCafeMenuGoodsUseCase(
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(cafeId: String): AppResult<CafeMenuGoodsSection> {
        return try {
            AppResult.Success(cafeRepository.getCafeMenuGoods(cafeId))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
