package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.CafeDashboardData
import org.hhp227.concafe.domain.model.UserRole
import org.hhp227.concafe.domain.repository.AuthRepository
import org.hhp227.concafe.domain.repository.CafeDashboardRepository

class GetCafeDashboardUseCase(
    private val authRepository: AuthRepository,
    private val cafeDashboardRepository: CafeDashboardRepository
) {
    suspend operator fun invoke(cafeId: String): AppResult<CafeDashboardData> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else if (currentUser.role != UserRole.CAFE_OWNER && currentUser.role != UserRole.ADMIN) {
                AppResult.Failure(AppError.PermissionDenied)
            } else {
                val ownerUserId = if (currentUser.role == UserRole.ADMIN) null else currentUser.id
                AppResult.Success(cafeDashboardRepository.getCafeDashboardData(cafeId, ownerUserId))
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
