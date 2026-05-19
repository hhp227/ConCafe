package com.hhp227.concafe.presentation.main.home

import com.hhp227.concafe.domain.model.HomeBanner

sealed interface HomeAction {
    data class ClickBanner(val banner: HomeBanner) : HomeAction
    data class ClickMaid(val id: String) : HomeAction
    data class ClickCafe(val id: String) : HomeAction
    data class ClickCafeEvent(val cafeId: String, val eventId: String) : HomeAction
    data class ClickBirthdayMaid(val id: String) : HomeAction
    data object ClickLoginPromptSignIn : HomeAction
    data object DismissLoginPrompt : HomeAction
    data object LoadMoreCafeEvents : HomeAction
    data object LoadMorePopularCasts : HomeAction
    data object LoadMoreNearbyCafes : HomeAction
    data object ClickCommunity : HomeAction
    data class ClickCommunityPost(val postId: String) : HomeAction
}
