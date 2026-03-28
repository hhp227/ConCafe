package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.*

interface CastRepository {
    suspend fun searchCasts(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast>

    suspend fun getHomePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast>

    suspend fun getBirthdayCasts(
        month: Int,
        dayOfMonth: Int,
        limit: Int
    ): List<Cast>

    suspend fun getCastDetail(castId: String): CastDetail

    suspend fun getCafeCastPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeCastPreview>

    suspend fun getCafeCastListPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeDetailCast>

    suspend fun upsertCast(update: CastUpsert): CastDetail

    suspend fun deleteCast(castId: String): Cast

    suspend fun getCastSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule>

    suspend fun getCastScheduleStatuses(
        castId: String,
        fromDate: String,
        toDate: String
    ): Map<String, CastScheduleStatus>

    suspend fun getWorkingCastIdsByCafeAndDate(cafeId: String, date: String): Set<String>

    suspend fun updateCastSchedule(update: CastScheduleUpdate): CastSchedule?

    suspend fun isFollowing(userId: String, castId: String): Boolean

    suspend fun getFollowedCastIds(userId: String): List<String>

    suspend fun getFollowedCasts(userId: String): List<Cast>

    suspend fun getCastsByIds(castIds: List<String>): List<Cast>

    suspend fun getCastByLinkedUserId(userId: String): Cast?

    suspend fun followCast(userId: String, castId: String)

    suspend fun unfollowCast(userId: String, castId: String)

    suspend fun getFollowerUserIds(castId: String): List<String>

    suspend fun getFollowerSnapshots(castId: String): List<CastFollowerSnapshot>

    suspend fun getPopularTodayCasts(limit: Int): List<CheckInCastSummary>
}
