package com.hhp227.concafe.presentation.main.home

import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.Notice

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val banners: List<HomeBanner>,
    val popularCasts: List<Cast>,
    val nearbyCafes: List<Cafe>,
    val nearbyCafeCursor: String? = null,
    val canLoadMoreNearbyCafes: Boolean,
    val birthdayCasts: List<Cast>,
    val notices: List<Notice>
) {
    companion object {
        fun empty() = HomeUiState(
            banners = emptyList(),
            popularCasts = emptyList(),
            nearbyCafes = emptyList(),
            nearbyCafeCursor = null,
            canLoadMoreNearbyCafes = false,
            birthdayCasts = emptyList(),
            notices = emptyList()
        )
    }
}
