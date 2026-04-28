package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeMenuGoodsSection
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.CheckInCafeSummary
import com.hhp227.concafe.domain.repository.CafeRepository

class FakeCafeRepository(
    private val dataSource: ConCafeDataSource
) : CafeRepository {
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

    override suspend fun getCafeMenuGoods(cafeId: String): CafeMenuGoodsSection {
        TODO("Not yet implemented")
    }

    override suspend fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail {
        return dataSource.updateCafeInfo(update)
    }

    override suspend fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail {
        return dataSource.upsertCafeMenuGoods(update)
    }

    override suspend fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail {
        return dataSource.deleteCafeMenuGoods(cafeId, itemId)
    }

    override suspend fun isFavorite(userId: String, cafeId: String): Boolean {
        val set = dataSource.favoriteCafeIdsByUser[userId]
        return set?.contains(cafeId) ?: false
    }

    override suspend fun getFavoriteCafeIds(userId: String, limit: Int?): List<String> {
        val sortedFavoriteCafeIds = dataSource.favoriteCafeIdsByUser[userId]
            ?.toList()
            .orEmpty()
            .sorted()
        return if (limit != null && limit > 0) {
            sortedFavoriteCafeIds.take(limit)
        } else {
            sortedFavoriteCafeIds
        }
    }

    override suspend fun getCafesByIds(cafeIds: List<String>): List<Cafe> {
        val idSet = cafeIds.toSet()
        return dataSource.cafes
            .asSequence()
            .filter { cafe -> cafe.approved && idSet.contains(cafe.id) }
            .toList()
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

    override suspend fun getPopularCheckInCafes(limit: Int): List<CheckInCafeSummary> {
        return dataSource.cafes
            .filter { it.approved }
            .sortedByDescending { dataSource.cafeCheckInCountById[it.id] ?: 0 }
            .take(limit)
            .map { cafe ->
                CheckInCafeSummary(
                    id = cafe.id,
                    name = cafe.name,
                    locationLabel = cafe.region.city,
                    geoPoint = cafe.region.location,
                    rating = cafe.ratingAvg,
                    checkInCount = dataSource.cafeCheckInCountById[cafe.id] ?: 0
                )
            }
    }

    override suspend fun updateCafeSocialMedia(
        cafeId: String,
        instagramId: String?,
        twitterId: String?,
        tiktokId: String?,
        youtubeId: String?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateCafeReservationUrl(cafeId: String, reservationUrl: String?) {
        TODO("Not yet implemented")
    }
}
