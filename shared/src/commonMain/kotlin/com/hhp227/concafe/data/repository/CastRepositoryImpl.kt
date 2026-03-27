package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.ScheduleStatusDataSource
import com.hhp227.concafe.data.source.SocialDataSource
import com.hhp227.concafe.data.source.FirestoreCacheDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
        val firestoreDataSource = castDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null && pageSize <= REMOTE_CAST_PAGE_LIMIT) {
            val remoteResult = runCatching {
                firestoreDataSource.searchCastsRemote(
                    query = query,
                    country = country,
                    city = city,
                    sort = sort,
                    cursor = cursor,
                    pageSize = pageSize
                )
            }.getOrNull()

            if (remoteResult != null) {
                return remoteResult
            }
        }
        return searchCastsFromCache(
            query = query,
            country = country,
            city = city,
            sort = sort,
            cursor = cursor,
            pageSize = pageSize
        )
    }

    override suspend fun getHomePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast> {
        val firestoreDataSource = castDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            val remoteResult = runCatching {
                firestoreDataSource.getHomePopularCastPageRemote(
                    cursor = cursor,
                    pageSize = pageSize
                )
            }.getOrNull()

            if (remoteResult != null) {
                return remoteResult
            }
        }
        val sorted = castDataSource.casts
            .sortedByDescending { it.followerCount }
        return pagingDataSource.toPaged(sorted, cursor, pageSize)
    }

    override suspend fun getBirthdayCasts(
        month: Int,
        dayOfMonth: Int,
        limit: Int
    ): List<Cast> {
        val safeLimit = if (limit > 0) {
            limit
        } else {
            1
        }
        val firestoreDataSource = castDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            val remoteCasts = runCatching {
                firestoreDataSource.fetchBirthdayCastsRemote(
                    month = month,
                    dayOfMonth = dayOfMonth,
                    limit = safeLimit
                )
            }.getOrElse {
                emptyList()
            }

            if (remoteCasts.isNotEmpty()) {
                return remoteCasts.take(safeLimit)
            }
        }

        return castDataSource.casts
            .asSequence()
            .filter { cast ->
                cast.birthday.matchesMonthAndDay(month = month, dayOfMonth = dayOfMonth)
            }
            .sortedByDescending { cast -> cast.id }
            .take(safeLimit)
            .toList()
    }

    override suspend fun getCastDetail(castId: String): CastDetail {
        var detail = castDataSource.castDetail(castId)

        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            runCatching {
                firestoreDataSource.refreshCastDetailRemote(castId)
            }
            detail = castDataSource.castDetail(castId) ?: detail
        }
        return detail ?: throw NoSuchElementException("cast detail not found")
    }

    override suspend fun getCafeCastPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeCastPreview> {
        val workingCastIds = resolveWorkingCastIds(cafeId)
        val firestoreDataSource = castDataSource as? FirestoreConCafeDataSource
        val page = if (firestoreDataSource != null) {
            runCatching {
                firestoreDataSource.getCafeCastPageRemote(
                    cafeId = cafeId,
                    cursor = cursor,
                    pageSize = pageSize
                )
            }.getOrNull()
        } else {
            null
        }
        val source = if (page != null) {
            page.items
        } else {
            castDataSource.casts.filter { it.cafeId == cafeId }
        }
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
        return if (page != null) {
            PagedResult(
                items = sorted,
                nextCursor = page.nextCursor,
                hasNext = page.hasNext
            )
        } else {
            pagingDataSource.toPaged(sorted, cursor, pageSize)
        }
    }

    override suspend fun getCafeCastListPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeDetailCast> {
        val workingCastIds = resolveWorkingCastIds(cafeId)
        val firestoreDataSource = castDataSource as? FirestoreConCafeDataSource
        val page = if (firestoreDataSource != null) {
            runCatching {
                firestoreDataSource.getCafeCastPageRemote(
                    cafeId = cafeId,
                    cursor = cursor,
                    pageSize = pageSize
                )
            }.getOrNull()
        } else {
            null
        }
        val source = if (page != null) {
            page.items
        } else {
            castDataSource.casts.filter { it.cafeId == cafeId }
        }
        val sorted = source
            .sortedWith(
                compareByDescending<Cast> { workingCastIds.contains(it.id) }
                    .thenBy { it.name }
            )
            .map { cast ->
                CafeDetailCast(
                    cast = cast,
                    isWorking = workingCastIds.contains(cast.id)
                )
            }
        return if (page != null) {
            PagedResult(
                items = sorted,
                nextCursor = page.nextCursor,
                hasNext = page.hasNext
            )
        } else {
            pagingDataSource.toPaged(sorted, cursor, pageSize)
        }
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

    override suspend fun getWorkingCastIdsByCafeAndDate(cafeId: String, date: String): Set<String> {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            return firestoreDataSource.getWorkingCastIdsByCafeAndDate(cafeId = cafeId, date = date)
        }
        val scheduleStatusDataSource = castDataSource as? ScheduleStatusDataSource
        val cafeCastIds = castDataSource.casts
            .asSequence()
            .filter { cast -> cast.cafeId == cafeId }
            .map { cast -> cast.id }
            .toSet()
        return scheduleStatusDataSource?.castScheduleStatusByCastId
            ?.asSequence()
            ?.filter { (castId, _) -> cafeCastIds.contains(castId) }
            ?.filter { (_, statuses) -> statuses[date] == CastScheduleStatus.WORK }
            ?.map { (castId, _) -> castId }
            ?.toSet()
            .orEmpty()
    }

    override suspend fun updateCastSchedule(update: CastScheduleUpdate): CastSchedule? {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            return firestoreDataSource.updateCastScheduleRemote(update)
        }
        return castDataSource.updateCastSchedule(update)
    }

    private suspend fun resolveWorkingCastIds(cafeId: String): Set<String> {
        val todayDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
        val firestoreDataSource = castDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            return runCatching {
                firestoreDataSource.getWorkingCastIdsByCafeAndDate(cafeId = cafeId, date = todayDate)
            }.getOrElse {
                cafeDataSource.onShiftCastIdsByCafeId[cafeId].orEmpty()
            }
        } else {
            return cafeDataSource.onShiftCastIdsByCafeId[cafeId].orEmpty()
        }
    }

    override suspend fun isFollowing(userId: String, castId: String): Boolean {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            runCatching {
                firestoreDataSource.refreshFollowedCastIds(userId)
            }
        }
        return socialDataSource.followedCastIdsByUser[userId]?.contains(castId) == true
    }

    override suspend fun getFollowedCastIds(userId: String): List<String> {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            runCatching {
                firestoreDataSource.refreshFollowedCastIds(userId)
            }
        }
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
        val cached = castDataSource.casts.firstOrNull { cast -> cast.linkedUserId == userId }

        if (cached != null) {
            return cached
        } else {
            val firestoreDataSource = castDataSource as? FirestoreConCafeDataSource
            if (firestoreDataSource != null) {
                val remote = runCatching {
                    firestoreDataSource.refreshCastByLinkedUserId(userId)
                }.getOrNull()
                if (remote != null) {
                    return remote
                }
            }
            return castDataSource.casts.firstOrNull { cast -> cast.linkedUserId == userId }
        }
    }

    override suspend fun followCast(userId: String, castId: String) {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            firestoreDataSource.followCastRemote(
                userId = userId,
                castId = castId
            )
            return
        }
        val set = socialDataSource.followedCastIdsByUser.getOrPut(userId) { mutableSetOf() }
        set.add(castId)
        val followerSet = socialDataSource.followerUserIdsByCastId.getOrPut(castId) { mutableSetOf() }

        followerSet.add(userId)
        updateFollowerCountInCache(castId = castId, followerCount = followerSet.size)
    }

    override suspend fun unfollowCast(userId: String, castId: String) {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            firestoreDataSource.unfollowCastRemote(
                userId = userId,
                castId = castId
            )
            return
        }
        val set = socialDataSource.followedCastIdsByUser.getOrPut(userId) { mutableSetOf() }
        set.remove(castId)
        val followerSet = socialDataSource.followerUserIdsByCastId.getOrPut(castId) { mutableSetOf() }
        followerSet.remove(userId)
        updateFollowerCountInCache(castId = castId, followerCount = followerSet.size)
    }

    override suspend fun getFollowerUserIds(castId: String): List<String> {
        (castDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            runCatching {
                firestoreDataSource.refreshFollowerUserIds(castId)
            }
        }
        return socialDataSource.followerUserIdsByCastId[castId]
            ?.toList()
            ?.sorted()
            .orEmpty()
    }

    override suspend fun getFollowerSnapshots(castId: String): List<CastFollowerSnapshot> {
        val firestoreDataSource = castDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            val remote = runCatching {
                firestoreDataSource.getCastFollowerSnapshots(castId)
            }.getOrNull()

            if (remote != null) {
                return remote
            }
        }
        return getFollowerUserIds(castId)
            .map { followerUserId ->
                CastFollowerSnapshot(
                    userId = followerUserId,
                    followedAt = ""
                )
            }
    }

    override suspend fun getPopularTodayCasts(limit: Int): List<CheckInCastSummary> {
        val castScoreById = castDataSource.casts.associate { cast ->
            val score = castDataSource.castTodayVisitCountById[cast.id] ?: cast.followerCount
            return@associate cast.id to score
        }
        return castDataSource.casts
            .sortedByDescending { castScoreById[it.id] ?: 0 }
            .take(limit)
            .map { cast ->
                val cafeName = cafeDataSource.cafes.firstOrNull { it.id == cast.cafeId }?.name ?: cast.cafeId
                return@map CheckInCastSummary(
                    id = cast.id,
                    cafeId = cast.cafeId,
                    cafeName = cafeName,
                    name = cast.name,
                    profileImage = cast.profileImage,
                    todayVisit = castDataSource.castTodayVisitCountById[cast.id] ?: 0
                )
            }
    }

    private fun updateFollowerCountInCache(castId: String, followerCount: Int) {
        val cacheDataSource = castDataSource as? FirestoreCacheDataSource
            ?: return
        val index = cacheDataSource.casts.indexOfFirst { cast -> cast.id == castId }

        if (index >= 0) {
            cacheDataSource.casts[index] = cacheDataSource.casts[index].copy(
                followerCount = followerCount
            )
        }
    }

    private fun searchCastsFromCache(
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
}

private fun String?.matchesMonthAndDay(month: Int, dayOfMonth: Int): Boolean {
    val birthdayValue = this
    if (birthdayValue == null) {
        return false
    }
    val parts = birthdayValue.split("-")
    if (parts.size != 3) {
        return false
    }
    val birthMonth = parts[1].toIntOrNull()
    val birthDay = parts[2].toIntOrNull()

    return birthMonth == month && birthDay == dayOfMonth
}

private const val REMOTE_CAST_PAGE_LIMIT = 100
