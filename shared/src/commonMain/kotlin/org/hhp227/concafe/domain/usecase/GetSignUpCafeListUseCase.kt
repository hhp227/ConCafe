package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.repository.CafeRepository

class GetSignUpCafeListUseCase(
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(): AppResult<List<Cafe>> {
        return try {
            val result = cafeRepository.searchCafes(
                query = null,
                country = null,
                city = null,
                sort = CafeSort.POPULAR,
                cursor = null,
                pageSize = 50
            )
            AppResult.Success(result.items.filter { it.approved })
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
