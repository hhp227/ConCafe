package com.hhp227.concafe.domain.model

sealed class BannerEvent {
    data class Created(val banner: HomeBanner) : BannerEvent()
}
