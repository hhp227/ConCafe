package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastRepository

class GetCafeCastPageUseCase(
    private val authRepository: AuthRepository,
    private val castRepository: CastRepository
) {
    fun defaultPageSize(): Int {
        return DEFAULT_PAGE_SIZE
    }

    suspend operator fun invoke(
        cafeId: String,
        cursor: String?,
        pageSize: Int
    ): AppResult<PagedResult<CafeCastPreview>> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else if (currentUser.role != UserRole.CAFE_OWNER && currentUser.role != UserRole.ADMIN) {
                AppResult.Failure(AppError.PermissionDenied)
            } else {
                AppResult.Success(castRepository.getCafeCastPage(cafeId, cursor, pageSize))
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

private const val DEFAULT_PAGE_SIZE = 15
