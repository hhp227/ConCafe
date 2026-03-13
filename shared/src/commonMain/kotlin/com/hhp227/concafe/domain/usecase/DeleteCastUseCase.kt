package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeManagementRepository
import com.hhp227.concafe.domain.repository.CastRepository

class DeleteCastUseCase(
    private val authRepository: AuthRepository,
    private val cafeManagementRepository: CafeManagementRepository,
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(castId: String): AppResult<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)

            if (currentUser.role != UserRole.CAFE_OWNER && currentUser.role != UserRole.ADMIN) {
                return AppResult.Failure(AppError.PermissionDenied)
            }

            val detail = castRepository.getCastDetail(castId)
            if (currentUser.role == UserRole.CAFE_OWNER) {
                val ownedCafeIds = cafeManagementRepository.getCafeManagementData(currentUser.id)
                    .ownedCafes
                    .map { it.id }
                    .toSet()
                if (!ownedCafeIds.contains(detail.cast.cafeId)) {
                    return AppResult.Failure(AppError.PermissionDenied)
                }
            }

            castRepository.deleteCast(castId)
            AppResult.Success(Unit)
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
