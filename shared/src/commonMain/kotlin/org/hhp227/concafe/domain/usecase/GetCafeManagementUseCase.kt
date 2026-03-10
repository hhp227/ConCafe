package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.CafeManagementData
import org.hhp227.concafe.domain.model.UserRole
import org.hhp227.concafe.domain.repository.AuthRepository
import org.hhp227.concafe.domain.repository.CafeManagementRepository

class GetCafeManagementUseCase(
    private val authRepository: AuthRepository,
    private val cafeManagementRepository: CafeManagementRepository
) {
    suspend operator fun invoke(): AppResult<CafeManagementData> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else if (currentUser.role != UserRole.CAFE_OWNER && currentUser.role != UserRole.ADMIN) {
                AppResult.Failure(AppError.PermissionDenied)
            } else {
                AppResult.Success(cafeManagementRepository.getCafeManagementData(currentUser.id))
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
