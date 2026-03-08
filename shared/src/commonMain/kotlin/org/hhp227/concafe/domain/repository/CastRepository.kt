package org.hhp227.concafe.domain.repository

import org.hhp227.concafe.domain.common.PagedResult
import org.hhp227.concafe.domain.model.Cast
import org.hhp227.concafe.domain.model.CastDetail
import org.hhp227.concafe.domain.model.CastSchedule
import org.hhp227.concafe.domain.model.CastSort

interface CastRepository {
    suspend fun searchCasts(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast>

    suspend fun getCastDetail(castId: String): CastDetail

    suspend fun getCastSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule>

    suspend fun isFollowing(userId: String, castId: String): Boolean

    suspend fun followCast(userId: String, castId: String)

    suspend fun unfollowCast(userId: String, castId: String)
}
