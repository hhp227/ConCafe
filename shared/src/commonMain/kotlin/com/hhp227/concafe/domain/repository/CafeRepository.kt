package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.*

interface CafeRepository {
    suspend fun searchCafes(
        query: String?,
        country: String?,
        city: String?,
        sort: CafeSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cafe>

    suspend fun getCafeDetail(cafeId: String): CafeDetail

    suspend fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail

    suspend fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail

    suspend fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail

    suspend fun isFavorite(userId: String, cafeId: String): Boolean

    suspend fun getFavoriteCafeIds(userId: String): List<String>

    suspend fun getCafesByIds(cafeIds: List<String>): List<Cafe>

    suspend fun toggleFavorite(userId: String, cafeId: String): Boolean

    suspend fun getPopularCheckInCafes(limit: Int): List<CheckInCafeSummary>

    suspend fun updateCafeSocialMedia(
        cafeId: String,
        instagramId: String?,
        twitterId: String?,
        tiktokId: String?,
        youtubeId: String?
    )

    suspend fun updateCafeReservationUrl(cafeId: String, reservationUrl: String?)
}
