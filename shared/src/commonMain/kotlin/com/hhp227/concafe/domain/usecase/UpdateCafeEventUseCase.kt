package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.NoticeManagementEvent
import com.hhp227.concafe.domain.event.publisher.NoticeManagementEventPublisher
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeEventUpdate
import com.hhp227.concafe.domain.repository.NoticeRepository

class UpdateCafeEventUseCase(
    private val noticeRepository: NoticeRepository,
    private val noticeManagementEventPublisher: NoticeManagementEventPublisher
) {
    suspend operator fun invoke(input: CafeEventUpdate): AppResult<CafeEventManagementItem> {
        return try {
            val event = noticeRepository.updateCafeEvent(input)

            noticeManagementEventPublisher.publish(
                NoticeManagementEvent.EventUpdated(input.cafeId, event)
            )
            AppResult.Success(event)
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
