package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.*

interface NoticeRepository {
    suspend fun getRecentNotices(limit: Int): List<Notice>
    suspend fun getCafeNoticePage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeNoticeManagementItem>
    suspend fun getCafeEventPage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeEventManagementItem>
    suspend fun createCafeNotice(input: CafeNoticeCreate): CafeNoticeManagementItem
    suspend fun createCafeEvent(input: CafeEventCreate): CafeEventManagementItem
    suspend fun updateCafeNotice(input: CafeNoticeUpdate): CafeNoticeManagementItem
    suspend fun updateCafeEvent(input: CafeEventUpdate): CafeEventManagementItem
    suspend fun deleteCafeNotice(cafeId: String, noticeId: String): String
    suspend fun deleteCafeEvent(cafeId: String, eventId: String): String
}
