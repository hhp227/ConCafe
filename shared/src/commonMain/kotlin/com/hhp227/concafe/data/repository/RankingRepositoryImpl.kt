package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.data.source.RankingDataSource
import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.repository.RankingRepository

class RankingRepositoryImpl(
    private val rankingDataSource: RankingDataSource,
    private val cafeDataSource: CafeDataSource,
    private val castDataSource: CastDataSource
) : RankingRepository {
    override suspend fun getCastRanking(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        val validCafeIds = cafeDataSource.cafes.filter { cafe ->
            val countryMatched = country.isNullOrBlank() || cafe.region.country.equals(country, ignoreCase = true)
            val cityMatched = city.isNullOrBlank() || cafe.region.city.equals(city, ignoreCase = true)
            countryMatched && cityMatched
        }.map { it.id }.toSet()

        return rankingDataSource.rankingItemsFromCasts()
            .filter { item ->
                if (validCafeIds.isEmpty()) {
                    true
                } else {
                    castDataSource.casts.any { cast ->
                        cast.id == item.id && validCafeIds.contains(cast.cafeId)
                    }
                }
            }
    }

    override suspend fun getCafeRanking(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        val validCafeIds = cafeDataSource.cafes.filter { cafe ->
            val countryMatched = country.isNullOrBlank() || cafe.region.country.equals(country, ignoreCase = true)
            val cityMatched = city.isNullOrBlank() || cafe.region.city.equals(city, ignoreCase = true)
            countryMatched && cityMatched
        }.map { it.id }.toSet()

        return rankingDataSource.rankingItemsFromCafes()
            .filter { item ->
                validCafeIds.isEmpty() || validCafeIds.contains(item.id)
            }
    }
}
