package org.hhp227.concafe.presentation.cast

import org.hhp227.concafe.domain.model.CastDetail
import org.hhp227.concafe.domain.model.CastRecentReview

data class CastUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val detail: CastDetail? = null,
    val recentReviews: List<CastRecentReview> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoggedIn: Boolean = false
) {
    companion object {
        fun empty(): CastUiState {
            return CastUiState()
        }
    }
}
