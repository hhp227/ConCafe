package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.*
import kotlinx.coroutines.flow.Flow

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

    suspend fun toggleFavorite(userId: String, cafeId: String): Boolean

    suspend fun getPopularCheckInCafes(limit: Int): List<CheckInCafeSummary>
}
