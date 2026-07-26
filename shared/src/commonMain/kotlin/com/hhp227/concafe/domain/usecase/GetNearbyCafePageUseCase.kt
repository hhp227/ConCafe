package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.ExploreRegionFilter
import com.hhp227.concafe.domain.repository.CafeRepository
import kotlinx.datetime.TimeZone

class GetNearbyCafePageUseCase(
    private val cafeRepository: CafeRepository
) {
    private enum class PagingPhase { LOCAL, GLOBAL }

    private data class CursorState(
        val phase: PagingPhase,
        val regionKey: String,
        val localCursor: String?,
        val globalCursor: String?
    )

    private fun resolveNearbyRegionFilter(): ExploreRegionFilter? {
        val timeZoneId = TimeZone.currentSystemDefault().id.lowercase()
        val isKoreaTimeZone = timeZoneId.contains("seoul") || timeZoneId == "rok"
        val isJapanTimeZone = timeZoneId.contains("tokyo")
            || timeZoneId.contains("osaka")
            || timeZoneId == "japan"
        return if (isKoreaTimeZone) {
            ExploreRegionFilter.from("seoul")
        } else if (isJapanTimeZone) {
            if (timeZoneId.contains("osaka")) ExploreRegionFilter.from("osaka")
            else ExploreRegionFilter.from("tokyo")
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

    private fun encodeCursor(state: CursorState): String {
        val sep = "\u001F"
        return listOf(
            "v3",
            state.phase.name,
            state.regionKey,
            state.localCursor ?: "",
            state.globalCursor ?: ""
        ).joinToString(sep)
    }

    private fun decodeCursor(rawCursor: String?): CursorState? {
        val sep = "\u001F"
        if (rawCursor.isNullOrBlank()) return null
        val tokens = rawCursor.split(sep)
        if (tokens.size != 5 || tokens[0] != "v3" || tokens[2].isBlank()) return null
        val phase = when (tokens[1]) {
            PagingPhase.LOCAL.name -> PagingPhase.LOCAL
            PagingPhase.GLOBAL.name -> PagingPhase.GLOBAL
            else -> return null
        }
        return CursorState(
            phase = phase,
            regionKey = tokens[2],
            localCursor = tokens[3].ifBlank { null },
            globalCursor = tokens[4].ifBlank { null }
        )
    }

    private suspend fun loadGlobalNonLocalPage(
        regionFilter: ExploreRegionFilter,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cafe> {
        val collectedItems = mutableListOf<Cafe>()
        var globalCursor = cursor
        var hasNext = true

        while (hasNext && collectedItems.size < pageSize) {
            val requestedSize = (pageSize - collectedItems.size).coerceAtLeast(1)
            val page = cafeRepository.searchCafes(
                query = null,
                country = null,
                city = null,
                sort = CafeSort.RATING,
                cursor = globalCursor,
                pageSize = requestedSize
            )
            collectedItems.addAll(page.items.filterNot { isSameRegion(it, regionFilter) })
            globalCursor = page.nextCursor
            hasNext = page.hasNext && !globalCursor.isNullOrBlank()
            if (page.items.isEmpty()) break
        }
        return PagedResult(
            items = collectedItems.take(pageSize),
            nextCursor = globalCursor,
            hasNext = hasNext
        )
    }

    private suspend fun hasAnyGlobalNonLocal(regionFilter: ExploreRegionFilter): Boolean {
        return loadGlobalNonLocalPage(
            regionFilter = regionFilter,
            cursor = null,
            pageSize = 1
        ).items.isNotEmpty()
    }

    private suspend fun loadPage(
        cursor: String?,
        nearbyRegionFilter: ExploreRegionFilter?
    ): PagedResult<Cafe> {
        if (nearbyRegionFilter == null) {
            return cafeRepository.searchCafes(
                query = null,
                country = null,
                city = null,
                sort = CafeSort.RATING,
                cursor = cursor,
                pageSize = PAGE_SIZE
            )
        }
        val decodedCursor = decodeCursor(cursor)
        val currentState = if (decodedCursor != null && decodedCursor.regionKey == nearbyRegionFilter.key) {
            decodedCursor
        } else {
            CursorState(
                phase = PagingPhase.LOCAL,
                regionKey = nearbyRegionFilter.key,
                localCursor = null,
                globalCursor = null
            )
        }
        return when (currentState.phase) {
            PagingPhase.LOCAL -> {
                val localPage = cafeRepository.searchCafes(
                    query = null,
                    country = nearbyRegionFilter.country,
                    city = nearbyRegionFilter.city,
                    sort = CafeSort.RATING,
                    cursor = currentState.localCursor,
                    pageSize = PAGE_SIZE
                )
                if (localPage.items.size == PAGE_SIZE) {
                    val hasNext = if (localPage.hasNext) {
                        true
                    } else {
                        hasAnyGlobalNonLocal(nearbyRegionFilter)
                    }
                    val nextCursor = if (hasNext) {
                        encodeCursor(
                            CursorState(
                                phase = if (localPage.hasNext) PagingPhase.LOCAL else PagingPhase.GLOBAL,
                                regionKey = nearbyRegionFilter.key,
                                localCursor = localPage.nextCursor,
                                globalCursor = null
                            )
                        )
                    } else {
                        null
                    }
                    PagedResult(
                        items = localPage.items,
                        nextCursor = nextCursor,
                        hasNext = hasNext
                    )
                } else {
                    val globalPage = loadGlobalNonLocalPage(
                        regionFilter = nearbyRegionFilter,
                        cursor = null,
                        pageSize = PAGE_SIZE - localPage.items.size
                    )
                    val items = localPage.items + globalPage.items
                    val hasNext = globalPage.hasNext
                    PagedResult(
                        items = items,
                        nextCursor = if (hasNext) {
                            encodeCursor(
                                CursorState(
                                    phase = PagingPhase.GLOBAL,
                                    regionKey = nearbyRegionFilter.key,
                                    localCursor = localPage.nextCursor,
                                    globalCursor = globalPage.nextCursor
                                )
                            )
                        } else {
                            null
                        },
                        hasNext = hasNext
                    )
                }
            }
            PagingPhase.GLOBAL -> {
                val globalPage = loadGlobalNonLocalPage(
                    regionFilter = nearbyRegionFilter,
                    cursor = currentState.globalCursor,
                    pageSize = PAGE_SIZE
                )
                PagedResult(
                    items = globalPage.items,
                    nextCursor = if (globalPage.hasNext) {
                        encodeCursor(
                            CursorState(
                                phase = PagingPhase.GLOBAL,
                                regionKey = nearbyRegionFilter.key,
                                localCursor = currentState.localCursor,
                                globalCursor = globalPage.nextCursor
                            )
                        )
                    } else {
                        null
                    },
                    hasNext = globalPage.hasNext
                )
            }
        }
    }

    suspend operator fun invoke(cursor: String?): AppResult<PagedResult<Cafe>> {
        return try {
            val nearbyRegionFilter = resolveNearbyRegionFilter()
            AppResult.Success(loadPage(cursor, nearbyRegionFilter))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    companion object {
        const val PAGE_SIZE = 6
    }
}
