package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.SocialDataSource
import com.hhp227.concafe.data.source.VisitDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.CheckInCafeSummary
import com.hhp227.concafe.domain.repository.CafeRepository

class CafeRepositoryImpl(
    private val cafeDataSource: CafeDataSource,
    private val socialDataSource: SocialDataSource,
    private val pagingDataSource: PagingDataSource,
    private val visitDataSource: VisitDataSource
) : CafeRepository {
    override suspend fun searchCafes(
        query: String?,
        country: String?,
        city: String?,
        sort: CafeSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cafe> {
        var filtered = cafeDataSource.cafes.filter { it.approved }

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
        return pagingDataSource.toPaged(filtered, cursor, pageSize)
    }

    override suspend fun getCafeDetail(cafeId: String): CafeDetail {
        val cachedDetail = cafeDataSource.cafeDetail(cafeId)

        if (cachedDetail != null) {
            return cachedDetail
        }
        val firestoreDataSource = cafeDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            runCatching {
                firestoreDataSource.refreshCafeDetail(cafeId)
            }
            val refreshed = cafeDataSource.cafeDetail(cafeId)

            if (refreshed != null) {
                return refreshed
            }
        }
        throw NoSuchElementException("cafe detail not found")
    }

    override suspend fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail {
        val firestoreDataSource = cafeDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            return firestoreDataSource.updateCafeInfoRemote(update)
        }
        return cafeDataSource.updateCafeInfo(update)
    }

    override suspend fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail {
        return cafeDataSource.upsertCafeMenuGoods(update)
    }

    override suspend fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail {
        return cafeDataSource.deleteCafeMenuGoods(cafeId, itemId)
    }

    override suspend fun isFavorite(userId: String, cafeId: String): Boolean {
        val set = socialDataSource.favoriteCafeIdsByUser[userId]
        return set?.contains(cafeId) ?: false
    }

    override suspend fun getFavoriteCafeIds(userId: String): List<String> {
        return socialDataSource.favoriteCafeIdsByUser[userId]
            ?.toList()
            .orEmpty()
            .sorted()
    }

    override suspend fun getCafesByIds(cafeIds: List<String>): List<Cafe> {
        val idSet = cafeIds.toSet()
        return cafeDataSource.cafes
            .asSequence()
            .filter { cafe -> cafe.approved && idSet.contains(cafe.id) }
            .toList()
    }

    override suspend fun toggleFavorite(userId: String, cafeId: String): Boolean {
        val set = socialDataSource.favoriteCafeIdsByUser.getOrPut(userId) { mutableSetOf() }
        return if (set.contains(cafeId)) {
            set.remove(cafeId)
            false
        } else {
            set.add(cafeId)
            true
        }
    }

    override suspend fun getPopularCheckInCafes(limit: Int): List<CheckInCafeSummary> {
        val visitCountByCafeId = visitDataSource.visits
            .groupingBy { it.cafeId }
            .eachCount()
        return cafeDataSource.cafes
            .filter { it.approved }
            .sortedByDescending { visitCountByCafeId[it.id] ?: 0 }
            .take(limit)
            .map { cafe ->
                CheckInCafeSummary(
                    id = cafe.id,
                    name = cafe.name,
                    locationLabel = cafe.region.city,
                    geoPoint = cafe.region.location,
                    rating = cafe.ratingAvg,
                    checkInCount = visitCountByCafeId[cafe.id] ?: 0,
                    thumbnailImage = cafe.thumbnailImage
                )
            }
    }
}
