package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.RankingDataSource
import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.repository.RankingRepository

class RankingRepositoryImpl(
    private val rankingDataSource: RankingDataSource
) : RankingRepository {
    override suspend fun getCastRanking(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        return rankingDataSource.rankingItemsFromCasts(period = period, country = country, city = city)
    }

    override suspend fun getCafeRanking(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        return rankingDataSource.rankingItemsFromCafes(period = period, country = country, city = city)
    }
}
