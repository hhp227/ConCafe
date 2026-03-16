package com.hhp227.concafe.presentation.main.home

import com.hhp227.concafe.domain.model.HomeBanner

sealed interface HomeAction {
    data class ClickBanner(val banner: HomeBanner) : HomeAction
    data class ClickMaid(val id: String) : HomeAction
    data class ClickCafe(val id: String) : HomeAction
    data class ClickBirthdayMaid(val id: String) : HomeAction
    data object LoadMorePopularCasts : HomeAction
    data object LoadMoreNearbyCafes : HomeAction
}
