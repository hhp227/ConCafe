package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.repository.BannerRepository

class FakeBannerRepository(
    private val dataSource: ConCafeDataSource
) : BannerRepository {
    override suspend fun getHomeBanners(limit: Int): List<HomeBanner> {
        return dataSource.banners.take(limit.coerceAtLeast(1))
    }
}
