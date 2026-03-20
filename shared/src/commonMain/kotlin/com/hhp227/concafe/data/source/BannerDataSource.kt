package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.HomeBanner

interface BannerDataSource {
    val banners: MutableList<HomeBanner>
}
