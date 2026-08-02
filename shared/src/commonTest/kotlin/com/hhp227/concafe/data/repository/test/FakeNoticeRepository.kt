package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeEventCreate
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeEventUpdate
import com.hhp227.concafe.domain.model.CafeNoticeCreate
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeUpdate
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.model.NoticeStatusAccent
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

    private val likedEventKeysByUserId = mutableMapOf<String, MutableSet<String>>()

    override suspend fun isCafeEventLikedByUser(cafeId: String, eventId: String, userId: String): Boolean {
        return likedEventKeysByUserId[userId]?.contains("$cafeId:$eventId") == true
    }

    override suspend fun toggleCafeEventLike(cafeId: String, eventId: String, userId: String): Boolean {
        val likedKeys = likedEventKeysByUserId.getOrPut(userId) { mutableSetOf() }
        val eventKey = "$cafeId:$eventId"

        return if (likedKeys.contains(eventKey)) {
            likedKeys.remove(eventKey)
            false
        } else {
            likedKeys.add(eventKey)
            true
        }
    }

    override suspend fun getHomeCafeEventPage(
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeEventManagementItem> {
        val sorted = dataSource.cafeEventManagementItems.sortedByDescending { it.startDate }
        return dataSource.toPaged(sorted, cursor, pageSize)
    }

    override suspend fun createCafeNotice(input: CafeNoticeCreate): CafeNoticeManagementItem {
        if (input.cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (input.title.isBlank()) throw IllegalArgumentException("notice title is required")
        if (input.content.isBlank()) throw IllegalArgumentException("notice content is required")

        val nextIndex = dataSource.cafeNoticeManagementItems.size + 1
        val createdAt = "2026-03-13T23:59:${(nextIndex % 60).toString().padStart(2, '0')}Z"
        val item = CafeNoticeManagementItem(
            id = "notice-management-$nextIndex",
            cafeId = input.cafeId,
            title = input.title.trim(),
            content = input.content.trim(),
            createdAt = createdAt,
            displayDate = createdAt.take(10).replace("-", "."),
            isPinned = input.isPinned,
            statusLabel = if (input.reservedAt.isNullOrBlank()) "게시 중" else "임시 저장",
            statusAccent = if (input.reservedAt.isNullOrBlank()) NoticeStatusAccent.PUBLISHED else NoticeStatusAccent.DRAFT
        )
        dataSource.cafeNoticeManagementItems.add(0, item)

        val cafeName = dataSource.cafes.firstOrNull { it.id == input.cafeId }?.name.orEmpty()
        dataSource.notices.add(
            0,
            Notice(
                id = item.id,
                cafeId = item.cafeId,
                cafeName = cafeName,
                title = item.title,
                content = item.content,
                createdAt = item.createdAt,
                relativeTime = "방금 전"
            )
        )
        return item
    }

    override suspend fun createCafeEvent(input: CafeEventCreate): CafeEventManagementItem {
        if (input.cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (input.title.isBlank()) throw IllegalArgumentException("event title is required")
        if (input.content.isBlank()) throw IllegalArgumentException("event content is required")
        if (input.imageUrl.isBlank()) throw IllegalArgumentException("event image is required")

        val nextIndex = dataSource.cafeEventManagementItems.size + 1
        val periodText = input.periodText?.takeIf { it.isNotBlank() } ?: "게시 일정 선택 필요"
        val startDate = periodText.substringBefore(" - ", missingDelimiterValue = periodText)
        val endDate = periodText.substringAfter(" - ", missingDelimiterValue = startDate)
        val item = CafeEventManagementItem(
            id = "event-management-$nextIndex",
            cafeId = input.cafeId,
            title = input.title.trim(),
            content = input.content.trim(),
            imageUrl = input.imageUrl,
            startDate = startDate,
            endDate = endDate,
            statusLabel = if (input.periodText.isNullOrBlank()) "진행 예정" else "진행 중",
            isDimmed = false
        )
        dataSource.cafeEventManagementItems.add(0, item)
        return item
    }

    override suspend fun updateCafeNotice(input: CafeNoticeUpdate): CafeNoticeManagementItem {
        if (input.cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (input.noticeId.isBlank()) throw IllegalArgumentException("noticeId is required")
        if (input.title.isBlank()) throw IllegalArgumentException("notice title is required")
        if (input.content.isBlank()) throw IllegalArgumentException("notice content is required")

        val noticeIndex = dataSource.cafeNoticeManagementItems.indexOfFirst {
            it.id == input.noticeId && it.cafeId == input.cafeId
        }
        if (noticeIndex == -1) throw NoSuchElementException()

        val original = dataSource.cafeNoticeManagementItems[noticeIndex]
        val updated = original.copy(
            title = input.title.trim(),
            content = input.content.trim(),
            isPinned = input.isPinned,
            statusLabel = if (input.reservedAt.isNullOrBlank()) "게시 중" else "임시 저장",
            statusAccent = if (input.reservedAt.isNullOrBlank()) NoticeStatusAccent.PUBLISHED else NoticeStatusAccent.DRAFT
        )
        dataSource.cafeNoticeManagementItems[noticeIndex] = updated

        val recentNoticeIndex = dataSource.notices.indexOfFirst { it.id == input.noticeId }
        if (recentNoticeIndex != -1) {
            val recent = dataSource.notices[recentNoticeIndex]
            dataSource.notices[recentNoticeIndex] = recent.copy(
                title = updated.title,
                content = updated.content
            )
        }
        return updated
    }

    override suspend fun updateCafeEvent(input: CafeEventUpdate): CafeEventManagementItem {
        if (input.cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (input.eventId.isBlank()) throw IllegalArgumentException("eventId is required")
        if (input.title.isBlank()) throw IllegalArgumentException("event title is required")
        if (input.content.isBlank()) throw IllegalArgumentException("event content is required")
        if (input.imageUrl.isBlank()) throw IllegalArgumentException("event image is required")

        val eventIndex = dataSource.cafeEventManagementItems.indexOfFirst {
            it.id == input.eventId && it.cafeId == input.cafeId
        }
        if (eventIndex == -1) throw NoSuchElementException()

        val original = dataSource.cafeEventManagementItems[eventIndex]
        val periodText = input.periodText?.takeIf { it.isNotBlank() } ?: original.periodText
        val startDate = periodText.substringBefore(" - ", missingDelimiterValue = periodText)
        val endDate = periodText.substringAfter(" - ", missingDelimiterValue = startDate)
        val updated = original.copy(
            title = input.title.trim(),
            content = input.content.trim(),
            imageUrl = input.imageUrl,
            startDate = startDate,
            endDate = endDate,
            statusLabel = if (input.periodText.isNullOrBlank()) original.statusLabel else "진행 중"
        )
        dataSource.cafeEventManagementItems[eventIndex] = updated
        return updated
    }

    override suspend fun deleteCafeNotice(cafeId: String, noticeId: String): String {
        if (cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (noticeId.isBlank()) throw IllegalArgumentException("noticeId is required")

        val noticeIndex = dataSource.cafeNoticeManagementItems.indexOfFirst {
            it.id == noticeId && it.cafeId == cafeId
        }
        if (noticeIndex == -1) throw NoSuchElementException()
        dataSource.cafeNoticeManagementItems.removeAt(noticeIndex)

        val recentNoticeIndex = dataSource.notices.indexOfFirst { it.id == noticeId && it.cafeId == cafeId }
        if (recentNoticeIndex != -1) {
            dataSource.notices.removeAt(recentNoticeIndex)
        }
        return noticeId
    }

    override suspend fun deleteCafeEvent(cafeId: String, eventId: String): String {
        if (cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (eventId.isBlank()) throw IllegalArgumentException("eventId is required")

        val eventIndex = dataSource.cafeEventManagementItems.indexOfFirst {
            it.id == eventId && it.cafeId == cafeId
        }
        if (eventIndex == -1) throw NoSuchElementException()
        dataSource.cafeEventManagementItems.removeAt(eventIndex)
        return eventId
    }
}