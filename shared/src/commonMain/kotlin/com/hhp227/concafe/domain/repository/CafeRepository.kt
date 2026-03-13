package com.hhp227.concafe.domain.repository

import kotlinx.coroutines.flow.Flow
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeDetailEvent
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.CheckInCafeSummary

interface CafeRepository {
    fun observeCafeDetailEvent(): Flow<CafeDetailEvent>

    suspend fun searchCafes(
        query: String?,
        country: String?,
        city: String?,
        sort: CafeSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cafe>

    suspend fun getCafeDetail(cafeId: String): CafeDetail

    fun observeCafeDetail(cafeId: String): Flow<CafeDetail>

    suspend fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail

    suspend fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail

    suspend fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail

    suspend fun isFavorite(userId: String, cafeId: String): Boolean

    suspend fun toggleFavorite(userId: String, cafeId: String): Boolean

    suspend fun getPopularCheckInCafes(limit: Int): List<CheckInCafeSummary>
}
