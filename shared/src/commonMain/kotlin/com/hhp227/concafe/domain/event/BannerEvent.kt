package com.hhp227.concafe.domain.event

import com.hhp227.concafe.domain.model.HomeBanner

sealed class BannerEvent {
    data class Created(val banner: HomeBanner) : BannerEvent()
    data class Updated(val banner: HomeBanner) : BannerEvent()
    data class Deleted(val banner: HomeBanner) : BannerEvent()
}
