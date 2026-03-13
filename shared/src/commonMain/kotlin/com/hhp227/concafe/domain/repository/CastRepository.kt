package com.hhp227.concafe.domain.repository

import kotlinx.coroutines.flow.Flow
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.domain.model.CafeDetailCast
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastEvent
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.CheckInCastSummary

interface CastRepository {
    fun observeCastEvent(): Flow<CastEvent>

    suspend fun searchCasts(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast>

    suspend fun getCastDetail(castId: String): CastDetail

    suspend fun getCafeCastPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeCastPreview>

    suspend fun getCafeCastListPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeDetailCast>

    fun observeCafeCastVersion(cafeId: String): Flow<Int>

    fun observeCastVersion(castId: String): Flow<Int>

    suspend fun upsertCast(update: CastUpsert): CastDetail

    suspend fun getCastSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule>

    suspend fun isFollowing(userId: String, castId: String): Boolean

    suspend fun followCast(userId: String, castId: String)

    suspend fun unfollowCast(userId: String, castId: String)

    suspend fun getFollowerUserIds(castId: String): List<String>

    suspend fun getPopularTodayCasts(limit: Int): List<CheckInCastSummary>
}
