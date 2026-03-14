package com.hhp227.concafe.domain.model

sealed interface BannerEvent {
    data class Created(val banner: HomeBanner) : BannerEvent
}
