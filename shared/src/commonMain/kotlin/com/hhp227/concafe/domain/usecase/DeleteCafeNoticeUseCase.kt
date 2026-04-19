package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.NoticeManagementEvent
import com.hhp227.concafe.domain.event.publisher.NoticeManagementEventPublisher
import com.hhp227.concafe.domain.repository.NoticeRepository

class DeleteCafeNoticeUseCase(
    private val noticeRepository: NoticeRepository,
    private val noticeManagementEventPublisher: NoticeManagementEventPublisher
) {
    suspend operator fun invoke(cafeId: String, noticeId: String): AppResult<String> {
        return try {
            val deletedNoticeId = noticeRepository.deleteCafeNotice(cafeId, noticeId)

            noticeManagementEventPublisher.publish(
                NoticeManagementEvent.NoticeDeleted(cafeId, deletedNoticeId)
            )
            AppResult.Success(deletedNoticeId)
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
