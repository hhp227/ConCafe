package com.hhp227.concafe.presentation.cafe

import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeDetailCast
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.CafeDetailReview

data class CafeUiState(
    val isLoading: Boolean = false,
    val isLoadingMoreCasts: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: TabType = TabType.INFO,
    val detail: CafeDetail? = null,
    val casts: List<CafeDetailCast> = emptyList(),
    val castsNextCursor: String? = null,
    val canLoadMoreCasts: Boolean = false,
    val isLoadingMenuGoods: Boolean = false,
    val hasLoadedMenuGoods: Boolean = false,
    val isLoadingMoreNotices: Boolean = false,
    val noticesNextCursor: String? = null,
    val canLoadMoreNotices: Boolean = false,
    val notices: List<CafeNoticeManagementItem> = emptyList(),
    val isLoadingMoreReviews: Boolean = false,
    val reviewsNextCursor: String? = null,
    val canLoadMoreReviews: Boolean = false,
    val reviews: List<CafeDetailReview> = emptyList(),
    val isFavorite: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isVisitVerified: Boolean = false,
    val shouldScrollToTopOnReturn: Boolean = false,
    val currentUserId: String? = null
) {
    enum class TabType {
        INFO,
        CASTS,
        MENU,
        REVIEWS,
        NOTICES
    }

    companion object {
        fun empty(): CafeUiState {
            return CafeUiState()
        }
    }
}
