package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeDetailCast
import com.hhp227.concafe.domain.repository.CastRepository

class GetCafeCastListPageUseCase(
    private val castRepository: CastRepository
) {
    fun defaultPageSize(): Int {
        return DEFAULT_PAGE_SIZE
    }

    suspend operator fun invoke(
        cafeId: String,
        cursor: String?
    ): AppResult<PagedResult<CafeDetailCast>> {
        return invoke(
            cafeId = cafeId,
            cursor = cursor,
            pageSize = DEFAULT_PAGE_SIZE
        )
    }

    suspend operator fun invoke(
        cafeId: String,
        cursor: String?,
        pageSize: Int
    ): AppResult<PagedResult<CafeDetailCast>> {
        return try {
            AppResult.Success(
                castRepository.getCafeCastListPage(
                    cafeId = cafeId,
                    cursor = cursor,
                    pageSize = pageSize
                )
            )
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}

private const val DEFAULT_PAGE_SIZE = 15
