package com.hhp227.concafe.presentation.cafe

sealed interface CafeAction {
    data object ClickBack : CafeAction

    data class ChangeTab(val tab: CafeUiState.TabType) : CafeAction

    data class ClickMaid(val id: String) : CafeAction

    data object ClickFavorite : CafeAction

    data object ClickWriteReview : CafeAction

    data object LoadMoreCasts : CafeAction

    data object LoadMoreNotices : CafeAction

    data object LoadMoreReviews : CafeAction

    data object Refresh : CafeAction

    data object ConsumeScrollToTopOnReturn : CafeAction

    data class EditReview(val reviewId: String) : CafeAction

    data class DeleteReview(val reviewId: String) : CafeAction

    data class ClickReviewImage(val imageUrl: String) : CafeAction

    data class ReportReview(val reviewId: String) : CafeAction
}
