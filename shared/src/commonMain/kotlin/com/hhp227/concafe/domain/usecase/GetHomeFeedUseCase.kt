package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Cafe
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
    private enum class NearbyCafePagingPhase {
        LOCAL,
        GLOBAL
    }

    private data class NearbyCafeCursorState(
        val phase: NearbyCafePagingPhase,
        val regionKey: String,
        val localCursor: String?,
        val globalCursor: String?
    )

    private data class NearbyCafePage(
        val items: List<Cafe>,
        val nextCursor: String?,
        val hasNext: Boolean
    )

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

    private fun isSameRegion(cafe: Cafe, regionFilter: ExploreRegionFilter): Boolean {
        val country = regionFilter.country
        val city = regionFilter.city
        return if (!country.isNullOrBlank() && !city.isNullOrBlank()) {
            cafe.region.country.equals(country, ignoreCase = true)
                && cafe.region.city.equals(city, ignoreCase = true)
        } else {
            false
        }
    }

    private fun encodeNearbyCafeCursor(state: NearbyCafeCursorState): String {
        val separator = "\u001F"
        val localCursor = state.localCursor ?: ""
        val globalCursor = state.globalCursor ?: ""
        return listOf(
            "v1",
            state.phase.name,
            state.regionKey,
            localCursor,
            globalCursor
        ).joinToString(separator)
    }

    private fun decodeNearbyCafeCursor(rawCursor: String?): NearbyCafeCursorState? {
        val separator = "\u001F"
        return if (rawCursor.isNullOrBlank()) {
            null
        } else {
            val tokens = rawCursor.split(separator)

            if (tokens.size != 5 || tokens[0] != "v1") {
                null
            } else {
                val phase = when (tokens[1]) {
                    NearbyCafePagingPhase.LOCAL.name -> NearbyCafePagingPhase.LOCAL
                    NearbyCafePagingPhase.GLOBAL.name -> NearbyCafePagingPhase.GLOBAL
                    else -> null
                }
                if (phase != null && tokens[2].isNotBlank()) {
                    NearbyCafeCursorState(
                        phase = phase,
                        regionKey = tokens[2],
                        localCursor = tokens[3].ifBlank { null },
                        globalCursor = tokens[4].ifBlank { null }
                    )
                } else {
                    null
                }
            }
        }
    }

    private suspend fun loadNearbyCafePage(
        nearbyCafeCursor: String?,
        nearbyRegionFilter: ExploreRegionFilter?
    ): NearbyCafePage {
        if (nearbyRegionFilter == null) {
            val page = cafeRepository.searchCafes(
                query = null,
                country = null,
                city = null,
                sort = CafeSort.RATING,
                cursor = nearbyCafeCursor,
                pageSize = NEARBY_CAFE_PAGE_SIZE
            )
            return NearbyCafePage(
                items = page.items,
                nextCursor = page.nextCursor,
                hasNext = page.hasNext
            )
        } else {
            val decodedCursor = decodeNearbyCafeCursor(nearbyCafeCursor)
            val currentState = if (decodedCursor != null && decodedCursor.regionKey == nearbyRegionFilter.key) {
                decodedCursor
            } else {
                NearbyCafeCursorState(
                    phase = NearbyCafePagingPhase.LOCAL,
                    regionKey = nearbyRegionFilter.key,
                    localCursor = null,
                    globalCursor = null
                )
            }
            val collectedItems = mutableListOf<Cafe>()
            var phase = currentState.phase
            var localCursor = currentState.localCursor
            var globalCursor = currentState.globalCursor
            var localHasNext = false
            var globalHasNext = false
            var didQueryGlobal = false

            if (phase == NearbyCafePagingPhase.LOCAL) {
                val localPage = cafeRepository.searchCafes(
                    query = null,
                    country = nearbyRegionFilter.country,
                    city = nearbyRegionFilter.city,
                    sort = CafeSort.RATING,
                    cursor = localCursor,
                    pageSize = NEARBY_CAFE_PAGE_SIZE
                )
                collectedItems.addAll(localPage.items)
                localCursor = localPage.nextCursor
                localHasNext = localPage.hasNext

                if (!localHasNext) {
                    phase = NearbyCafePagingPhase.GLOBAL
                }
            }

            var remainingCount = NEARBY_CAFE_PAGE_SIZE - collectedItems.size
            var globalLoopCount = 0

            if (remainingCount > 0 && phase == NearbyCafePagingPhase.GLOBAL) {
                while (remainingCount > 0 && globalLoopCount < NEARBY_GLOBAL_QUERY_MAX_ATTEMPTS) {
                    val previousGlobalCursor = globalCursor
                    val globalPage = cafeRepository.searchCafes(
                        query = null,
                        country = null,
                        city = null,
                        sort = CafeSort.RATING,
                        cursor = globalCursor,
                        pageSize = NEARBY_CAFE_PAGE_SIZE
                    )
                    val nonLocalItems = globalPage.items.filter { cafe ->
                        !isSameRegion(cafe, nearbyRegionFilter)
                    }
                    val appendItems = nonLocalItems.take(remainingCount)

                    collectedItems.addAll(appendItems)
                    globalCursor = globalPage.nextCursor
                    globalHasNext = globalPage.hasNext
                    didQueryGlobal = true
                    remainingCount = NEARBY_CAFE_PAGE_SIZE - collectedItems.size
                    globalLoopCount += 1

                    if (!globalPage.hasNext) {
                        break
                    }
                    if (globalCursor == previousGlobalCursor) {
                        break
                    }
                }
            }

            val hasNext = if (phase == NearbyCafePagingPhase.LOCAL) localHasNext else if (!didQueryGlobal) true else globalHasNext
            val nextCursor = if (hasNext) {
                encodeNearbyCafeCursor(
                    NearbyCafeCursorState(
                        phase = phase,
                        regionKey = nearbyRegionFilter.key,
                        localCursor = localCursor,
                        globalCursor = globalCursor
                    )
                )
            } else {
                null
            }
            return NearbyCafePage(
                items = collectedItems,
                nextCursor = nextCursor,
                hasNext = hasNext
            )
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
                loadNearbyCafePage(
                    nearbyCafeCursor = nearbyCafeCursor,
                    nearbyRegionFilter = nearbyRegionFilter
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
        val resolvedFromSummary = if (unresolvedCafeIds.isEmpty()) {
            emptyMap()
        } else {
            runCatching {
                cafeRepository.getCafesByIds(unresolvedCafeIds)
                    .associate { cafe -> cafe.id to cafe.name }
            }.getOrElse { emptyMap() }
        }
        val popularCastCafeNames = popularCastCafeIds.associateWith { cafeId ->
            nearbyCafeNameById[cafeId] ?: resolvedFromSummary[cafeId] ?: cafeId
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
        private const val NEARBY_GLOBAL_QUERY_MAX_ATTEMPTS = 3
    }
}
