package com.hhp227.concafe.data.source.cache

import com.hhp227.concafe.data.source.NoticeRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeEventCreate
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeEventUpdate

class CachedNoticeRemoteDataSource(
    private val upstream: NoticeRemoteDataSource,
    private val cache: RemoteMemoryCache
) : NoticeRemoteDataSource by upstream {
    override suspend fun fetchCafeEventPage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeEventManagementItem> = cache.cacheFirst(
        cacheKey(
            CAFE_EVENT_CACHE_PREFIX,
            "page",
            "cafeId" to cafeId,
            "query" to query.trim(),
            "cursor" to cursor,
            "pageSize" to pageSize
        )
    ) {
        upstream.fetchCafeEventPage(cafeId, query, cursor, pageSize)
    }

    override suspend fun fetchHomeCafeEventPage(
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeEventManagementItem> = cache.cacheFirst(
        cacheKey(CAFE_EVENT_CACHE_PREFIX, "homePage", "cursor" to cursor, "pageSize" to pageSize)
    ) {
        upstream.fetchHomeCafeEventPage(cursor, pageSize)
    }

    override suspend fun createCafeEvent(input: CafeEventCreate): CafeEventManagementItem {
        val created = upstream.createCafeEvent(input)
        invalidateCafeEvents(input.cafeId)
        return created
    }

    override suspend fun updateCafeEvent(input: CafeEventUpdate): CafeEventManagementItem {
        val updated = upstream.updateCafeEvent(input)
        invalidateCafeEvents(input.cafeId)
        cache.removeByPrefix(cafeEventLikePrefix(input.cafeId, input.eventId))
        return updated
    }

    override suspend fun deleteCafeEvent(cafeId: String, eventId: String): String {
        val deletedEventId = upstream.deleteCafeEvent(cafeId, eventId)
        invalidateCafeEvents(cafeId)
        cache.removeByPrefix(cafeEventLikePrefix(cafeId, eventId))
        return deletedEventId
    }

    override suspend fun isCafeEventLikedByUser(cafeId: String, eventId: String, userId: String): Boolean =
        cache.cacheFirst(cafeEventLikeKey(cafeId, eventId, userId)) {
            upstream.isCafeEventLikedByUser(cafeId, eventId, userId)
        }

    override suspend fun toggleCafeEventLike(cafeId: String, eventId: String, userId: String): Boolean {
        val isLiked = upstream.toggleCafeEventLike(cafeId, eventId, userId)
        cache.put(cafeEventLikeKey(cafeId, eventId, userId), isLiked)
        invalidateCafeEvents(cafeId)
        return isLiked
    }

    private suspend fun invalidateCafeEvents(cafeId: String) {
        cache.removeByPrefix(cacheKey(CAFE_EVENT_CACHE_PREFIX, "page", "cafeId" to cafeId))
        cache.removeByPrefix("$CAFE_EVENT_CACHE_PREFIX.homePage")
    }

    private fun cafeEventLikeKey(cafeId: String, eventId: String, userId: String): String =
        cacheKey(CAFE_EVENT_CACHE_PREFIX, "liked", "cafeId" to cafeId, "eventId" to eventId, "userId" to userId)

    private fun cafeEventLikePrefix(cafeId: String, eventId: String): String =
        cacheKey(CAFE_EVENT_CACHE_PREFIX, "liked", "cafeId" to cafeId, "eventId" to eventId)
}

fun NoticeRemoteDataSource.withLocalCache(cache: RemoteMemoryCache): NoticeRemoteDataSource =
    CachedNoticeRemoteDataSource(this, cache)

private const val CAFE_EVENT_CACHE_PREFIX = "cafeEvent"
