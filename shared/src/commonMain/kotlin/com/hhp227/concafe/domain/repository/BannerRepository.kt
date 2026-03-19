package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.HomeBannerCreate

interface BannerRepository {
    suspend fun getHomeBanners(limit: Int): List<HomeBanner>
    suspend fun createHomeBanner(input: HomeBannerCreate): HomeBanner
}
