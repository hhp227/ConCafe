package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.NoticeManagementEvent
import com.hhp227.concafe.domain.event.publisher.NoticeManagementEventPublisher
import com.hhp227.concafe.domain.repository.NoticeRepository

class DeleteCafeEventUseCase(
    private val noticeRepository: NoticeRepository,
    private val noticeManagementEventPublisher: NoticeManagementEventPublisher
) {
    suspend operator fun invoke(cafeId: String, eventId: String): AppResult<String> {
        return try {
            val eventId = noticeRepository.deleteCafeEvent(cafeId, eventId)

            noticeManagementEventPublisher.publish(
                NoticeManagementEvent.EventDeleted(cafeId, eventId)
            )
            AppResult.Success(eventId)
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
