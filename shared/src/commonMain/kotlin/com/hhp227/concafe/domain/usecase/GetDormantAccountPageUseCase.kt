package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.DormantAccountFilter
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.policy.DormantAccountPolicy
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.UserRepository

class GetDormantAccountPageUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    private val policy = DormantAccountPolicy()

    suspend operator fun invoke(
        filter: DormantAccountFilter,
        cursor: String?,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): AppResult<PagedResult<User>> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)

            if (currentUser.role != UserRole.ADMIN) {
                AppResult.Failure(AppError.PermissionDenied)
            } else {
                AppResult.Success(
                    userRepository.getDormantAccountPage(
                        filter = filter,
                        lastLoginBefore = policy.dormantCutoffIso(),
                        cursor = cursor,
                        pageSize = pageSize.coerceAtLeast(1)
                    )
                )
            }
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 15
    }
}
