package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.GuestCastSchedule
import com.hhp227.concafe.domain.model.GuestCastScheduleUpsert
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.util.isScheduleEndAfterStart

class UpsertGuestCastScheduleUseCase(
    private val authRepository: AuthRepository,
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(input: GuestCastScheduleUpsert): AppResult<GuestCastSchedule> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            if (currentUser.role != UserRole.ADMIN && currentUser.role != UserRole.CAFE_OWNER) {
                return AppResult.Failure(AppError.PermissionDenied)
            }
            if (input.name.isBlank()) {
                return AppResult.Failure(AppError.ValidationFailed("guest name is required"))
            }
            if (input.startTime.isBlank()) {
                return AppResult.Failure(AppError.ValidationFailed("start time is required"))
            }
            if (input.endTime.isBlank()) {
                return AppResult.Failure(AppError.ValidationFailed("end time is required"))
            }
            if (!isScheduleEndAfterStart(input.startTime, input.endTime)) {
                return AppResult.Failure(AppError.ValidationFailed("end time must be after start time"))
            }
            AppResult.Success(castRepository.upsertGuestCastSchedule(input))
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
