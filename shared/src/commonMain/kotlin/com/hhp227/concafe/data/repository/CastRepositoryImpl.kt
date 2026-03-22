package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.SocialDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
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

class CastRepositoryImpl(
    private val castDataSource: CastDataSource,
    private val cafeDataSource: CafeDataSource,
    private val socialDataSource: SocialDataSource,
    private val pagingDataSource: PagingDataSource
) : CastRepository {
    override suspend fun searchCasts(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast> {
        var filtered = castDataSource.casts

        if (!query.isNullOrBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }
        if (!country.isNullOrBlank() || !city.isNullOrBlank()) {
            val validCafeIds = cafeDataSource.cafes.filter { cafe ->
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
        return pagingDataSource.toPaged(filtered, cursor, pageSize)
    }

    override suspend fun getHomePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast> {
        val sorted = castDataSource.casts
            .sortedByDescending { it.followerCount }
        return pagingDataSource.toPaged(sorted, cursor, pageSize)
    }

    override suspend fun getCastDetail(castId: String): CastDetail {
        return castDataSource.castDetail(castId)
            ?: throw NoSuchElementException("cast detail not found")
    }

    override suspend fun getCafeCastPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeCastPreview> {
        val sorted = castDataSource.casts
            .filter { it.cafeId == cafeId }
            .sortedWith(
                compareByDescending<Cast> { cafeDataSource.onShiftCastIdsByCafeId[cafeId].orEmpty().contains(it.id) }
                    .thenBy { it.name }
            )
            .map { cast ->
                CafeCastPreview(
                    id = cast.id,
                    name = cast.name,
                    isOnShift = cafeDataSource.onShiftCastIdsByCafeId[cafeId].orEmpty().contains(cast.id),
                    profileImage = cast.profileImage
                )
            }
        return pagingDataSource.toPaged(sorted, cursor, pageSize)
    }

    override suspend fun getCafeCastListPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeDetailCast> {
        val sorted = castDataSource.casts
            .filter { it.cafeId == cafeId }
            .sortedWith(
                compareByDescending<Cast> { cafeDataSource.onShiftCastIdsByCafeId[cafeId].orEmpty().contains(it.id) }
                    .thenBy { it.name }
            )
            .map { cast ->
                CafeDetailCast(
                    cast = cast,
                    isWorking = cafeDataSource.onShiftCastIdsByCafeId[cafeId].orEmpty().contains(cast.id)
                )
            }
        return pagingDataSource.toPaged(sorted, cursor, pageSize)
    }

    override suspend fun upsertCast(update: CastUpsert): CastDetail {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            return firestoreDataSource.upsertCastRemote(update)
        }
        return castDataSource.upsertCast(update)
    }

    override suspend fun deleteCast(castId: String): Cast {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            return firestoreDataSource.deleteCastRemote(castId)
        }
        return castDataSource.deleteCast(castId)
    }

    override suspend fun getCastSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule> {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            runCatching {
                firestoreDataSource.refreshCastSchedulesRemote(castId, fromDate, toDate)
            }
        }
        return castDataSource.castSchedules(castId, fromDate, toDate)
    }

    override suspend fun getCastScheduleStatuses(
        castId: String,
        fromDate: String,
        toDate: String
    ): Map<String, CastScheduleStatus> {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            runCatching {
                firestoreDataSource.refreshCastSchedulesRemote(castId, fromDate, toDate)
            }
        }
        return castDataSource.castScheduleStatuses(castId, fromDate, toDate)
    }

    override suspend fun updateCastSchedule(update: CastScheduleUpdate): CastSchedule? {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            return firestoreDataSource.updateCastScheduleRemote(update)
        }
        return castDataSource.updateCastSchedule(update)
    }

    override suspend fun isFollowing(userId: String, castId: String): Boolean {
        return socialDataSource.followedCastIdsByUser[userId]?.contains(castId) == true
    }

    override suspend fun getFollowedCastIds(userId: String): List<String> {
        return socialDataSource.followedCastIdsByUser[userId]
            ?.toList()
            .orEmpty()
            .sorted()
    }

    override suspend fun getCastsByIds(castIds: List<String>): List<Cast> {
        val idSet = castIds.toSet()
        return castDataSource.casts
            .asSequence()
            .filter { cast -> idSet.contains(cast.id) }
            .toList()
    }

    override suspend fun getCastByLinkedUserId(userId: String): Cast? {
        return castDataSource.casts.firstOrNull { cast -> cast.linkedUserId == userId }
    }

    override suspend fun followCast(userId: String, castId: String) {
        val set = socialDataSource.followedCastIdsByUser.getOrPut(userId) { mutableSetOf() }
        set.add(castId)
    }

    override suspend fun unfollowCast(userId: String, castId: String) {
        val set = socialDataSource.followedCastIdsByUser.getOrPut(userId) { mutableSetOf() }
        set.remove(castId)
    }

    override suspend fun getFollowerUserIds(castId: String): List<String> {
        return socialDataSource.followedCastIdsByUser
            .filterValues { followedIds -> followedIds.contains(castId) }
            .keys
            .sorted()
    }

    override suspend fun getPopularTodayCasts(limit: Int): List<CheckInCastSummary> {
        val castScoreById = castDataSource.casts.associate { cast ->
            val score = castDataSource.castTodayVisitCountById[cast.id] ?: cast.followerCount
            cast.id to score
        }
        return castDataSource.casts
            .sortedByDescending { castScoreById[it.id] ?: 0 }
            .take(limit)
            .map { cast ->
                val cafeName = cafeDataSource.cafes.firstOrNull { it.id == cast.cafeId }?.name ?: cast.cafeId

                CheckInCastSummary(
                    id = cast.id,
                    cafeId = cast.cafeId,
                    cafeName = cafeName,
                    name = cast.name,
                    profileImage = cast.profileImage,
                    todayVisit = castDataSource.castTodayVisitCountById[cast.id] ?: 0
                )
            }
    }
}
