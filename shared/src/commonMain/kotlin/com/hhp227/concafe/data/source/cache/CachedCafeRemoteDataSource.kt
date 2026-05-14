package com.hhp227.concafe.data.source.cache

import com.hhp227.concafe.data.source.CafeRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.CafeMenuGoodsSection
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeRegistrationClaim
import com.hhp227.concafe.domain.model.CafeSort

class CachedCafeRemoteDataSource(
    private val upstream: CafeRemoteDataSource,
    private val cache: RemoteMemoryCache
) : CafeRemoteDataSource by upstream {
    override suspend fun searchCafesRemote(
        query: String?,
        country: String?,
        city: String?,
        sort: CafeSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cafe> = cache.cacheFirst(
        cacheKey(
            CAFE_CACHE_PREFIX,
            "search",
            "query" to query?.trim(),
            "country" to country?.trim(),
            "city" to city?.trim(),
            "sort" to sort.name,
            "cursor" to cursor,
            "pageSize" to pageSize
        )
    ) {
        upstream.searchCafesRemote(query, country, city, sort, cursor, pageSize)
    }

    override suspend fun refreshCafeDetail(cafeId: String) {
        upstream.refreshCafeDetail(cafeId)
        cache.remove(cafeDetailKey(cafeId))
    }

    override suspend fun fetchCafeDetail(cafeId: String): CafeDetail = cache.cacheFirst(cafeDetailKey(cafeId)) {
        upstream.fetchCafeDetail(cafeId)
    }

    override suspend fun fetchCafeMenuGoods(cafeId: String): CafeMenuGoodsSection =
        cache.cacheFirst(cafeMenuGoodsKey(cafeId)) {
            upstream.fetchCafeMenuGoods(cafeId)
        }

    override suspend fun fetchCafeById(cafeId: String): Cafe? = cache.cacheFirst(cafeByIdKey(cafeId)) {
        upstream.fetchCafeById(cafeId) ?: CachedRemoteNull
    }.let { value ->
        if (value === CachedRemoteNull) null else value as Cafe
    }

    override suspend fun fetchAllCafes(): List<Cafe> = cache.cacheFirst(cacheKey(CAFE_CACHE_PREFIX, "all")) {
        upstream.fetchAllCafes()
    }

    override suspend fun updateCafeInfoRemote(update: CafeInfoUpdate): CafeDetail {
        val updated = upstream.updateCafeInfoRemote(update)
        invalidateCafe(update.cafeId)
        cache.put(cafeDetailKey(update.cafeId), updated)
        return updated
    }

    override suspend fun upsertCafeMenuGoodsRemote(update: CafeMenuGoodsUpsert): CafeDetail {
        val updated = upstream.upsertCafeMenuGoodsRemote(update)
        invalidateCafe(update.cafeId)
        cache.put(cafeDetailKey(update.cafeId), updated)
        return updated
    }

    override suspend fun deleteCafeMenuGoodsRemote(cafeId: String, itemId: String): CafeDetail {
        val updated = upstream.deleteCafeMenuGoodsRemote(cafeId, itemId)
        invalidateCafe(cafeId)
        cache.put(cafeDetailKey(cafeId), updated)
        return updated
    }

    override suspend fun favoriteCafeRemote(userId: String, cafeId: String) {
        upstream.favoriteCafeRemote(userId, cafeId)
        cache.removeByPrefix(cacheKey(CAFE_CACHE_PREFIX, "favoriteIds", "userId" to userId))
    }

    override suspend fun unfavoriteCafeRemote(userId: String, cafeId: String) {
        upstream.unfavoriteCafeRemote(userId, cafeId)
        cache.removeByPrefix(cacheKey(CAFE_CACHE_PREFIX, "favoriteIds", "userId" to userId))
    }

    override suspend fun fetchFavoriteCafeIds(userId: String, limit: Int?): List<String> = cache.cacheFirst(
        cacheKey(CAFE_CACHE_PREFIX, "favoriteIds", "userId" to userId, "limit" to limit)
    ) {
        upstream.fetchFavoriteCafeIds(userId, limit)
    }

    override suspend fun fetchOwnedCafeIds(userId: String): Set<String> = cache.cacheFirst(
        cacheKey(CAFE_CACHE_PREFIX, "ownedIds", "userId" to userId)
    ) {
        upstream.fetchOwnedCafeIds(userId)
    }

    override suspend fun fetchPendingCafeOwnerClaims(userId: String): List<CafeManagementData.PendingClaimSummary> =
        cache.cacheFirst(cacheKey(CAFE_CACHE_PREFIX, "pendingOwnerClaims", "userId" to userId)) {
            upstream.fetchPendingCafeOwnerClaims(userId)
        }

    override suspend fun fetchPendingCafeRegistrationClaims(userId: String): List<CafeRegistrationClaim> =
        cache.cacheFirst(cacheKey(CAFE_CACHE_PREFIX, "pendingRegistrationClaims", "userId" to userId)) {
            upstream.fetchPendingCafeRegistrationClaims(userId)
        }

    override suspend fun fetchCafeTodayCheckInCount(cafeId: String): Int =
        cache.cacheFirst(cacheKey(CAFE_CACHE_PREFIX, "todayCheckInCount", "cafeId" to cafeId)) {
            upstream.fetchCafeTodayCheckInCount(cafeId)
        }

    override suspend fun fetchCafeTodayReviewCount(cafeId: String): Int =
        cache.cacheFirst(cacheKey(CAFE_CACHE_PREFIX, "todayReviewCount", "cafeId" to cafeId)) {
            upstream.fetchCafeTodayReviewCount(cafeId)
        }

    override suspend fun fetchCafeCheckInCount(cafeId: String): Int =
        cache.cacheFirst(cacheKey(CAFE_CACHE_PREFIX, "checkInCount", "cafeId" to cafeId)) {
            upstream.fetchCafeCheckInCount(cafeId)
        }

    override suspend fun fetchCafeHomeBannerPreview(cafeId: String): CafeDashboardData.HomeBannerPreview? =
        cache.cacheFirst(cacheKey(CAFE_CACHE_PREFIX, "homeBannerPreview", "cafeId" to cafeId)) {
            upstream.fetchCafeHomeBannerPreview(cafeId) ?: CachedRemoteNull
        }.let { value ->
            if (value === CachedRemoteNull) null else value as CafeDashboardData.HomeBannerPreview
        }

    override suspend fun fetchNoticeCount(cafeId: String): Int =
        cache.cacheFirst(cacheKey(CAFE_CACHE_PREFIX, "noticeCount", "cafeId" to cafeId)) {
            upstream.fetchNoticeCount(cafeId)
        }

    override suspend fun updateCafeSocialMediaRemote(
        cafeId: String,
        instagramId: String?,
        twitterId: String?,
        tiktokId: String?,
        youtubeId: String?
    ) {
        upstream.updateCafeSocialMediaRemote(cafeId, instagramId, twitterId, tiktokId, youtubeId)
        invalidateCafe(cafeId)
    }

    override suspend fun updateCafeReservationUrlRemote(cafeId: String, reservationUrl: String?) {
        upstream.updateCafeReservationUrlRemote(cafeId, reservationUrl)
        invalidateCafe(cafeId)
    }

    override suspend fun updateCafeTableCountsRemote(cafeId: String, current: Int, total: Int) {
        upstream.updateCafeTableCountsRemote(cafeId, current, total)
        invalidateCafe(cafeId)
    }

    private suspend fun invalidateCafe(cafeId: String) {
        cache.remove(cafeDetailKey(cafeId))
        cache.remove(cafeMenuGoodsKey(cafeId))
        cache.remove(cafeByIdKey(cafeId))
        cache.removeByPrefix("$CAFE_CACHE_PREFIX.search")
        cache.remove(cacheKey(CAFE_CACHE_PREFIX, "all"))
    }

    private fun cafeDetailKey(cafeId: String): String = cacheKey(CAFE_CACHE_PREFIX, "detail", "cafeId" to cafeId)

    private fun cafeMenuGoodsKey(cafeId: String): String = cacheKey(CAFE_CACHE_PREFIX, "menuGoods", "cafeId" to cafeId)

    private fun cafeByIdKey(cafeId: String): String = cacheKey(CAFE_CACHE_PREFIX, "byId", "cafeId" to cafeId)
}

fun CafeRemoteDataSource.withLocalCache(cache: RemoteMemoryCache): CafeRemoteDataSource =
    CachedCafeRemoteDataSource(this, cache)

private const val CAFE_CACHE_PREFIX = "cafe"
