package com.hhp227.concafe.presentation.main.checkin

sealed interface CheckInAction {
    data class ClickCafe(val id: String) : CheckInAction

    data class ClickCast(val id: String) : CheckInAction

    data object ClickCheckIn : CheckInAction

    data class ClickCheckInForCafe(val cafeId: String) : CheckInAction

    data object ClickSignIn : CheckInAction

    data object ClickSignUp : CheckInAction

    data object DismissLoginPrompt : CheckInAction

    data object DismissError : CheckInAction

    data object DismissNewVisitSheet : CheckInAction

    data object DismissReviewPrompt : CheckInAction

    data object ClickWriteReviewPrompt : CheckInAction

    data object LoadMoreRecentVisits : CheckInAction

    data object ClickQrCheckIn : CheckInAction

    data class SubmitNewVisit(
        val cafeId: String,
        val visitedAt: String,
        val memo: String?
    ) : CheckInAction
}
