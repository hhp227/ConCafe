package com.hhp227.concafe.data.source

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

interface CafeRemoteDataSource {
    suspend fun searchCafesRemote(
        query: String?,
        country: String?,
        city: String?,
        sort: CafeSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cafe>

    suspend fun refreshCafeDetail(cafeId: String)

    suspend fun fetchCafeDetail(cafeId: String): CafeDetail

    suspend fun fetchCafeMenuGoods(cafeId: String): CafeMenuGoodsSection

    suspend fun fetchCafeById(cafeId: String): Cafe?

    suspend fun fetchAllCafes(): List<Cafe>

    suspend fun updateCafeInfoRemote(update: CafeInfoUpdate): CafeDetail

    suspend fun upsertCafeMenuGoodsRemote(update: CafeMenuGoodsUpsert): CafeDetail

    suspend fun deleteCafeMenuGoodsRemote(cafeId: String, itemId: String): CafeDetail

    suspend fun refreshFavoriteCafeIds(userId: String)

    suspend fun fetchFavoriteCafeIds(userId: String, limit: Int? = null): List<String>

    suspend fun favoriteCafeRemote(userId: String, cafeId: String)

    suspend fun unfavoriteCafeRemote(userId: String, cafeId: String)

    suspend fun refreshCafeReviews(cafeId: String)

    suspend fun refreshCafeManagementData(userId: String)

    suspend fun fetchOwnedCafeIds(userId: String): Set<String>

    suspend fun fetchPendingCafeOwnerClaims(userId: String): List<CafeManagementData.PendingClaimSummary>

    suspend fun fetchPendingCafeRegistrationClaims(userId: String): List<CafeRegistrationClaim>

    suspend fun fetchCafeTodayCheckInCount(cafeId: String): Int

    suspend fun fetchCafeTodayReviewCount(cafeId: String): Int

    suspend fun fetchCafeCheckInCount(cafeId: String): Int

    suspend fun fetchCafeHomeBannerPreview(cafeId: String): CafeDashboardData.HomeBannerPreview?

    suspend fun fetchNoticeCount(cafeId: String): Int

    suspend fun updateCafeSocialMediaRemote(
        cafeId: String,
        instagramId: String?,
        twitterId: String?,
        tiktokId: String?,
        youtubeId: String?
    )

    suspend fun updateCafeReservationUrlRemote(cafeId: String, reservationUrl: String?)
}
