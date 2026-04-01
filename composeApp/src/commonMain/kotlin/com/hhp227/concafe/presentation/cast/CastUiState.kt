package com.hhp227.concafe.presentation.cast

import com.hhp227.concafe.domain.model.CastAttendanceStatus
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastRecentReview

data class CastUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val detail: CastDetail? = null,
    val recentReviews: List<CastRecentReview> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoggedIn: Boolean = false,
    val todayAttendanceStatus: CastAttendanceStatus = CastAttendanceStatus.OFF
) {
    companion object {
        fun empty(): CastUiState {
            return CastUiState()
        }
    }
}
