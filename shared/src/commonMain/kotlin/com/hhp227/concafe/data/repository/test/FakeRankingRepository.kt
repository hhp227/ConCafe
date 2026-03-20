package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.repository.RankingRepository

class FakeRankingRepository(
    private val dataSource: ConCafeDataSource
) : RankingRepository {
    override suspend fun getCastRanking(period: RankingPeriod, country: String?, city: String?): List<RankingItem> {
        val validCafeIds = dataSource.cafes.filter { cafe ->
            val countryMatched = country.isNullOrBlank() || cafe.region.country.equals(country, ignoreCase = true)
            val cityMatched = city.isNullOrBlank() || cafe.region.city.equals(city, ignoreCase = true)
            countryMatched && cityMatched
        }.map { it.id }.toSet()

        return dataSource.rankingItemsFromCasts()
            .filter { item ->
                if (validCafeIds.isEmpty()) {
                    true
                } else {
                    dataSource.casts.any { cast ->
                        cast.id == item.id && validCafeIds.contains(cast.cafeId)
                    }
                }
            }
    }

    override suspend fun getCafeRanking(period: RankingPeriod, country: String?, city: String?): List<RankingItem> {
        val validCafeIds = dataSource.cafes.filter { cafe ->
            val countryMatched = country.isNullOrBlank() || cafe.region.country.equals(country, ignoreCase = true)
            val cityMatched = city.isNullOrBlank() || cafe.region.city.equals(city, ignoreCase = true)
            countryMatched && cityMatched
        }.map { it.id }.toSet()

        return dataSource.rankingItemsFromCafes()
            .filter { item ->
                validCafeIds.isEmpty() || validCafeIds.contains(item.id)
            }
    }
}