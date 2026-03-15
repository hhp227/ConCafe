package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.BannerEvent
import com.hhp227.concafe.domain.model.HomeBannerCreate
import com.hhp227.concafe.domain.model.HomeBanner
import kotlinx.coroutines.flow.Flow

interface BannerRepository {
    fun observeBannerEvent(): Flow<BannerEvent>
    suspend fun getHomeBanners(limit: Int): List<HomeBanner>
    suspend fun createHomeBanner(input: HomeBannerCreate): HomeBanner
}
