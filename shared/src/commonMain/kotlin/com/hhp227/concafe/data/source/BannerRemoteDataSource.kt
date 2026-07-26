package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.HomeBanner

interface BannerRemoteDataSource {
    suspend fun fetchHomeBanners(): List<HomeBanner>
}
