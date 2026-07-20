package com.hhp227.concafe.presentation.main.home

import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CommunityPost
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.HomeCafeEvent
import com.hhp227.concafe.domain.model.Notice

data class HomeUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isLoginPromptVisible: Boolean = false,
    val errorMessage: String? = null,
    val banners: List<HomeBanner>,
    val popularCasts: List<Cast>,
    val popularCastCafeNames: Map<String, String>,
    val popularCastCafeRegions: Map<String, String> = emptyMap(),
    val popularCastCursor: String? = null,
    val canLoadMorePopularCasts: Boolean,
    val isLoadingMorePopularCasts: Boolean = false,
    val nearbyCafes: List<Cafe>,
    val nearbyCafeCursor: String? = null,
    val canLoadMoreNearbyCafes: Boolean,
    val isLoadingMoreNearbyCafes: Boolean = false,
    val birthdayCasts: List<Cast>,
    val birthdayCastCafeNames: Map<String, String> = emptyMap(),
    val notices: List<Notice>,
    val cafeEvents: List<HomeCafeEvent>,
    val cafeEventCursor: String? = null,
    val canLoadMoreCafeEvents: Boolean = false,
    val isLoadingMoreCafeEvents: Boolean = false,
    val communityPosts: List<CommunityPost> = emptyList()
) {
    companion object {
        fun empty() = HomeUiState(
            isLoggedIn = false,
            isLoginPromptVisible = false,
            banners = emptyList(),
            popularCasts = emptyList(),
            popularCastCafeNames = emptyMap(),
            popularCastCafeRegions = emptyMap(),
            popularCastCursor = null,
            canLoadMorePopularCasts = false,
            isLoadingMorePopularCasts = false,
            nearbyCafes = emptyList(),
            nearbyCafeCursor = null,
            canLoadMoreNearbyCafes = false,
            isLoadingMoreNearbyCafes = false,
            birthdayCasts = emptyList(),
            birthdayCastCafeNames = emptyMap(),
            notices = emptyList(),
            cafeEvents = emptyList(),
            cafeEventCursor = null,
            canLoadMoreCafeEvents = false,
            isLoadingMoreCafeEvents = false,
            communityPosts = emptyList()
        )
    }
}
