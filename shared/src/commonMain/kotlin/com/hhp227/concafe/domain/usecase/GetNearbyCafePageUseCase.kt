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
            "v1",
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
        if (tokens.size != 5 || tokens[0] != "v1") return null
        val phase = when (tokens[1]) {
            PagingPhase.LOCAL.name -> PagingPhase.LOCAL
            PagingPhase.GLOBAL.name -> PagingPhase.GLOBAL
            else -> null
        } ?: return null
        if (tokens[2].isBlank()) return null
        return CursorState(
            phase = phase,
            regionKey = tokens[2],
            localCursor = tokens[3].ifBlank { null },
            globalCursor = tokens[4].ifBlank { null }
        )
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
        val collectedItems = mutableListOf<Cafe>()
        var phase = currentState.phase
        var localCursor = currentState.localCursor
        var globalCursor = currentState.globalCursor
        var localHasNext = false
        var globalHasNext = false
        var didQueryGlobal = false

        if (phase == PagingPhase.LOCAL) {
            val localPage = cafeRepository.searchCafes(
                query = null,
                country = nearbyRegionFilter.country,
                city = nearbyRegionFilter.city,
                sort = CafeSort.RATING,
                cursor = localCursor,
                pageSize = PAGE_SIZE
            )
            collectedItems.addAll(localPage.items)
            localCursor = localPage.nextCursor
            localHasNext = localPage.hasNext
            if (!localHasNext) {
                phase = PagingPhase.GLOBAL
            }
        }
        if (collectedItems.size < PAGE_SIZE && phase == PagingPhase.GLOBAL) {
            while (collectedItems.size < PAGE_SIZE) {
                val previousGlobalCursor = globalCursor
                val globalPage = cafeRepository.searchCafes(
                    query = null,
                    country = null,
                    city = null,
                    sort = CafeSort.RATING,
                    cursor = globalCursor,
                    pageSize = PAGE_SIZE
                )
                val nonLocalItems = globalPage.items.filter { !isSameRegion(it, nearbyRegionFilter) }
                val remaining = PAGE_SIZE - collectedItems.size
                collectedItems.addAll(nonLocalItems.take(remaining))
                globalCursor = globalPage.nextCursor
                globalHasNext = globalPage.hasNext
                didQueryGlobal = true
                if (!globalPage.hasNext) break
                if (globalCursor == previousGlobalCursor) break
            }
        }
        val hasNext = when {
            phase == PagingPhase.LOCAL -> localHasNext
            !didQueryGlobal -> true
            else -> globalHasNext
        }
        val nextCursor = if (hasNext) {
            encodeCursor(
                CursorState(
                    phase = phase,
                    regionKey = nearbyRegionFilter.key,
                    localCursor = localCursor,
                    globalCursor = globalCursor
                )
            )
        } else {
            null
        }
        return PagedResult(
            items = collectedItems,
            nextCursor = nextCursor,
            hasNext = hasNext
        )
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
