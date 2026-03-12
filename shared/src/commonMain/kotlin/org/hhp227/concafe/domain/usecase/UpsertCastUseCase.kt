package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.CastDetail
import org.hhp227.concafe.domain.model.CastUpsert
import org.hhp227.concafe.domain.model.UserRole
import org.hhp227.concafe.domain.repository.AuthRepository
import org.hhp227.concafe.domain.repository.CafeManagementRepository
import org.hhp227.concafe.domain.repository.CastRepository

class UpsertCastUseCase(
    private val authRepository: AuthRepository,
    private val cafeManagementRepository: CafeManagementRepository,
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(update: CastUpsert): AppResult<CastDetail> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            val resolvedCafeId = when {
                !update.cafeId.isNullOrBlank() -> update.cafeId
                !update.castId.isNullOrBlank() -> null
                currentUser.role == UserRole.CAFE_OWNER -> {
                    cafeManagementRepository.getCafeManagementData(currentUser.id)
                        .ownedCafes
                        .firstOrNull()
                        ?.id
                }
                else -> null
            }

            if (update.castId.isNullOrBlank() && resolvedCafeId.isNullOrBlank()) {
                return AppResult.Failure(AppError.ValidationFailed("등록할 카페를 찾을 수 없습니다."))
            }
            AppResult.Success(
                castRepository.upsertCast(
                    update.copy(cafeId = resolvedCafeId ?: update.cafeId)
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
