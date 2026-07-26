package com.hhp227.concafe.presentation.main.checkin

import com.hhp227.concafe.presentation.main.explore.ExploreUiState

sealed interface CheckInAction {
    data class ClickCafe(val id: String) : CheckInAction

    data class ClickCast(val id: String) : CheckInAction

    data object ClickCheckIn : CheckInAction

    data class ClickCheckInForCafe(val cafeId: String) : CheckInAction

    data object ClickMapFullView : CheckInAction

    data class UpdateMapRegion(val region: ExploreUiState.RegionFilter) : CheckInAction

    data object ClickSignIn : CheckInAction

    data object ClickSignUp : CheckInAction

    data object DismissLoginPrompt : CheckInAction

    data object DismissError : CheckInAction

    data object DismissNewVisitSheet : CheckInAction

    data object DismissQrCheckInSheet : CheckInAction

    data object DismissReviewPrompt : CheckInAction

    data object ClickWriteReviewPrompt : CheckInAction

    data object LoadMoreRecentVisits : CheckInAction

    data object ClickQrCheckIn : CheckInAction

    data class QrScanFailed(val message: String) : CheckInAction

    data class SubmitQrCheckIn(val rawValue: String) : CheckInAction

    data class SubmitNewVisit(
        val cafeId: String,
        val visitedAt: String,
        val memo: String?
    ) : CheckInAction
}
