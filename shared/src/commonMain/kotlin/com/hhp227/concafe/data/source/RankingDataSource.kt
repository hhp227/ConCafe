package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.RankingItem

interface RankingDataSource {
    fun rankingItemsFromCasts(): List<RankingItem>
    fun rankingItemsFromCafes(): List<RankingItem>
    fun homePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast>
}