package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.ExploreRegionFilter
import com.hhp227.concafe.domain.model.HomeFeed
import com.hhp227.concafe.domain.repository.BannerRepository
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.NoticeRepository
import com.hhp227.concafe.domain.model.CafeSort
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetHomeFeedUseCase(
    private val bannerRepository: BannerRepository,
    private val cafeRepository: CafeRepository,
    private val castRepository: CastRepository,
    private val noticeRepository: NoticeRepository
) {
    private fun resolveNearbyRegionFilter(): ExploreRegionFilter? {
        val timeZoneId = TimeZone.currentSystemDefault().id.lowercase()
        val isKoreaTimeZone = timeZoneId.contains("seoul")
            || timeZoneId == "rok"
        val isJapanTimeZone = timeZoneId.contains("tokyo")
            || timeZoneId.contains("osaka")
            || timeZoneId == "japan"
        return if (isKoreaTimeZone) {
            ExploreRegionFilter.from("seoul")
        } else if (isJapanTimeZone) {
            if (timeZoneId.contains("osaka")) {
                ExploreRegionFilter.from("osaka")
            } else {
                ExploreRegionFilter.from("tokyo")
            }
        } else {
            null
        }
    }

    suspend operator fun invoke(
        popularCastCursor: String? = null,
        nearbyCafeCursor: String? = null
    ): AppResult<HomeFeed> = coroutineScope {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val nearbyRegionFilter = resolveNearbyRegionFilter()
        val bannersDeferred = async {
            runCatching { bannerRepository.getHomeBanners(HOME_FEED_LIMIT) }
        }
        val nearbyCafePageDeferred = async {
            runCatching {
                cafeRepository.searchCafes(
                    query = null,
                    country = nearbyRegionFilter?.country,
                    city = nearbyRegionFilter?.city,
                    sort = CafeSort.RATING,
                    cursor = nearbyCafeCursor,
                    pageSize = NEARBY_CAFE_PAGE_SIZE
                )
            }
        }
        val popularCastPageDeferred = async {
            runCatching {
                castRepository.getHomePopularCastPage(
                    cursor = popularCastCursor,
                    pageSize = POPULAR_CAST_PAGE_SIZE
                )
            }
        }
        val birthdayCastsDeferred = async {
            runCatching {
                castRepository.getBirthdayCasts(
                    month = today.monthNumber,
                    dayOfMonth = today.dayOfMonth,
                    limit = HOME_FEED_LIMIT
                )
            }
        }
        val noticesDeferred = async {
            runCatching { noticeRepository.getRecentNotices(HOME_FEED_LIMIT) }
        }
        val bannersResult = bannersDeferred.await()
        val nearbyCafePageResult = nearbyCafePageDeferred.await()
        val popularCastPageResult = popularCastPageDeferred.await()
        val birthdayCastsResult = birthdayCastsDeferred.await()
        val noticesResult = noticesDeferred.await()
        bannersResult.exceptionOrNull()?.let { error ->
            println("TEST, ${error.message}")
        }
        nearbyCafePageResult.exceptionOrNull()?.let { error ->
            println("TEST, ${error.message}")
        }
        popularCastPageResult.exceptionOrNull()?.let { error ->
            println("TEST, ${error.message}")
        }
        birthdayCastsResult.exceptionOrNull()?.let { error ->
            println("TEST, ${error.message}")
        }
        noticesResult.exceptionOrNull()?.let { error ->
            println("TEST, ${error.message}")
        }
        val firstError = listOf(
            bannersResult.exceptionOrNull(),
            nearbyCafePageResult.exceptionOrNull(),
            popularCastPageResult.exceptionOrNull(),
            birthdayCastsResult.exceptionOrNull(),
            noticesResult.exceptionOrNull()
        ).firstOrNull { throwable -> throwable != null }
        val allFailed = bannersResult.isFailure &&
            nearbyCafePageResult.isFailure &&
            popularCastPageResult.isFailure &&
            birthdayCastsResult.isFailure &&
            noticesResult.isFailure

        if (allFailed) {
            val throwable = firstError
            return@coroutineScope when (throwable) {
                is NoSuchElementException -> {
                    AppResult.Failure(AppError.NotFound)
                }
                is IllegalArgumentException -> {
                    AppResult.Failure(AppError.ValidationFailed(throwable.message ?: "invalid request"))
                }
                else -> {
                    AppResult.Failure(AppError.Unknown(throwable?.message))
                }
            }
        }
        val nearbyCafePage = nearbyCafePageResult.getOrNull()
        val popularCastPage = popularCastPageResult.getOrNull()
        val nearbyCafeNameById = nearbyCafePage
            ?.items
            ?.associate { cafe -> cafe.id to cafe.name }
            ?: emptyMap()
        val popularCastCafeIds = popularCastPage
            ?.items
            ?.map { cast -> cast.cafeId }
            ?.distinct()
            .orEmpty()
        val unresolvedCafeIds = popularCastCafeIds
            .filterNot { cafeId -> nearbyCafeNameById.containsKey(cafeId) }
        val resolvedFromDetail = unresolvedCafeIds
            .associateWith { cafeId ->
                async {
                    runCatching { cafeRepository.getCafeDetail(cafeId).cafe.name }
                        .getOrElse { cafeId }
                }
            }
            .mapValues { entry -> entry.value.await() }
        val popularCastCafeNames = popularCastCafeIds.associateWith { cafeId ->
            nearbyCafeNameById[cafeId] ?: resolvedFromDetail[cafeId] ?: cafeId
        }
        return@coroutineScope AppResult.Success(
            HomeFeed(
                banners = bannersResult.getOrElse { emptyList() },
                popularCasts = popularCastPage?.items ?: emptyList(),
                popularCastCafeNames = popularCastCafeNames,
                popularCastsNextCursor = popularCastPage?.nextCursor,
                hasMorePopularCasts = popularCastPage?.hasNext == true,
                nearbyCafes = nearbyCafePage?.items ?: emptyList(),
                nearbyCafesNextCursor = nearbyCafePage?.nextCursor,
                hasMoreNearbyCafes = nearbyCafePage?.hasNext == true,
                birthdayCasts = birthdayCastsResult.getOrElse { emptyList() },
                notices = noticesResult.getOrElse { emptyList() }
            )
        )
    }

    companion object {
        private const val HOME_FEED_LIMIT = 6
        private const val POPULAR_CAST_PAGE_SIZE = 10
        private const val NEARBY_CAFE_PAGE_SIZE = 6
    }
}
