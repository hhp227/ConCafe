package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.CheckInCafeSummary
import com.hhp227.concafe.domain.repository.CafeRepository

class CafeRepositoryImpl : CafeRepository {
    override suspend fun searchCafes(
        query: String?,
        country: String?,
        city: String?,
        sort: CafeSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cafe> {
        TODO("Not yet implemented")
    }

    override suspend fun getCafeDetail(cafeId: String): CafeDetail {
        TODO("Not yet implemented")
    }

    override suspend fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail {
        TODO("Not yet implemented")
    }

    override suspend fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCafeMenuGoods(
        cafeId: String,
        itemId: String
    ): CafeDetail {
        TODO("Not yet implemented")
    }

    override suspend fun isFavorite(userId: String, cafeId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toggleFavorite(userId: String, cafeId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getPopularCheckInCafes(limit: Int): List<CheckInCafeSummary> {
        TODO("Not yet implemented")
    }
}