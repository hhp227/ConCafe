package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeEventCreate
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeEventUpdate
import com.hhp227.concafe.domain.model.CafeNoticeCreate
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeUpdate
import com.hhp227.concafe.domain.model.NoticeManagementEvent
import com.hhp227.concafe.domain.model.Notice
import kotlinx.coroutines.flow.Flow

interface NoticeRepository {
    fun observeNoticeManagementEvent(): Flow<NoticeManagementEvent>
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
