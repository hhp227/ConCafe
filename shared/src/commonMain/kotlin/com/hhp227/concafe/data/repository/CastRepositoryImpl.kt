package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.domain.model.CafeDetailCast
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.CheckInCastSummary
import com.hhp227.concafe.domain.repository.CastRepository

class CastRepositoryImpl : CastRepository {
    override suspend fun searchCasts(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast> {
        TODO("Not yet implemented")
    }

    override suspend fun getHomePopularCastPage(
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast> {
        TODO("Not yet implemented")
    }

    override suspend fun getCastDetail(castId: String): CastDetail {
        TODO("Not yet implemented")
    }

    override suspend fun getCafeCastPage(
        cafeId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeCastPreview> {
        TODO("Not yet implemented")
    }

    override suspend fun getCafeCastListPage(
        cafeId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeDetailCast> {
        TODO("Not yet implemented")
    }

    override suspend fun upsertCast(update: CastUpsert): CastDetail {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCast(castId: String): Cast {
        TODO("Not yet implemented")
    }

    override suspend fun getCastSchedules(
        castId: String,
        fromDate: String,
        toDate: String
    ): List<CastSchedule> {
        TODO("Not yet implemented")
    }

    override suspend fun getCastScheduleStatuses(
        castId: String,
        fromDate: String,
        toDate: String
    ): Map<String, CastScheduleStatus> {
        TODO("Not yet implemented")
    }

    override suspend fun updateCastSchedule(update: CastScheduleUpdate): CastSchedule? {
        TODO("Not yet implemented")
    }

    override suspend fun isFollowing(userId: String, castId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun followCast(userId: String, castId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun unfollowCast(userId: String, castId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getFollowerUserIds(castId: String): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun getPopularTodayCasts(limit: Int): List<CheckInCastSummary> {
        TODO("Not yet implemented")
    }
}