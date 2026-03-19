package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.NoticeManagementEvent
import com.hhp227.concafe.domain.event.publisher.NoticeManagementEventPublisher
import com.hhp227.concafe.domain.model.CafeNoticeCreate
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.repository.NoticeRepository

class CreateCafeNoticeUseCase(
    private val noticeRepository: NoticeRepository,
    private val noticeManagementEventPublisher: NoticeManagementEventPublisher
) {
    suspend operator fun invoke(input: CafeNoticeCreate): AppResult<CafeNoticeManagementItem> {
        return try {
            noticeManagementEventPublisher.publish(
                NoticeManagementEvent.NoticeCreated(input.cafeId)
            )
            AppResult.Success(noticeRepository.createCafeNotice(input))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
