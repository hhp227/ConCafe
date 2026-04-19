package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeEventEvent
import com.hhp227.concafe.domain.event.NoticeManagementEvent
import com.hhp227.concafe.domain.event.publisher.CafeEventEventPublisher
import com.hhp227.concafe.domain.event.publisher.NoticeManagementEventPublisher
import com.hhp227.concafe.domain.repository.NoticeRepository

class DeleteCafeEventUseCase(
    private val noticeRepository: NoticeRepository,
    private val noticeManagementEventPublisher: NoticeManagementEventPublisher,
    private val cafeEventEventPublisher: CafeEventEventPublisher
) {
    suspend operator fun invoke(cafeId: String, eventId: String): AppResult<String> {
        return try {
            val deletedEventId = noticeRepository.deleteCafeEvent(cafeId, eventId)

            noticeManagementEventPublisher.publish(
                NoticeManagementEvent.EventDeleted(cafeId, deletedEventId)
            )
            cafeEventEventPublisher.publish(
                CafeEventEvent.Deleted(cafeId = cafeId, eventId = deletedEventId)
            )
            AppResult.Success(deletedEventId)
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
