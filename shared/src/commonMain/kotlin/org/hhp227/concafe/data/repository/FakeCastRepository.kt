package org.hhp227.concafe.data.repository

import org.hhp227.concafe.data.source.ConCafeDataSource
import org.hhp227.concafe.domain.common.PagedResult
import org.hhp227.concafe.domain.model.Cast
import org.hhp227.concafe.domain.model.CastDetail
import org.hhp227.concafe.domain.model.CastSchedule
import org.hhp227.concafe.domain.model.CastSort
import org.hhp227.concafe.domain.repository.CastRepository

class FakeCastRepository(
    private val dataSource: ConCafeDataSource
) : CastRepository {
    override suspend fun searchCasts(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast> {
        var filtered = dataSource.casts

        if (!query.isNullOrBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        if (!country.isNullOrBlank() || !city.isNullOrBlank()) {
            val validCafeIds = dataSource.cafes.filter { cafe ->
                val countryMatched = country.isNullOrBlank() || cafe.region.country.equals(country, ignoreCase = true)
                val cityMatched = city.isNullOrBlank() || cafe.region.city.equals(city, ignoreCase = true)
                countryMatched && cityMatched
            }.map { it.id }.toSet()
            filtered = filtered.filter { validCafeIds.contains(it.cafeId) }
        }

        filtered = when (sort) {
            CastSort.POPULAR -> filtered.sortedByDescending { it.followerCount }
            CastSort.LATEST -> filtered.sortedByDescending { it.id }
            CastSort.FOLLOWERS -> filtered.sortedByDescending { it.followerCount }
        }

        return dataSource.toPaged(filtered, cursor, pageSize)
    }

    override suspend fun getCastDetail(castId: String): CastDetail {
        return dataSource.castDetail(castId)
            ?: throw NoSuchElementException("cast detail not found")
    }

    override suspend fun getCastSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule> {
        val castDetail = dataSource.castDetail(castId)

        return if (castDetail != null) {
            castDetail.schedule.filter { it.date >= fromDate && it.date <= toDate }
        } else {
            throw NoSuchElementException("cast detail not found")
        }
    }

    override suspend fun followCast(userId: String, castId: String) {
        val set = dataSource.followedCastIdsByUser.getOrPut(userId) { mutableSetOf() }
        set.add(castId)
    }

    override suspend fun unfollowCast(userId: String, castId: String) {
        val set = dataSource.followedCastIdsByUser.getOrPut(userId) { mutableSetOf() }
        set.remove(castId)
    }
}
