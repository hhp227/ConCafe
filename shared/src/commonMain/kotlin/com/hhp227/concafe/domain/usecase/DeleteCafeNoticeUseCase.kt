package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.NoticeRepository

class DeleteCafeNoticeUseCase(
    private val noticeRepository: NoticeRepository
) {
    suspend operator fun invoke(cafeId: String, noticeId: String): AppResult<String> {
        return try {
            AppResult.Success(noticeRepository.deleteCafeNotice(cafeId, noticeId))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
