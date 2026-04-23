package com.hhp227.concafe.presentation.cafe.event

import com.hhp227.concafe.domain.model.CafeEventManagementItem

data class CafeEventUiState(
    val isLoading: Boolean = false,
    val event: CafeEventManagementItem? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun empty() = CafeEventUiState()
    }
}
