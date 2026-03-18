package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.model.*
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

    override suspend fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail {
        return dataSource.updateCafeInfo(update).also {
            //cafeDetailEvent.tryEmit(CafeDetailEvent.CafeInfoUpdated(update.cafeId, it.cafe))
        }
    }

    override suspend fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail {
        val existingItemId = update.itemId
        val isCreate = existingItemId.isNullOrBlank()
        val updatedDetail = dataSource.upsertCafeMenuGoods(update)
        val updatedMenu = updatedDetail.menus.firstOrNull { it.id == existingItemId }
            ?: updatedDetail.menus.lastOrNull()?.takeIf { update.category.lowercase() != "goods" }
        val updatedGoods = updatedDetail.goods.firstOrNull { it.id == existingItemId }
            ?: updatedDetail.goods.lastOrNull()?.takeIf { update.category.lowercase() == "goods" }

        val event = when {
            updatedMenu != null && isCreate -> CafeDetailEvent.MenuCreated(update.cafeId, updatedMenu)
            updatedMenu != null -> CafeDetailEvent.MenuUpdated(update.cafeId, updatedMenu)
            updatedGoods != null && isCreate -> CafeDetailEvent.GoodsCreated(update.cafeId, updatedGoods)
            updatedGoods != null -> CafeDetailEvent.GoodsUpdated(update.cafeId, updatedGoods)
            else -> throw NoSuchElementException("menu goods item not found")
        }
        //cafeDetailEvent.tryEmit(event)
        return updatedDetail
    }

    override suspend fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail {
        return dataSource.deleteCafeMenuGoods(cafeId, itemId).also {
            /*cafeDetailEvent.tryEmit(
                if (itemId.startsWith("goods")) {
                    CafeDetailEvent.GoodsDeleted(cafeId, itemId)
                } else {
                    CafeDetailEvent.MenuDeleted(cafeId, itemId)
                }
            )*/
        }
    }

    override suspend fun isFavorite(userId: String, cafeId: String): Boolean {
        val set = dataSource.favoriteCafeIdsByUser[userId]
        return set?.contains(cafeId) ?: false
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
}
