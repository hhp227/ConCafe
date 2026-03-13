package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.repository.NoticeRepository

class GetCafeNoticePageUseCase(
    private val noticeRepository: NoticeRepository
) {
    suspend operator fun invoke(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): AppResult<PagedResult<CafeNoticeManagementItem>> {
        return try {
            AppResult.Success(noticeRepository.getCafeNoticePage(cafeId, query, cursor, pageSize))
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 15
    }
}
