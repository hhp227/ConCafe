package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeEventCreate
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeEventUpdate
import com.hhp227.concafe.domain.model.CafeNoticeCreate
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeUpdate
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.repository.NoticeRepository

class NoticeRepositoryImpl : NoticeRepository {
    override suspend fun getRecentNotices(limit: Int): List<Notice> {
        TODO("Not yet implemented")
    }

    override suspend fun getCafeNoticePage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeNoticeManagementItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getCafeEventPage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeEventManagementItem> {
        TODO("Not yet implemented")
    }

    override suspend fun createCafeNotice(input: CafeNoticeCreate): CafeNoticeManagementItem {
        TODO("Not yet implemented")
    }

    override suspend fun createCafeEvent(input: CafeEventCreate): CafeEventManagementItem {
        TODO("Not yet implemented")
    }

    override suspend fun updateCafeNotice(input: CafeNoticeUpdate): CafeNoticeManagementItem {
        TODO("Not yet implemented")
    }

    override suspend fun updateCafeEvent(input: CafeEventUpdate): CafeEventManagementItem {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCafeNotice(cafeId: String, noticeId: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCafeEvent(cafeId: String, eventId: String): String {
        TODO("Not yet implemented")
    }
}