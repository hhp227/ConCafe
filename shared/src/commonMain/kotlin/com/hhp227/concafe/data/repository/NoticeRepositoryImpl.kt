package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.NoticeRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeEventCreate
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeEventUpdate
import com.hhp227.concafe.domain.model.CafeNoticeCreate
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeUpdate
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.repository.NoticeRepository

class NoticeRepositoryImpl(
    private val noticeRemoteDataSource: NoticeRemoteDataSource
) : NoticeRepository {
    override suspend fun getRecentNotices(limit: Int): List<Notice> {
        val safeLimit = limit.coerceAtLeast(1)
        return noticeRemoteDataSource.fetchRecentNotices(safeLimit)
    }

    override suspend fun getCafeNoticePage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeNoticeManagementItem> {
        return noticeRemoteDataSource.fetchCafeNoticePage(
            cafeId = cafeId,
            query = query,
            cursor = cursor,
            pageSize = pageSize
        )
    }

    override suspend fun getCafeEventPage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeEventManagementItem> {
        return noticeRemoteDataSource.fetchCafeEventPage(
            cafeId = cafeId,
            query = query,
            cursor = cursor,
            pageSize = pageSize
        )
    }

    override suspend fun createCafeNotice(input: CafeNoticeCreate): CafeNoticeManagementItem {
        if (input.cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (input.title.isBlank()) throw IllegalArgumentException("notice title is required")
        if (input.content.isBlank()) throw IllegalArgumentException("notice content is required")
        return noticeRemoteDataSource.createCafeNotice(input)
    }

    override suspend fun createCafeEvent(input: CafeEventCreate): CafeEventManagementItem {
        if (input.cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (input.title.isBlank()) throw IllegalArgumentException("event title is required")
        if (input.content.isBlank()) throw IllegalArgumentException("event content is required")
        if (input.imageUrl.isBlank()) throw IllegalArgumentException("event image is required")
        return noticeRemoteDataSource.createCafeEvent(input)
    }

    override suspend fun updateCafeNotice(input: CafeNoticeUpdate): CafeNoticeManagementItem {
        if (input.cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (input.noticeId.isBlank()) throw IllegalArgumentException("noticeId is required")
        if (input.title.isBlank()) throw IllegalArgumentException("notice title is required")
        if (input.content.isBlank()) throw IllegalArgumentException("notice content is required")
        return noticeRemoteDataSource.updateCafeNotice(input)
    }

    override suspend fun updateCafeEvent(input: CafeEventUpdate): CafeEventManagementItem {
        if (input.cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (input.eventId.isBlank()) throw IllegalArgumentException("eventId is required")
        if (input.title.isBlank()) throw IllegalArgumentException("event title is required")
        if (input.content.isBlank()) throw IllegalArgumentException("event content is required")
        if (input.imageUrl.isBlank()) throw IllegalArgumentException("event image is required")
        return noticeRemoteDataSource.updateCafeEvent(input)
    }

    override suspend fun deleteCafeNotice(cafeId: String, noticeId: String): String {
        if (cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (noticeId.isBlank()) throw IllegalArgumentException("noticeId is required")
        return noticeRemoteDataSource.deleteCafeNotice(cafeId = cafeId, noticeId = noticeId)
    }

    override suspend fun deleteCafeEvent(cafeId: String, eventId: String): String {
        if (cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (eventId.isBlank()) throw IllegalArgumentException("eventId is required")
        return noticeRemoteDataSource.deleteCafeEvent(cafeId = cafeId, eventId = eventId)
    }
}
