package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.VisitEvent
import com.hhp227.concafe.domain.event.publisher.VisitEventPublisher
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.VisitRepository

class CreateVisitUseCase(
    private val authRepository: AuthRepository,
    private val visitRepository: VisitRepository,
    private val visitEventPublisher: VisitEventPublisher
) {
    suspend operator fun invoke(
        cafeId: String,
        visitedAt: String,
        memo: String?,
        latitude: Double,
        longitude: Double
    ): AppResult<Visit> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else if (cafeId.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("cafeId is required"))
            } else if (visitedAt.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("visitedAt is required"))
            } else if (latitude !in -90.0..90.0) {
                AppResult.Failure(AppError.ValidationFailed("latitude out of range"))
            } else if (longitude !in -180.0..180.0) {
                AppResult.Failure(AppError.ValidationFailed("longitude out of range"))
            } else {
                val verification = visitRepository.verifyVisit(
                    cafeId = cafeId,
                    latitude = latitude,
                    longitude = longitude,
                    visitedAt = visitedAt
                )

                if (!verification.verified) {
                    return AppResult.Failure(
                        AppError.ValidationFailed(verification.message)
                    )
                }
                val visited = visitRepository.createVisit(
                    userId = currentUser.id,
                    cafeId = cafeId,
                    visitedAt = visitedAt,
                    memo = memo?.trim().takeIf { !it.isNullOrBlank() },
                    latitude = latitude,
                    longitude = longitude
                )

                visitEventPublisher.publish(VisitEvent.Created(visited.id))
                AppResult.Success(visited)
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
