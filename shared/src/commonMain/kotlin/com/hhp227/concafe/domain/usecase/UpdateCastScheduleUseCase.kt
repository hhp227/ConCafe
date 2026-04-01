package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.event.ScheduleManagementEvent
import com.hhp227.concafe.domain.event.publisher.ScheduleManagementEventPublisher
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastRepository

class UpdateCastScheduleUseCase(
    private val authRepository: AuthRepository,
    private val castRepository: CastRepository,
    private val scheduleManagementEventPublisher: ScheduleManagementEventPublisher
) {
    suspend operator fun invoke(input: CastScheduleUpdate): AppResult<CastSchedule?> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            val castDetail = castRepository.getCastDetail(input.castId)
            val isAllowed = when (currentUser.role) {
                UserRole.ADMIN, UserRole.CAFE_OWNER -> true
                UserRole.CAST -> castDetail.cast.linkedUserId == currentUser.id
                else -> false
            }

            if (!isAllowed) {
                return AppResult.Failure(AppError.PermissionDenied)
            }
            if (input.status == CastScheduleStatus.WORK) {
                val startTime = input.startTime?.takeIf { it.isNotBlank() }
                    ?: return AppResult.Failure(AppError.ValidationFailed("start time is required"))
                val endTime = input.endTime?.takeIf { it.isNotBlank() }
                    ?: return AppResult.Failure(AppError.ValidationFailed("end time is required"))
                if (startTime >= endTime) {
                    return AppResult.Failure(AppError.ValidationFailed("end time must be after start time"))
                }
            }
            val updated = castRepository.updateCastSchedule(input)
            scheduleManagementEventPublisher.publish(
                ScheduleManagementEvent.Updated(
                    castId = input.castId,
                    date = input.date,
                    status = input.status,
                    startTime = input.startTime,
                    endTime = input.endTime
                )
            )
            AppResult.Success(updated)
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
