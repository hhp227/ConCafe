package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.HomeBanner

interface BannerRepository {
    suspend fun getHomeBanners(limit: Int): List<HomeBanner>
}
