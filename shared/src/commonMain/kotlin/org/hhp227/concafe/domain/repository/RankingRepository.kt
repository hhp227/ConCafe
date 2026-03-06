package org.hhp227.concafe.domain.repository

import org.hhp227.concafe.domain.model.RankingItem
import org.hhp227.concafe.domain.model.RankingPeriod

interface RankingRepository {
    suspend fun getCastRanking(period: RankingPeriod, country: String?, city: String?): List<RankingItem>

    suspend fun getCafeRanking(period: RankingPeriod, country: String?, city: String?): List<RankingItem>
}
