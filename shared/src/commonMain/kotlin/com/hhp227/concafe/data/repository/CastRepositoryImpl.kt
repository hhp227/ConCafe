package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeRemoteDataSource
import com.hhp227.concafe.data.source.CastRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.domain.model.CafeDetailCast
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastFollowerSnapshot
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.CheckInCastSummary
import com.hhp227.concafe.domain.repository.CastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CastRepositoryImpl(
    private val cafeRemoteDataSource: CafeRemoteDataSource,
    private val castRemoteDataSource: CastRemoteDataSource
) : CastRepository {
    override suspend fun searchCasts(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast> {
        return castRemoteDataSource.searchCastsRemote(
            query = query,
            country = country,
            city = city,
            sort = sort,
            cursor = cursor,
            pageSize = pageSize
        )
    }

    override suspend fun getHomePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast> {
        return castRemoteDataSource.getHomePopularCastPageRemote(
            cursor = cursor,
            pageSize = pageSize
        )
    }

    override suspend fun getBirthdayCasts(
        month: Int,
        dayOfMonth: Int,
        limit: Int
    ): List<Cast> {
        val safeLimit = if (limit > 0) limit else 1
        return castRemoteDataSource.fetchBirthdayCastsRemote(
            month = month,
            dayOfMonth = dayOfMonth,
            limit = safeLimit
        ).take(safeLimit)
    }

    override suspend fun getCastDetail(castId: String): CastDetail {
        return castRemoteDataSource.fetchCastDetail(castId)
    }

    override suspend fun getCafeCastPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeCastPreview> {
        val workingCastIds = resolveWorkingCastIds(cafeId)
        val page = castRemoteDataSource.getCafeCastPageRemote(
            cafeId = cafeId,
            cursor = cursor,
            pageSize = pageSize
        )
        val source = page.items
        val sorted = source
            .sortedWith(
                compareByDescending<Cast> { workingCastIds.contains(it.id) }
                    .thenBy { it.name }
            )
            .map { cast ->
                CafeCastPreview(
                    id = cast.id,
                    name = cast.name,
                    isOnShift = workingCastIds.contains(cast.id),
                    profileImage = cast.profileImage
                )
            }
        return PagedResult(
            items = sorted,
            nextCursor = page.nextCursor,
            hasNext = page.hasNext
        )
    }

    override suspend fun getCafeCastListPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeDetailCast> {
        val todayDate = todayDate()
        val page = castRemoteDataSource.getCafeCastPageRemote(
            cafeId = cafeId,
            cursor = cursor,
            pageSize = pageSize
        )
        val source = page.items
        val todayScheduleByCastId = resolveTodaySchedulesByCastId(
            cafeId = cafeId,
            casts = source,
            todayDate = todayDate
        )
        val sorted = source
            .sortedWith(
                compareByDescending<Cast> { todayScheduleByCastId.containsKey(it.id) }
                    .thenBy { it.name }
            )
            .map { cast ->
                val todaySchedule = todayScheduleByCastId[cast.id]
                CafeDetailCast(
                    cast = cast,
                    isWorking = todaySchedule?.isOnShift() == true,
                    todaySchedule = todaySchedule
                )
            }
        return PagedResult(
            items = sorted,
            nextCursor = page.nextCursor,
            hasNext = page.hasNext
        )
    }

    override suspend fun upsertCast(update: CastUpsert): CastDetail {
        return castRemoteDataSource.upsertCastRemote(update)
    }

    override suspend fun deleteCast(castId: String): Cast {
        return castRemoteDataSource.deleteCastRemote(castId)
    }

    override suspend fun getCastSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule> {
        return castRemoteDataSource.fetchCastSchedules(castId, fromDate, toDate)
    }

    override suspend fun getCastScheduleStatuses(
        castId: String,
        fromDate: String,
        toDate: String
    ): Map<String, CastScheduleStatus> {
        return castRemoteDataSource.fetchCastScheduleStatuses(castId, fromDate, toDate)
    }

    override suspend fun getWorkingCastIdsByCafeAndDate(cafeId: String, date: String): Set<String> {
        return castRemoteDataSource.getWorkingCastIdsByCafeAndDate(cafeId = cafeId, date = date)
    }

    override suspend fun updateCastSchedule(update: CastScheduleUpdate): CastSchedule? {
        return castRemoteDataSource.updateCastScheduleRemote(update)
    }

    private suspend fun resolveWorkingCastIds(cafeId: String): Set<String> {
        return castRemoteDataSource.getWorkingCastIdsByCafeAndDate(cafeId = cafeId, date = todayDate())
    }

    private suspend fun resolveTodaySchedulesByCastId(
        cafeId: String,
        casts: List<Cast>,
        todayDate: String
    ): Map<String, CastSchedule> = coroutineScope {
        casts.associate { cast ->
            cast.id to async {
                castRemoteDataSource.fetchCastSchedules(
                    castId = cast.id,
                    fromDate = todayDate,
                    toDate = todayDate
                ).firstOrNull { schedule ->
                    schedule.cafeId == cafeId && schedule.date == todayDate
                }
            }
        }.mapNotNull { (castId, scheduleDeferred) ->
            val schedule = scheduleDeferred.await()
            if (schedule == null) null else castId to schedule
        }.toMap()
    }

    override suspend fun isFollowing(userId: String, castId: String): Boolean {
        val followedCastIds = castRemoteDataSource.fetchFollowedCastIds(userId)
        return followedCastIds.contains(castId)
    }

    override suspend fun getFollowedCastIds(userId: String): List<String> {
        return castRemoteDataSource.fetchFollowedCastIds(userId)
    }

    override suspend fun getFollowedCasts(userId: String): List<Cast> {
        return castRemoteDataSource.getFollowedCastsRemote(userId)
    }

    override suspend fun getCastsByIds(castIds: List<String>): List<Cast> {
        return castRemoteDataSource.fetchCastsByIds(castIds)
    }

    override suspend fun getCastByLinkedUserId(userId: String): Cast? {
        return castRemoteDataSource.fetchCastByLinkedUserId(userId)
    }

    override suspend fun followCast(userId: String, castId: String) {
        castRemoteDataSource.followCastRemote(
            userId = userId,
            castId = castId
        )
    }

    override suspend fun unfollowCast(userId: String, castId: String) {
        castRemoteDataSource.unfollowCastRemote(
            userId = userId,
            castId = castId
        )
    }

    override suspend fun getFollowerUserIds(castId: String): List<String> {
        return castRemoteDataSource.fetchFollowerUserIds(castId)
    }

    override suspend fun getFollowerSnapshots(castId: String): List<CastFollowerSnapshot> {
        return castRemoteDataSource.getCastFollowerSnapshots(castId)
    }

    override suspend fun getPopularTodayCasts(limit: Int): List<CheckInCastSummary> {
        val safeLimit = if (limit > 0) limit else 1
        val sourceCasts = castRemoteDataSource.searchCastsRemote(
            query = null,
            country = null,
            city = null,
            sort = CastSort.POPULAR,
            cursor = null,
            pageSize = safeLimit
        ).items
        val cafeNameById = sourceCasts
            .map { cast -> cast.cafeId }
            .distinct()
            .associateWith { cafeId ->
                runCatching {
                    cafeRemoteDataSource.fetchCafeDetail(cafeId).cafe.name
                }.getOrNull()
            }
        return sourceCasts.map { cast ->
            val cafeName = cafeNameById[cast.cafeId] ?: cast.cafeId

            CheckInCastSummary(
                id = cast.id,
                cafeId = cast.cafeId,
                cafeName = cafeName,
                name = cast.name,
                profileImage = cast.profileImage,
                todayVisit = 0
            )
        }
    }

}

private fun todayDate(): String {
    return Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}

private fun CastSchedule.isOnShift(): Boolean {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val currentMinutes = now.hour * 60 + now.minute
    val startMinutes = startTime.toMinutes()
    val endMinutes = endTime.toMinutes()
    return currentMinutes >= startMinutes && currentMinutes <= endMinutes
}

private fun String.toMinutes(): Int {
    val hour = substringBefore(':').toIntOrNull() ?: 0
    val minute = substringAfter(':').toIntOrNull() ?: 0
    return hour * 60 + minute
}
