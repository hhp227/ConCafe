package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.VisitEvent
import com.hhp227.concafe.domain.event.publisher.VisitEventPublisher
import com.hhp227.concafe.domain.model.CurrentLocation
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.LocationRepository
import com.hhp227.concafe.domain.repository.VisitRepository

class CreateVisitUseCase(
    private val authRepository: AuthRepository,
    private val visitRepository: VisitRepository,
    private val locationRepository: LocationRepository,
    private val visitEventPublisher: VisitEventPublisher
) {
    // A location-based check-in is only valid at the user's verified current position, so the
    // fix is taken here rather than accepted from the caller. Location failures surface as
    // ValidationFailed(LocationFailureReason.name).
    suspend operator fun invoke(
        cafeId: String,
        visitedAt: String,
        memo: String?
    ): AppResult<Visit> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                return AppResult.Failure(AppError.Unauthorized)
            } else if (cafeId.isBlank()) {
                return AppResult.Failure(AppError.ValidationFailed("cafeId is required"))
            } else if (visitedAt.isBlank()) {
                return AppResult.Failure(AppError.ValidationFailed("visitedAt is required"))
            }
            val point = when (val location = locationRepository.getCurrentLocation()) {
                is CurrentLocation.Available -> location.point
                is CurrentLocation.Unavailable -> {
                    return AppResult.Failure(AppError.ValidationFailed(location.reason.name))
                }
            }
            val latitude = point.latitude
            val longitude = point.longitude

            if (latitude !in -90.0..90.0) {
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
                    return AppResult.Failure(AppError.ValidationFailed(verification.message))
                }
                val visited = visitRepository.createVisit(
                    userId = currentUser.id,
                    cafeId = cafeId,
                    visitedAt = visitedAt,
                    memo = memo,
                    latitude = latitude,
                    longitude = longitude
                )

                visitEventPublisher.publish(VisitEvent.Created(visited.id, cafeId))
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

    suspend fun invokeQr(
        cafeId: String,
        visitedAt: String,
        memo: String?
    ): AppResult<Visit> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else if (cafeId.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("cafeId is required"))
            } else if (visitedAt.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("visitedAt is required"))
            } else {
                val visited = visitRepository.createQrVisit(
                    userId = currentUser.id,
                    cafeId = cafeId,
                    visitedAt = visitedAt,
                    memo = memo
                )

                visitEventPublisher.publish(VisitEvent.Created(visited.id, cafeId))
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
