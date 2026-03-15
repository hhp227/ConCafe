package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CastClaimCandidate
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastClaimRepository

class GetMyRequestableCastPageUseCase(
    private val authRepository: AuthRepository,
    private val castClaimRepository: CastClaimRepository
) {
    fun defaultPageSize(): Int {
        return DEFAULT_PAGE_SIZE
    }

    suspend operator fun invoke(
        cursor: String?
    ): AppResult<PagedResult<CastClaimCandidate>> {
        return invoke(cursor = cursor, pageSize = DEFAULT_PAGE_SIZE)
    }

    suspend operator fun invoke(
        cursor: String?,
        pageSize: Int
    ): AppResult<PagedResult<CastClaimCandidate>> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            if (currentUser.role != UserRole.CAST) {
                return AppResult.Failure(AppError.PermissionDenied)
            }
            AppResult.Success(castClaimRepository.getMyRequestableCastPage(currentUser.id, cursor, pageSize))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}

private const val DEFAULT_PAGE_SIZE = 20
