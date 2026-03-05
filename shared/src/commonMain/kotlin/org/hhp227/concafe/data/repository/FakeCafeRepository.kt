package org.hhp227.concafe.data.repository

import org.hhp227.concafe.data.source.ConCafeDataSource
import org.hhp227.concafe.domain.common.PagedResult
import org.hhp227.concafe.domain.model.Cafe
import org.hhp227.concafe.domain.model.CafeDetail
import org.hhp227.concafe.domain.model.CafeSort
import org.hhp227.concafe.domain.model.HomeFeed
import org.hhp227.concafe.domain.repository.CafeRepository

class FakeCafeRepository(
    private val dataSource: ConCafeDataSource
) : CafeRepository {
    override suspend fun getHomeFeed(limit: Int): HomeFeed {
        val cappedLimit = limit.coerceAtLeast(1)
        return dataSource.homeFeed.copy(
            popularCasts = dataSource.homeFeed.popularCasts.take(cappedLimit),
            nearbyCafes = dataSource.homeFeed.nearbyCafes.take(cappedLimit),
            birthdayCasts = dataSource.homeFeed.birthdayCasts.take(cappedLimit),
            notices = dataSource.homeFeed.notices.take(cappedLimit)
        )
    }

    override suspend fun searchCafes(
        query: String?,
        country: String?,
        city: String?,
        sort: CafeSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cafe> {
        var filtered = dataSource.cafes.filter { it.approved }

        if (!query.isNullOrBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        if (!country.isNullOrBlank()) {
            filtered = filtered.filter { it.region.country.equals(country, ignoreCase = true) }
        }

        if (!city.isNullOrBlank()) {
            filtered = filtered.filter { it.region.city.equals(city, ignoreCase = true) }
        }

        filtered = when (sort) {
            CafeSort.POPULAR -> filtered.sortedByDescending { it.reviewCount }
            CafeSort.LATEST -> filtered.sortedByDescending { it.id }
            CafeSort.RATING -> filtered.sortedByDescending { it.ratingAvg }
        }

        return dataSource.toPaged(filtered, cursor, pageSize)
    }

    override suspend fun getCafeDetail(cafeId: String): CafeDetail {
        return dataSource.cafeDetail(cafeId)
            ?: throw NoSuchElementException("cafe detail not found")
    }

    override suspend fun toggleFavorite(userId: String, cafeId: String): Boolean {
        val set = dataSource.favoriteCafeIdsByUser.getOrPut(userId) { mutableSetOf() }
        return if (set.contains(cafeId)) {
            set.remove(cafeId)
            false
        } else {
            set.add(cafeId)
            true
        }
    }
}
