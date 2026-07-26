package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.RankingPeriod

interface RankingDataSource {
    suspend fun rankingItemsFromCasts(period: RankingPeriod, country: String?, city: String?): List<RankingItem>
    suspend fun rankingItemsFromCafes(period: RankingPeriod, country: String?, city: String?): List<RankingItem>
    fun homePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast>
}
