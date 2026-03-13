package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeDashboardRepository

class GetCafeDashboardUseCase(
    private val authRepository: AuthRepository,
    private val cafeDashboardRepository: CafeDashboardRepository
) {
    suspend operator fun invoke(cafeId: String): AppResult<CafeDashboardData> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else {
                val canAccess = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.CAFE_OWNER

                if (!canAccess) {
                    AppResult.Failure(AppError.PermissionDenied)
                } else {
                    val ownerUserId = if (currentUser.role == UserRole.CAFE_OWNER) currentUser.id else null
                    AppResult.Success(cafeDashboardRepository.getCafeDashboardData(cafeId, ownerUserId))
                }
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
