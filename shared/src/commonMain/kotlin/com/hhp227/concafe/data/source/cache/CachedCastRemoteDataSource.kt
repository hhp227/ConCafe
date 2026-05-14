package com.hhp227.concafe.data.source.cache

import com.hhp227.concafe.data.source.CastRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastFollowerSnapshot
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.CastUpsert

class CachedCastRemoteDataSource(
    private val upstream: CastRemoteDataSource,
    private val cache: RemoteMemoryCache
) : CastRemoteDataSource by upstream {
    override suspend fun searchCastsRemote(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast> = cache.cacheFirst(
        cacheKey(
            CAST_CACHE_PREFIX,
            "search",
            "query" to query?.trim(),
            "country" to country?.trim(),
            "city" to city?.trim(),
            "sort" to sort.name,
            "cursor" to cursor,
            "pageSize" to pageSize
        )
    ) {
        upstream.searchCastsRemote(query, country, city, sort, cursor, pageSize)
    }

    override suspend fun getHomePopularCastPageRemote(cursor: String?, pageSize: Int): PagedResult<Cast> =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "homePopular", "cursor" to cursor, "pageSize" to pageSize)) {
            upstream.getHomePopularCastPageRemote(cursor, pageSize)
        }

    override suspend fun fetchBirthdayCastsRemote(month: Int, dayOfMonth: Int, limit: Int): List<Cast> =
        cache.cacheFirst(
            cacheKey(CAST_CACHE_PREFIX, "birthday", "month" to month, "dayOfMonth" to dayOfMonth, "limit" to limit)
        ) {
            upstream.fetchBirthdayCastsRemote(month, dayOfMonth, limit)
        }

    override suspend fun refreshCastDetailRemote(castId: String) {
        upstream.refreshCastDetailRemote(castId)
        cache.remove(castDetailKey(castId))
    }

    override suspend fun fetchCastDetail(castId: String): CastDetail = cache.cacheFirst(castDetailKey(castId)) {
        upstream.fetchCastDetail(castId)
    }

    override suspend fun getCafeCastPageRemote(cafeId: String, cursor: String?, pageSize: Int): PagedResult<Cast> =
        cache.cacheFirst(
            cacheKey(CAST_CACHE_PREFIX, "cafePage", "cafeId" to cafeId, "cursor" to cursor, "pageSize" to pageSize)
        ) {
            upstream.getCafeCastPageRemote(cafeId, cursor, pageSize)
        }

    override suspend fun upsertCastRemote(update: CastUpsert): CastDetail {
        val updated = upstream.upsertCastRemote(update)
        invalidateCast(updated.cast.id, updated.cast.cafeId)
        cache.put(castDetailKey(updated.cast.id), updated)
        return updated
    }

    override suspend fun deleteCastRemote(castId: String): Cast {
        val deleted = upstream.deleteCastRemote(castId)
        invalidateCast(castId, deleted.cafeId)
        return deleted
    }

    override suspend fun refreshCastSchedulesRemote(castId: String, fromDate: String, toDate: String) {
        upstream.refreshCastSchedulesRemote(castId, fromDate, toDate)
        cache.remove(castSchedulesKey(castId, fromDate, toDate))
        cache.remove(castScheduleStatusesKey(castId, fromDate, toDate))
    }

    override suspend fun fetchCastSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule> =
        cache.cacheFirst(castSchedulesKey(castId, fromDate, toDate)) {
            upstream.fetchCastSchedules(castId, fromDate, toDate)
        }

    override suspend fun fetchCastScheduleStatuses(
        castId: String,
        fromDate: String,
        toDate: String
    ): Map<String, CastScheduleStatus> = cache.cacheFirst(castScheduleStatusesKey(castId, fromDate, toDate)) {
        upstream.fetchCastScheduleStatuses(castId, fromDate, toDate)
    }

    override suspend fun getWorkingCastIdsByCafeAndDate(cafeId: String, date: String): Set<String> =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "workingIds", "cafeId" to cafeId, "date" to date)) {
            upstream.getWorkingCastIdsByCafeAndDate(cafeId, date)
        }

    override suspend fun getWorkingCastSchedulesByCafeAndDate(cafeId: String, date: String): Map<String, CastSchedule> =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "workingSchedules", "cafeId" to cafeId, "date" to date)) {
            upstream.getWorkingCastSchedulesByCafeAndDate(cafeId, date)
        }

    override suspend fun updateCastScheduleRemote(update: CastScheduleUpdate): CastSchedule? {
        val updated = upstream.updateCastScheduleRemote(update)
        cache.removeByPrefix(cacheKey(CAST_CACHE_PREFIX, "schedules", "castId" to update.castId))
        cache.removeByPrefix(cacheKey(CAST_CACHE_PREFIX, "scheduleStatuses", "castId" to update.castId))
        updated?.let {
            cache.removeByPrefix(cacheKey(CAST_CACHE_PREFIX, "workingIds", "cafeId" to it.cafeId))
            cache.removeByPrefix(cacheKey(CAST_CACHE_PREFIX, "workingSchedules", "cafeId" to it.cafeId))
        }
        return updated
    }

    override suspend fun fetchFollowedCastIds(userId: String): List<String> =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "followedIds", "userId" to userId)) {
            upstream.fetchFollowedCastIds(userId)
        }

    override suspend fun getFollowedCastsRemote(userId: String): List<Cast> =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "followedCasts", "userId" to userId)) {
            upstream.getFollowedCastsRemote(userId)
        }

    override suspend fun refreshCastByLinkedUserId(userId: String): Cast? =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "byLinkedUser", "userId" to userId)) {
            upstream.refreshCastByLinkedUserId(userId) ?: CachedRemoteNull
        }.let { value ->
            if (value === CachedRemoteNull) null else value as Cast
        }

    override suspend fun fetchCastByLinkedUserId(userId: String): Cast? =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "byLinkedUser", "userId" to userId)) {
            upstream.fetchCastByLinkedUserId(userId) ?: CachedRemoteNull
        }.let { value ->
            if (value === CachedRemoteNull) null else value as Cast
        }

    override suspend fun fetchCafeCasts(cafeId: String): List<Cast> =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "cafeCasts", "cafeId" to cafeId)) {
            upstream.fetchCafeCasts(cafeId)
        }

    override suspend fun fetchCafeCastCount(cafeId: String): Int =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "cafeCastCount", "cafeId" to cafeId)) {
            upstream.fetchCafeCastCount(cafeId)
        }

    override suspend fun fetchCastsByIds(castIds: List<String>): List<Cast> {
        val normalizedIds = castIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        return cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "byIds", "castIds" to normalizedIds)) {
            upstream.fetchCastsByIds(normalizedIds)
        }
    }

    override suspend fun fetchAllCasts(): List<Cast> =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "all")) {
            upstream.fetchAllCasts()
        }

    override suspend fun fetchAffiliatedCafeId(userId: String): String? =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "affiliatedCafe", "userId" to userId)) {
            upstream.fetchAffiliatedCafeId(userId) ?: CachedRemoteNull
        }.let { value ->
            if (value === CachedRemoteNull) null else value as String
        }

    override suspend fun setAffiliatedCafeId(userId: String, cafeId: String) {
        upstream.setAffiliatedCafeId(userId, cafeId)
        cache.put(cacheKey(CAST_CACHE_PREFIX, "affiliatedCafe", "userId" to userId), cafeId)
    }

    override suspend fun clearAffiliatedCafeId(userId: String) {
        upstream.clearAffiliatedCafeId(userId)
        cache.remove(cacheKey(CAST_CACHE_PREFIX, "affiliatedCafe", "userId" to userId))
    }

    override suspend fun followCastRemote(userId: String, castId: String) {
        upstream.followCastRemote(userId, castId)
        cache.remove(cacheKey(CAST_CACHE_PREFIX, "followedIds", "userId" to userId))
        cache.remove(cacheKey(CAST_CACHE_PREFIX, "followedCasts", "userId" to userId))
        cache.removeByPrefix(cacheKey(CAST_CACHE_PREFIX, "followers", "castId" to castId))
    }

    override suspend fun unfollowCastRemote(userId: String, castId: String) {
        upstream.unfollowCastRemote(userId, castId)
        cache.remove(cacheKey(CAST_CACHE_PREFIX, "followedIds", "userId" to userId))
        cache.remove(cacheKey(CAST_CACHE_PREFIX, "followedCasts", "userId" to userId))
        cache.removeByPrefix(cacheKey(CAST_CACHE_PREFIX, "followers", "castId" to castId))
    }

    override suspend fun fetchFollowerUserIds(castId: String): List<String> =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "followers", "castId" to castId)) {
            upstream.fetchFollowerUserIds(castId)
        }

    override suspend fun getCastFollowerSnapshots(castId: String): List<CastFollowerSnapshot> =
        cache.cacheFirst(cacheKey(CAST_CACHE_PREFIX, "followerSnapshots", "castId" to castId)) {
            upstream.getCastFollowerSnapshots(castId)
        }

    private suspend fun invalidateCast(castId: String, cafeId: String) {
        cache.remove(castDetailKey(castId))
        cache.removeByPrefix("$CAST_CACHE_PREFIX.search")
        cache.removeByPrefix("$CAST_CACHE_PREFIX.homePopular")
        cache.removeByPrefix("$CAST_CACHE_PREFIX.birthday")
        cache.removeByPrefix(cacheKey(CAST_CACHE_PREFIX, "cafePage", "cafeId" to cafeId))
        cache.remove(cacheKey(CAST_CACHE_PREFIX, "cafeCasts", "cafeId" to cafeId))
        cache.remove(cacheKey(CAST_CACHE_PREFIX, "cafeCastCount", "cafeId" to cafeId))
        cache.remove(cacheKey(CAST_CACHE_PREFIX, "all"))
    }

    private fun castDetailKey(castId: String): String = cacheKey(CAST_CACHE_PREFIX, "detail", "castId" to castId)

    private fun castSchedulesKey(castId: String, fromDate: String, toDate: String): String =
        cacheKey(CAST_CACHE_PREFIX, "schedules", "castId" to castId, "fromDate" to fromDate, "toDate" to toDate)

    private fun castScheduleStatusesKey(castId: String, fromDate: String, toDate: String): String =
        cacheKey(CAST_CACHE_PREFIX, "scheduleStatuses", "castId" to castId, "fromDate" to fromDate, "toDate" to toDate)
}

fun CastRemoteDataSource.withLocalCache(cache: RemoteMemoryCache): CastRemoteDataSource =
    CachedCastRemoteDataSource(this, cache)

private const val CAST_CACHE_PREFIX = "cast"
