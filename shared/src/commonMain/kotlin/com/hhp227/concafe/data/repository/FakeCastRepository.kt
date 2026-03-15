package com.hhp227.concafe.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.domain.model.CafeDetailCast
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastEvent
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.CheckInCastSummary
import com.hhp227.concafe.domain.model.ScheduleManagementEvent
import com.hhp227.concafe.domain.repository.CastRepository

class FakeCastRepository(
    private val dataSource: ConCafeDataSource
) : CastRepository {
    private val castEvent = MutableSharedFlow<CastEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    private val scheduleManagementEvent = MutableSharedFlow<ScheduleManagementEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override fun observeCastEvent(): Flow<CastEvent> {
        return castEvent
    }

    override fun observeScheduleManagementEvent(): Flow<ScheduleManagementEvent> {
        return scheduleManagementEvent
    }

    override suspend fun searchCasts(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast> {
        var filtered = dataSource.casts

        if (!query.isNullOrBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        if (!country.isNullOrBlank() || !city.isNullOrBlank()) {
            val validCafeIds = dataSource.cafes.filter { cafe ->
                val countryMatched = country.isNullOrBlank() || cafe.region.country.equals(country, ignoreCase = true)
                val cityMatched = city.isNullOrBlank() || cafe.region.city.equals(city, ignoreCase = true)
                countryMatched && cityMatched
            }.map { it.id }.toSet()
            filtered = filtered.filter { validCafeIds.contains(it.cafeId) }
        }

        filtered = when (sort) {
            CastSort.POPULAR -> filtered.sortedByDescending { it.followerCount }
            CastSort.LATEST -> filtered.sortedByDescending { it.id }
            CastSort.FOLLOWERS -> filtered.sortedByDescending { it.followerCount }
        }

        return dataSource.toPaged(filtered, cursor, pageSize)
    }

    override suspend fun getCastDetail(castId: String): CastDetail {
        return dataSource.castDetail(castId)
            ?: throw NoSuchElementException("cast detail not found")
    }

    override suspend fun getCafeCastPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeCastPreview> {
        val sorted = dataSource.casts
            .filter { it.cafeId == cafeId }
            .sortedWith(
                compareByDescending<Cast> { dataSource.onShiftCastIdsByCafeId[cafeId].orEmpty().contains(it.id) }
                    .thenBy { it.name }
            )
            .map { cast ->
                CafeCastPreview(
                    id = cast.id,
                    name = cast.name,
                    isOnShift = dataSource.onShiftCastIdsByCafeId[cafeId].orEmpty().contains(cast.id)
                )
            }

        return dataSource.toPaged(sorted, cursor, pageSize)
    }

    override suspend fun getCafeCastListPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeDetailCast> {
        val sorted = dataSource.casts
            .filter { it.cafeId == cafeId }
            .sortedWith(
                compareByDescending<Cast> { dataSource.onShiftCastIdsByCafeId[cafeId].orEmpty().contains(it.id) }
                    .thenBy { it.name }
            )
            .map { cast ->
                CafeDetailCast(
                    cast = cast,
                    isWorking = dataSource.onShiftCastIdsByCafeId[cafeId].orEmpty().contains(cast.id)
                )
            }

        return dataSource.toPaged(sorted, cursor, pageSize)
    }

    override fun observeCafeCastVersion(cafeId: String): Flow<Int> {
        return dataSource.observeCafeCastVersion(cafeId)
    }

    override fun observeCastVersion(castId: String): Flow<Int> {
        return dataSource.observeCastVersion(castId)
    }

    override suspend fun upsertCast(update: CastUpsert): CastDetail {
        val isCreate = update.castId.isNullOrBlank()
        return dataSource.upsertCast(update).also { detail ->
            val event = if (isCreate) {
                CastEvent.Created(detail.cast.cafeId, detail.cast)
            } else {
                CastEvent.Updated(detail.cast.cafeId, detail.cast)
            }
            castEvent.tryEmit(event)
        }
    }

    override suspend fun deleteCast(castId: String): Cast {
        return dataSource.deleteCast(castId).also { deletedCast ->
            castEvent.tryEmit(CastEvent.Deleted(deletedCast.cafeId, deletedCast.id))
        }
    }

    override suspend fun getCastSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule> {
        return dataSource.castSchedules(castId, fromDate, toDate)
    }

    override suspend fun getCastScheduleStatuses(
        castId: String,
        fromDate: String,
        toDate: String
    ): Map<String, CastScheduleStatus> {
        return dataSource.castScheduleStatuses(castId, fromDate, toDate)
    }

    override suspend fun updateCastSchedule(update: CastScheduleUpdate): ScheduleManagementEvent {
        dataSource.updateCastSchedule(update)
        return ScheduleManagementEvent.Updated(
            castId = update.castId,
            date = update.date,
            status = update.status
        ).also { event ->
            scheduleManagementEvent.tryEmit(event)
        }
    }

    override suspend fun isFollowing(userId: String, castId: String): Boolean {
        return dataSource.followedCastIdsByUser[userId]?.contains(castId) == true
    }

    override suspend fun followCast(userId: String, castId: String) {
        val set = dataSource.followedCastIdsByUser.getOrPut(userId) { mutableSetOf() }
        set.add(castId)
    }

    override suspend fun unfollowCast(userId: String, castId: String) {
        val set = dataSource.followedCastIdsByUser.getOrPut(userId) { mutableSetOf() }
        set.remove(castId)
    }

    override suspend fun getFollowerUserIds(castId: String): List<String> {
        return dataSource.followedCastIdsByUser
            .filterValues { followedIds -> followedIds.contains(castId) }
            .keys
            .sorted()
    }

    override suspend fun getPopularTodayCasts(limit: Int): List<CheckInCastSummary> {
        return dataSource.casts
            .sortedByDescending { dataSource.castTodayVisitCountById[it.id] ?: 0 }
            .take(limit)
            .map { cast ->
                val cafeName = dataSource.cafes.firstOrNull { it.id == cast.cafeId }?.name ?: cast.cafeId

                CheckInCastSummary(
                    id = cast.id,
                    cafeId = cast.cafeId,
                    cafeName = cafeName,
                    name = cast.name,
                    profileImage = cast.profileImage,
                    todayVisit = dataSource.castTodayVisitCountById[cast.id] ?: 0
                )
            }
    }
}
