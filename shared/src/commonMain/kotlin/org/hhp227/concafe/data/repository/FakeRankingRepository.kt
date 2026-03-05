package org.hhp227.concafe.data.repository

import org.hhp227.concafe.data.source.ConCafeDataSource
import org.hhp227.concafe.domain.model.RankingItem
import org.hhp227.concafe.domain.model.RankingPeriod
import org.hhp227.concafe.domain.repository.RankingRepository

class FakeRankingRepository(
    private val dataSource: ConCafeDataSource
) : RankingRepository {
    override suspend fun getCastRanking(period: RankingPeriod, country: String?, city: String?): List<RankingItem> {
        return dataSource.rankingItemsFromCasts()
    }

    override suspend fun getCafeRanking(period: RankingPeriod, country: String?, city: String?): List<RankingItem> {
        return dataSource.rankingItemsFromCafes()
    }
}
