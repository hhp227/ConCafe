package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastFollowerSnapshot
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.CastUpsert

interface CastRemoteDataSource {
    suspend fun searchCastsRemote(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast>

    suspend fun getHomePopularCastPageRemote(cursor: String?, pageSize: Int): PagedResult<Cast>

    suspend fun fetchBirthdayCastsRemote(month: Int, dayOfMonth: Int, limit: Int): List<Cast>

    suspend fun refreshCastDetailRemote(castId: String)

    suspend fun fetchCastDetail(castId: String): CastDetail

    suspend fun getCafeCastPageRemote(
        cafeId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast>

    suspend fun upsertCastRemote(update: CastUpsert): CastDetail

    suspend fun deleteCastRemote(castId: String): Cast

    suspend fun refreshCastSchedulesRemote(castId: String, fromDate: String, toDate: String)

    suspend fun fetchCastSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule>

    suspend fun fetchCastScheduleStatuses(
        castId: String,
        fromDate: String,
        toDate: String
    ): Map<String, CastScheduleStatus>

    suspend fun getWorkingCastIdsByCafeAndDate(cafeId: String, date: String): Set<String>

    suspend fun updateCastScheduleRemote(update: CastScheduleUpdate): CastSchedule?

    suspend fun refreshFollowedCastIds(userId: String)

    suspend fun fetchFollowedCastIds(userId: String): List<String>

    suspend fun getFollowedCastsRemote(userId: String): List<Cast>

    suspend fun refreshCastByLinkedUserId(userId: String): Cast?

    suspend fun fetchCastByLinkedUserId(userId: String): Cast?

    suspend fun refreshCafeCastsRemote(cafeId: String)

    suspend fun fetchCafeCasts(cafeId: String): List<Cast>

    suspend fun fetchCastsByIds(castIds: List<String>): List<Cast>

    suspend fun fetchAllCasts(): List<Cast>

    suspend fun fetchAffiliatedCafeId(userId: String): String?

    suspend fun setAffiliatedCafeId(userId: String, cafeId: String)

    suspend fun clearAffiliatedCafeId(userId: String)

    suspend fun followCastRemote(userId: String, castId: String)

    suspend fun unfollowCastRemote(userId: String, castId: String)

    suspend fun refreshFollowerUserIds(castId: String)

    suspend fun fetchFollowerUserIds(castId: String): List<String>

    suspend fun getCastFollowerSnapshots(castId: String): List<CastFollowerSnapshot>
}
