package org.hhp227.concafe.domain.repository

import org.hhp227.concafe.domain.common.PagedResult
import org.hhp227.concafe.domain.model.Cafe
import org.hhp227.concafe.domain.model.CafeDetail
import org.hhp227.concafe.domain.model.CafeSort
import org.hhp227.concafe.domain.model.CheckInCafeSummary

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

    suspend fun isFavorite(userId: String, cafeId: String): Boolean

    suspend fun toggleFavorite(userId: String, cafeId: String): Boolean

    suspend fun getPopularCheckInCafes(limit: Int): List<CheckInCafeSummary>
}
