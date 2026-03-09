package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.Visit
import org.hhp227.concafe.domain.repository.AuthRepository
import org.hhp227.concafe.domain.repository.VisitRepository

class CreateVisitUseCase(
    private val authRepository: AuthRepository,
    private val visitRepository: VisitRepository
) {
    suspend operator fun invoke(cafeId: String, visitedAt: String, memo: String?): AppResult<Visit> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else if (cafeId.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("cafeId is required"))
            } else if (visitedAt.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("visitedAt is required"))
            } else {
                AppResult.Success(
                    visitRepository.createVisit(
                        userId = currentUser.id,
                        cafeId = cafeId,
                        visitedAt = visitedAt,
                        memo = memo?.trim().takeIf { !it.isNullOrBlank() }
                    )
                )
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
