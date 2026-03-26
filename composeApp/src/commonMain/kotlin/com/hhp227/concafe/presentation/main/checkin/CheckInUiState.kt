package com.hhp227.concafe.presentation.main.checkin

import com.hhp227.concafe.domain.model.CheckInCafeSummary
import com.hhp227.concafe.domain.model.CheckInCastSummary
import com.hhp227.concafe.domain.model.CheckInVisitEntry
import com.hhp227.concafe.domain.model.User

data class CheckInUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: User? = null,
    val currentLocationLabel: String = "",
    val mapCafes: List<CheckInCafeSummary> = emptyList(),
    val popularCafes: List<CheckInCafeSummary> = emptyList(),
    val popularCasts: List<CheckInCastSummary> = emptyList(),
    val todayVisits: List<CheckInVisitEntry> = emptyList(),
    val recentVisits: List<CheckInVisitEntry> = emptyList(),
    val recentVisitsNextCursor: String? = null,
    val canLoadMoreRecentVisits: Boolean = false,
    val isLoadingMoreRecentVisits: Boolean = false,
    val isLoginPromptVisible: Boolean = false,
    val isNewVisitSheetVisible: Boolean = false,
    val reviewPrompt: ReviewPrompt? = null
) {
    data class ReviewPrompt(
        val visitId: String,
        val cafeId: String,
        val cafeName: String
    )

    companion object {
        fun empty() = CheckInUiState()
    }
}
