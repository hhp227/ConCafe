package org.hhp227.concafe.domain.repository

import org.hhp227.concafe.domain.model.HomeBanner

interface BannerRepository {
    suspend fun getHomeBanners(limit: Int): List<HomeBanner>
}
