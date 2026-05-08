package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.repository.NoticeRepository

class GetRecentNoticesUseCase(
    private val noticeRepository: NoticeRepository
) {
    suspend operator fun invoke(limit: Int = RECENT_NOTICE_LIMIT): AppResult<List<Notice>> {
        return try {
            AppResult.Success(noticeRepository.getRecentNotices(limit.coerceAtLeast(1)))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    companion object {
        private const val RECENT_NOTICE_LIMIT = 6
    }
}
