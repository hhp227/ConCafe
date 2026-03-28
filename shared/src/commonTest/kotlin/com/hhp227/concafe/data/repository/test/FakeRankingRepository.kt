package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.repository.RankingRepository

class FakeRankingRepository(
    private val dataSource: ConCafeDataSource
) : RankingRepository {
    override suspend fun getCastRanking(period: RankingPeriod, country: String?, city: String?): List<RankingItem> {
        return dataSource.rankingItemsFromCasts(period = period, country = country, city = city)
    }

    override suspend fun getCafeRanking(period: RankingPeriod, country: String?, city: String?): List<RankingItem> {
        return dataSource.rankingItemsFromCafes(period = period, country = country, city = city)
    }
}
