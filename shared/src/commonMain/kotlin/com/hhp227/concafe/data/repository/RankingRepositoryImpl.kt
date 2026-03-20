package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.repository.RankingRepository

class RankingRepositoryImpl : RankingRepository {
    override suspend fun getCastRanking(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getCafeRanking(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        TODO("Not yet implemented")
    }
}