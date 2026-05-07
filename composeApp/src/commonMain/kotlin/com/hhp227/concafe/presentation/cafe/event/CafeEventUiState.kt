package com.hhp227.concafe.presentation.cafe.event

import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CafeEventManagementItem

data class CafeEventUiState(
    val isLoading: Boolean = false,
    val event: CafeEventManagementItem? = null,
    val errorMessage: String? = null,
    val participantCasts: List<Cast> = emptyList(),
    val isLikedByMe: Boolean = false,
    val likeCount: Int = 0,
    val isTogglingLike: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isLoginPromptVisible: Boolean = false
) {
    companion object {
        fun empty() = CafeEventUiState()
    }
}
