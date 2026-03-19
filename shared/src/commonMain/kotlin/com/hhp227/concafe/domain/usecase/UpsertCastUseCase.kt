package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CastEvent
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeManagementRepository
import com.hhp227.concafe.domain.repository.CastRepository

class UpsertCastUseCase(
    private val authRepository: AuthRepository,
    private val cafeManagementRepository: CafeManagementRepository,
    private val castRepository: CastRepository,
    private val castEventPublisher: CastEventPublisher
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
                AppResult.Failure(AppError.ValidationFailed("등록할 카페를 찾을 수 없습니다."))
            } else {
                val isCreate = update.castId.isNullOrBlank()
                val detail = castRepository.upsertCast(
                    update.copy(cafeId = resolvedCafeId ?: update.cafeId)
                )

                castEventPublisher.publish(
                    if (isCreate) {
                        CastEvent.Created(detail.cast.cafeId, detail.cast)
                    } else {
                        CastEvent.Updated(detail.cast.cafeId, detail.cast)
                    }
                )
                AppResult.Success(detail)
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
