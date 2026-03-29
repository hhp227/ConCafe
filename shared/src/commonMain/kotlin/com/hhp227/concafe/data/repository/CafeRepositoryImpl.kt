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
        val firestoreDataSource = cafeDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            return firestoreDataSource.searchCafesRemote(
                query = query,
                country = country,
                city = city,
                sort = sort,
                cursor = cursor,
                pageSize = pageSize
            )
        }
        return searchCafesFromCache(
            query = query,
            country = country,
            city = city,
            sort = sort,
            cursor = cursor,
            pageSize = pageSize
        )
    }

    override suspend fun getCafeDetail(cafeId: String): CafeDetail {
        val cachedDetail = cafeDataSource.cafeDetail(cafeId)
        val firestoreDataSource = cafeDataSource as? FirestoreConCafeDataSource

        if (cachedDetail != null) {
            val shouldRefresh = firestoreDataSource != null && (
                !firestoreDataSource.isCafeDetailHydrated(cafeId) ||
                cachedDetail.businessHours.isBlank() ||
                    cachedDetail.phoneNumber.isBlank() ||
                    cachedDetail.businessHours == "운영시간 정보 준비중" ||
                    cachedDetail.phoneNumber == "연락처 정보 준비중"
                )

            if (shouldRefresh) {
                firestoreDataSource?.refreshCafeDetail(cafeId)
                val refreshed = cafeDataSource.cafeDetail(cafeId)

                if (refreshed != null) {
                    return refreshed
                }
            }
            return cachedDetail
        }

        if (firestoreDataSource != null) {
            firestoreDataSource.refreshCafeDetail(cafeId)
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
        val firestoreDataSource = cafeDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            return firestoreDataSource.upsertCafeMenuGoodsRemote(update)
        }
        return cafeDataSource.upsertCafeMenuGoods(update)
    }

    override suspend fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail {
        val firestoreDataSource = cafeDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            return firestoreDataSource.deleteCafeMenuGoodsRemote(cafeId, itemId)
        }
        return cafeDataSource.deleteCafeMenuGoods(cafeId, itemId)
    }

    override suspend fun isFavorite(userId: String, cafeId: String): Boolean {
        (cafeDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            firestoreDataSource.refreshFavoriteCafeIds(userId)
        }
        val set = socialDataSource.favoriteCafeIdsByUser[userId]
        return set?.contains(cafeId) ?: false
    }

    override suspend fun getFavoriteCafeIds(userId: String): List<String> {
        (cafeDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            firestoreDataSource.refreshFavoriteCafeIds(userId)
        }
        return socialDataSource.favoriteCafeIdsByUser[userId]
            ?.toList()
            .orEmpty()
            .sorted()
    }

    override suspend fun getCafesByIds(cafeIds: List<String>): List<Cafe> {
        if (cafeIds.isEmpty()) {
            return emptyList()
        }
        val requestedIds = cafeIds.toSet()
        val firestoreDataSource = cafeDataSource as? FirestoreConCafeDataSource
        val cachedIds = cafeDataSource.cafes
            .asSequence()
            .map { cafe -> cafe.id }
            .toSet()
        val missingIds = requestedIds - cachedIds

        if (firestoreDataSource != null && missingIds.isNotEmpty()) {
            missingIds.forEach { cafeId ->
                firestoreDataSource.refreshCafeDetail(cafeId)
            }
        }

        val idSet = cafeIds.toSet()
        val cafeById = cafeDataSource.cafes
            .asSequence()
            .filter { cafe -> cafe.approved && idSet.contains(cafe.id) }
            .associateBy { cafe -> cafe.id }
        return cafeIds.distinct().mapNotNull { cafeId -> cafeById[cafeId] }
    }

    override suspend fun toggleFavorite(userId: String, cafeId: String): Boolean {
        (cafeDataSource as? FirestoreConCafeDataSource)?.let { firestoreDataSource ->
            firestoreDataSource.refreshFavoriteCafeIds(userId)
            val favoriteSet = socialDataSource.favoriteCafeIdsByUser[userId]
            val isFavorite = favoriteSet?.contains(cafeId) == true

            if (isFavorite) {
                firestoreDataSource.unfavoriteCafeRemote(
                    userId = userId,
                    cafeId = cafeId
                )
            } else {
                firestoreDataSource.favoriteCafeRemote(
                    userId = userId,
                    cafeId = cafeId
                )
            }
            return !isFavorite
        }
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
        val safeLimit = if (limit > 0) limit else 1
        val visitCountByCafeId = visitDataSource.visits
            .groupingBy { it.cafeId }
            .eachCount()
        val firestoreDataSource = cafeDataSource as? FirestoreConCafeDataSource
        val sourceCafes = if (firestoreDataSource != null) {
            val page = firestoreDataSource.searchCafesRemote(
                query = null,
                country = null,
                city = null,
                sort = CafeSort.POPULAR,
                cursor = null,
                pageSize = safeLimit
            )
            page.items
        } else {
            cafeDataSource.cafes
                .filter { it.approved }
                .sortedByDescending { visitCountByCafeId[it.id] ?: 0 }
                .take(safeLimit)
        }
        return sourceCafes.map { cafe ->
            val resolvedVisitCount = visitCountByCafeId[cafe.id] ?: 0

            CheckInCafeSummary(
                id = cafe.id,
                name = cafe.name,
                locationLabel = cafe.region.city,
                geoPoint = cafe.region.location,
                rating = cafe.ratingAvg,
                checkInCount = resolvedVisitCount,
                thumbnailImage = cafe.thumbnailImage
            )
        }
    }

    private fun searchCafesFromCache(
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
}
