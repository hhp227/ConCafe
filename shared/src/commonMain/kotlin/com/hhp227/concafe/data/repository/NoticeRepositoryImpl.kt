package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.NoticeDataSource
import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
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
import kotlinx.datetime.Clock

class NoticeRepositoryImpl(
    private val noticeDataSource: NoticeDataSource,
    private val cafeDataSource: CafeDataSource,
    private val pagingDataSource: PagingDataSource
) : NoticeRepository {
    override suspend fun getRecentNotices(limit: Int): List<Notice> {
        val safeLimit = limit.coerceAtLeast(1)
        val firestoreDataSource = noticeDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null && safeLimit <= REMOTE_NOTICE_PAGE_LIMIT) {
            val remote = runCatching {
                firestoreDataSource.getRecentNoticesRemote(safeLimit)
            }.getOrNull()

            if (remote != null) {
                return remote
            }
        }
        return noticeDataSource.notices.take(safeLimit)
    }

    override suspend fun getCafeNoticePage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeNoticeManagementItem> {
        val firestoreDataSource = noticeDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null && pageSize <= REMOTE_NOTICE_PAGE_LIMIT) {
            val remoteResult = runCatching {
                firestoreDataSource.getCafeNoticePageRemote(
                    cafeId = cafeId,
                    query = query,
                    cursor = cursor,
                    pageSize = pageSize
                )
            }.getOrNull()

            if (remoteResult != null) {
                return remoteResult
            }
        }
        val hasCachedNotices = noticeDataSource.cafeNoticeManagementItems.any { item -> item.cafeId == cafeId }
        val shouldRefresh = cursor == null && !hasCachedNotices

        if (shouldRefresh) {
            (noticeDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
                runCatching {
                    firestoreDataSource.refreshCafeNoticeEventManagement(cafeId)
                }
            }
        }
        val normalizedQuery = query.trim()
        val filtered = noticeDataSource.cafeNoticeManagementItems
            .asSequence()
            .filter { it.cafeId == cafeId }
            .filter {
                normalizedQuery.isBlank() ||
                    it.title.contains(normalizedQuery, ignoreCase = true) ||
                    it.content.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending { it.createdAt }
            .toList()
        return pagingDataSource.toPaged(filtered, cursor, pageSize)
    }

    override suspend fun getCafeEventPage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeEventManagementItem> {
        val firestoreDataSource = noticeDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null && pageSize <= REMOTE_NOTICE_PAGE_LIMIT) {
            val remoteResult = runCatching {
                firestoreDataSource.getCafeEventPageRemote(
                    cafeId = cafeId,
                    query = query,
                    cursor = cursor,
                    pageSize = pageSize
                )
            }.getOrNull()

            if (remoteResult != null) {
                return remoteResult
            }
        }
        val hasCachedEvents = noticeDataSource.cafeEventManagementItems.any { item -> item.cafeId == cafeId }
        val shouldRefresh = cursor == null && !hasCachedEvents

        if (shouldRefresh) {
            (noticeDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
                runCatching {
                    firestoreDataSource.refreshCafeNoticeEventManagement(cafeId)
                }
            }
        }
        val normalizedQuery = query.trim()
        val filtered = noticeDataSource.cafeEventManagementItems
            .asSequence()
            .filter { it.cafeId == cafeId }
            .filter {
                normalizedQuery.isBlank() ||
                    it.title.contains(normalizedQuery, ignoreCase = true) ||
                    it.content.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending { it.startDate }
            .toList()
        return pagingDataSource.toPaged(filtered, cursor, pageSize)
    }

    override suspend fun createCafeNotice(input: CafeNoticeCreate): CafeNoticeManagementItem {
        if (input.cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (input.title.isBlank()) throw IllegalArgumentException("notice title is required")
        if (input.content.isBlank()) throw IllegalArgumentException("notice content is required")
        (noticeDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            return firestoreDataSource.createCafeNoticeRemote(input)
        }

        val createdAt = nowIsoUtc()
        val item = CafeNoticeManagementItem(
            id = nextEntityId("notice-management"),
            cafeId = input.cafeId,
            title = input.title.trim(),
            content = input.content.trim(),
            createdAt = createdAt,
            displayDate = createdAt.take(10).replace("-", "."),
            isPinned = input.isPinned,
            statusLabel = if (input.reservedAt.isNullOrBlank()) "게시 중" else "임시 저장",
            statusAccent = if (input.reservedAt.isNullOrBlank()) NoticeStatusAccent.PUBLISHED else NoticeStatusAccent.DRAFT
        )
        noticeDataSource.cafeNoticeManagementItems.add(0, item)

        val cafeName = cafeDataSource.cafes.firstOrNull { it.id == input.cafeId }?.name.orEmpty()
        noticeDataSource.notices.add(
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
        (noticeDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            return firestoreDataSource.createCafeEventRemote(input)
        }

        val periodText = input.periodText?.takeIf { it.isNotBlank() } ?: "게시 일정 선택 필요"
        val startDate = periodText.substringBefore(" - ", missingDelimiterValue = periodText)
        val endDate = periodText.substringAfter(" - ", missingDelimiterValue = startDate)
        val item = CafeEventManagementItem(
            id = nextEntityId("event-management"),
            cafeId = input.cafeId,
            title = input.title.trim(),
            content = input.content.trim(),
            imageUrl = input.imageUrl,
            startDate = startDate,
            endDate = endDate,
            statusLabel = if (input.periodText.isNullOrBlank()) "진행 예정" else "진행 중",
            isDimmed = false
        )
        noticeDataSource.cafeEventManagementItems.add(0, item)
        return item
    }

    override suspend fun updateCafeNotice(input: CafeNoticeUpdate): CafeNoticeManagementItem {
        if (input.cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (input.noticeId.isBlank()) throw IllegalArgumentException("noticeId is required")
        if (input.title.isBlank()) throw IllegalArgumentException("notice title is required")
        if (input.content.isBlank()) throw IllegalArgumentException("notice content is required")
        (noticeDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            return firestoreDataSource.updateCafeNoticeRemote(input)
        }

        val noticeIndex = noticeDataSource.cafeNoticeManagementItems.indexOfFirst {
            it.id == input.noticeId && it.cafeId == input.cafeId
        }
        if (noticeIndex == -1) throw NoSuchElementException()

        val original = noticeDataSource.cafeNoticeManagementItems[noticeIndex]
        val updated = original.copy(
            title = input.title.trim(),
            content = input.content.trim(),
            isPinned = input.isPinned,
            statusLabel = if (input.reservedAt.isNullOrBlank()) "게시 중" else "임시 저장",
            statusAccent = if (input.reservedAt.isNullOrBlank()) NoticeStatusAccent.PUBLISHED else NoticeStatusAccent.DRAFT
        )
        noticeDataSource.cafeNoticeManagementItems[noticeIndex] = updated

        val recentNoticeIndex = noticeDataSource.notices.indexOfFirst { it.id == input.noticeId }
        if (recentNoticeIndex != -1) {
            val recent = noticeDataSource.notices[recentNoticeIndex]
            noticeDataSource.notices[recentNoticeIndex] = recent.copy(
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
        (noticeDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            return firestoreDataSource.updateCafeEventRemote(input)
        }

        val eventIndex = noticeDataSource.cafeEventManagementItems.indexOfFirst {
            it.id == input.eventId && it.cafeId == input.cafeId
        }
        if (eventIndex == -1) throw NoSuchElementException()

        val original = noticeDataSource.cafeEventManagementItems[eventIndex]
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
        noticeDataSource.cafeEventManagementItems[eventIndex] = updated
        return updated
    }

    override suspend fun deleteCafeNotice(cafeId: String, noticeId: String): String {
        if (cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (noticeId.isBlank()) throw IllegalArgumentException("noticeId is required")
        (noticeDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            firestoreDataSource.deleteCafeNoticeRemote(cafeId = cafeId, noticeId = noticeId)
            return noticeId
        }

        val noticeIndex = noticeDataSource.cafeNoticeManagementItems.indexOfFirst {
            it.id == noticeId && it.cafeId == cafeId
        }
        if (noticeIndex == -1) throw NoSuchElementException()
        noticeDataSource.cafeNoticeManagementItems.removeAt(noticeIndex)

        val recentNoticeIndex = noticeDataSource.notices.indexOfFirst { it.id == noticeId && it.cafeId == cafeId }
        if (recentNoticeIndex != -1) {
            noticeDataSource.notices.removeAt(recentNoticeIndex)
        }
        return noticeId
    }

    override suspend fun deleteCafeEvent(cafeId: String, eventId: String): String {
        if (cafeId.isBlank()) throw IllegalArgumentException("cafeId is required")
        if (eventId.isBlank()) throw IllegalArgumentException("eventId is required")
        (noticeDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            firestoreDataSource.deleteCafeEventRemote(cafeId = cafeId, eventId = eventId)
            return eventId
        }

        val eventIndex = noticeDataSource.cafeEventManagementItems.indexOfFirst {
            it.id == eventId && it.cafeId == cafeId
        }
        if (eventIndex == -1) throw NoSuchElementException()
        noticeDataSource.cafeEventManagementItems.removeAt(eventIndex)
        return eventId
    }
}

private const val REMOTE_NOTICE_PAGE_LIMIT = 100

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}

private fun nowIsoUtc(): String {
    return Clock.System.now().toString()
}
