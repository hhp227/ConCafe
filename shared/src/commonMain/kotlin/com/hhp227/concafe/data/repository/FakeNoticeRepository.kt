package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.repository.NoticeRepository

class FakeNoticeRepository(
    private val dataSource: ConCafeDataSource
) : NoticeRepository {
    override suspend fun getRecentNotices(limit: Int): List<Notice> {
        return dataSource.notices.take(limit.coerceAtLeast(1))
    }

    override suspend fun getCafeNoticePage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeNoticeManagementItem> {
        val normalizedQuery = query.trim()
        val filtered = dataSource.cafeNoticeManagementItems
            .asSequence()
            .filter { it.cafeId == cafeId }
            .filter {
                normalizedQuery.isBlank() ||
                    it.title.contains(normalizedQuery, ignoreCase = true) ||
                    it.content.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending { it.createdAt }
            .toList()
        return dataSource.toPaged(filtered, cursor, pageSize)
    }

    override suspend fun getCafeEventPage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeEventManagementItem> {
        val normalizedQuery = query.trim()
        val filtered = dataSource.cafeEventManagementItems
            .asSequence()
            .filter { it.cafeId == cafeId }
            .filter {
                normalizedQuery.isBlank() ||
                    it.title.contains(normalizedQuery, ignoreCase = true) ||
                    it.content.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending { it.startDate }
            .toList()
        return dataSource.toPaged(filtered, cursor, pageSize)
    }
}
