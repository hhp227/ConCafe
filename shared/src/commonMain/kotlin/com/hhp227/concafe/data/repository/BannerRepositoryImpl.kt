package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.HomeBannerCreate
import com.hhp227.concafe.domain.repository.BannerRepository

class BannerRepositoryImpl : BannerRepository {
    override suspend fun getAllHomeBanners(): List<HomeBanner> {
        TODO("Not yet implemented")
    }

    override suspend fun getHomeBanners(limit: Int): List<HomeBanner> {
        TODO("Not yet implemented")
    }

    override suspend fun createHomeBanner(input: HomeBannerCreate): HomeBanner {
        TODO("Not yet implemented")
    }

    override suspend fun updateHomeBanner(
        bannerId: String,
        input: HomeBannerCreate
    ): HomeBanner {
        TODO("Not yet implemented")
    }

    override suspend fun deleteHomeBanner(bannerId: String): HomeBanner {
        TODO("Not yet implemented")
    }
}