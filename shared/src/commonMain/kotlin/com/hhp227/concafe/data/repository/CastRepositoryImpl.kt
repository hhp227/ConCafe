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
import com.hhp227.concafe.domain.model.GuestCastSchedule
import com.hhp227.concafe.domain.model.GuestCastScheduleUpsert
import com.hhp227.concafe.domain.repository.CastRepository
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
        ).withCafeThumbnails()
    }

    override suspend fun getHomePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast> {
        return castRemoteDataSource.getHomePopularCastPageRemote(
            cursor = cursor,
            pageSize = pageSize
        ).withCafeThumbnails()
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
        ).take(safeLimit).withCafeThumbnails()
    }

    override suspend fun getCastDetail(castId: String): CastDetail {
        return castRemoteDataSource.fetchCastDetail(castId).withCafeThumbnail()
    }

    override suspend fun getCafeCastPage(cafeId: String, cursor: String?, pageSize: Int): PagedResult<CafeCastPreview> {
        val workingCastIds = resolveWorkingCastIds(cafeId)
        val page = castRemoteDataSource.getCafeCastPageRemote(
            cafeId = cafeId,
            cursor = cursor,
            pageSize = pageSize
        )
        val source = page.items.withCafeThumbnails()
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
                    profileImage = cast.profileImage,
                    cafeThumbnailImage = cast.cafeThumbnailImage
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
        var page = castRemoteDataSource.getCafeCastPageRemote(
            cafeId = cafeId,
            cursor = cursor,
            pageSize = pageSize
        )
        var source: List<Cast> = page.items.filter { it.isActive }
        // 비활동(INACTIVE) 캐스트만 담긴 페이지면 다음 페이지를 이어서 조회해 빈 페이지 반환을 방지
        while (source.isEmpty() && page.hasNext && !page.nextCursor.isNullOrBlank()) {
            page = castRemoteDataSource.getCafeCastPageRemote(
                cafeId = cafeId,
                cursor = page.nextCursor,
                pageSize = pageSize
            )
            source = page.items.filter { it.isActive }
        }
        source = source.withCafeThumbnails()
        val todayScheduleByCastId = castRemoteDataSource.getWorkingCastSchedulesByCafeAndDate(cafeId, todayDate)
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
        return castRemoteDataSource.upsertCastRemote(update).withCafeThumbnail()
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

    override suspend fun getWorkingCastSchedulesByCafeAndDate(
        cafeId: String,
        date: String
    ): Map<String, CastSchedule> {
        return castRemoteDataSource.getWorkingCastSchedulesByCafeAndDate(cafeId = cafeId, date = date)
    }

    override suspend fun updateCastSchedule(update: CastScheduleUpdate): CastSchedule? {
        return castRemoteDataSource.updateCastScheduleRemote(update)
    }

    override suspend fun getGuestCastSchedules(cafeId: String, fromDate: String, toDate: String): List<GuestCastSchedule> {
        return castRemoteDataSource.fetchGuestCastSchedules(cafeId, fromDate, toDate)
    }

    override suspend fun upsertGuestCastSchedule(input: GuestCastScheduleUpsert): GuestCastSchedule {
        return castRemoteDataSource.upsertGuestCastScheduleRemote(input)
    }

    override suspend fun deleteGuestCastSchedule(scheduleId: String) {
        castRemoteDataSource.deleteGuestCastScheduleRemote(scheduleId)
    }

    private suspend fun resolveWorkingCastIds(cafeId: String): Set<String> {
        return castRemoteDataSource.getWorkingCastIdsByCafeAndDate(cafeId = cafeId, date = todayDate())
    }

    override suspend fun isFollowing(userId: String, castId: String): Boolean {
        val followedCastIds = castRemoteDataSource.fetchFollowedCastIds(userId)
        return followedCastIds.contains(castId)
    }

    override suspend fun getFollowedCastIds(userId: String): List<String> {
        return castRemoteDataSource.fetchFollowedCastIds(userId)
    }

    override suspend fun getFollowedCasts(userId: String): List<Cast> {
        return castRemoteDataSource.getFollowedCastsRemote(userId).withCafeThumbnails()
    }

    override suspend fun getCastsByIds(castIds: List<String>): List<Cast> {
        return castRemoteDataSource.fetchCastsByIds(castIds).withCafeThumbnails()
    }

    override suspend fun getCastByLinkedUserId(userId: String): Cast? {
        return castRemoteDataSource.refreshCastByLinkedUserId(userId)
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
        val cafeById = sourceCasts
            .map { cast -> cast.cafeId }
            .distinct()
            .associateWith { cafeId ->
                runCatching {
                    cafeRemoteDataSource.fetchCafeDetail(cafeId).cafe
                }.getOrNull()
            }
        return sourceCasts.map { cast ->
            val cafe = cafeById[cast.cafeId]
            val cafeName = cafe?.name ?: cast.cafeId

            CheckInCastSummary(
                id = cast.id,
                cafeId = cast.cafeId,
                cafeName = cafeName,
                name = cast.name,
                profileImage = cast.profileImage,
                todayVisit = 0,
                cafeThumbnailImage = cafe?.thumbnailImage
            )
        }
    }

    override suspend fun getCafeCasts(cafeId: String): List<Cast> {
        return castRemoteDataSource.fetchCafeCasts(cafeId).withCafeThumbnails()
    }

    /** 캐스트 프로필 이미지가 없을 때 대체 표시할 소속 카페 썸네일을 채운다. */
    private suspend fun List<Cast>.withCafeThumbnails(): List<Cast> {
        if (isEmpty()) return this
        val thumbnailByCafeId = map { cast -> cast.cafeId }
            .filter { cafeId -> cafeId.isNotBlank() }
            .distinct()
            .associateWith { cafeId ->
                runCatching { cafeRemoteDataSource.fetchCafeById(cafeId)?.thumbnailImage }
                    .getOrNull()
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            }
        return map { cast ->
            if (!cast.cafeThumbnailImage.isNullOrBlank()) cast
            else cast.copy(cafeThumbnailImage = thumbnailByCafeId[cast.cafeId])
        }
    }

    private fun CastDetail.withCafeThumbnail(): CastDetail {
        if (!cast.cafeThumbnailImage.isNullOrBlank()) return this
        val thumbnail = cafe.thumbnailImage?.trim()?.takeIf { it.isNotEmpty() } ?: return this
        return copy(cast = cast.copy(cafeThumbnailImage = thumbnail))
    }

    private suspend fun PagedResult<Cast>.withCafeThumbnails(): PagedResult<Cast> {
        return copy(items = items.withCafeThumbnails())
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
